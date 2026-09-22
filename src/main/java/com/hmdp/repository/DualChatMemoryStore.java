package com.hmdp.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.cache.RedisMemoryHelper;
import com.hmdp.entity.ChatMemory;
import com.hmdp.mapper.ChatMemoryMapper;
import com.hmdp.policy.TokenWindowPolicy;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 双层会话记忆存储 (Redis L1 + MySQL L2)
 *
 * 设计要点:
 * 1. updateMessages 时:先做 token 裁剪 → 同步写 Redis (带 24h TTL) → 异步写 MySQL
 * 2. getMessages 时:Redis 命中直接返回 → 未命中回源 MySQL → 回写 Redis
 * 3. MySQL 写失败只记日志不抛异常,不影响主流程对话
 * 4. Redis 故障时降级到 MySQL,对话稍慢但可用
 */
@Slf4j
@Repository
public class DualChatMemoryStore implements ChatMemoryStore {

    @Autowired
    private RedisMemoryHelper redisMemoryHelper;
    @Autowired
    private ChatMemoryMapper chatMemoryMapper;
    @Autowired
    private TokenWindowPolicy tokenWindowPolicy;

    /** Redis TTL: 24 小时 */
    private static final long TTL_SECONDS = 24 * 3600L;

    /** MySQL 异步落库专用线程池
     *  核心 4,队列 16,拒绝策略 CallerRunsPolicy (打满时让调用线程自己跑,保证不丢)
     */
    private final ExecutorService ioExecutor = new ThreadPoolExecutor(
            4, 4,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(16),
            r -> {
                Thread t = new Thread(r, "chat-memory-io");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String memoryIdStr = memoryId.toString();

        // 1. 先查 Redis
        List<ChatMessage> redisResult = redisMemoryHelper.get(memoryIdStr);
        if (redisResult != null && !redisResult.isEmpty()) {
            return redisResult;
        }

        // 2. 查 MySQL:按 memory_id 取整条记录
        try {
            LambdaQueryWrapper<ChatMemory> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ChatMemory::getMemoryId, memoryIdStr);
            ChatMemory record = chatMemoryMapper.selectOne(wrapper);

            if (record == null || record.getMesagesJson() == null || record.getMesagesJson().isEmpty()) {
                // 没记录返回空列表,不返回 null,避免上层 NPE
                return new ArrayList<>();
            }

            // 3. 用 LangChain4j 自带反序列化器
            List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(record.getMesagesJson());

            // 4. 回写 Redis 缓存 (TTL 24h)
            redisMemoryHelper.setWithTtl(memoryIdStr, messages, TTL_SECONDS);

            return messages;
        } catch (Exception e) {
            log.error("[DualMemory] MySQL 回源失败 memoryId={}", memoryIdStr, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        String memoryIdStr = memoryId.toString();

        // 1. token 裁剪 (关键:裁剪后才入库,保证 Redis/MySQL 存的永远是被裁剪过的版本)
        List<ChatMessage> trimmed = tokenWindowPolicy.trim(list);

        // 2. 序列化为 JSON
        String json = ChatMessageSerializer.messagesToJson(trimmed);

        // 3. 同步写 Redis (毫秒级,保证下次对话立即读到)
        try {
            redisMemoryHelper.setWithTtl(memoryIdStr, trimmed, TTL_SECONDS);
        } catch (Exception e) {
            log.error("[DualMemory] Redis 写入失败 memoryId={},降级 MySQL 同步写", memoryIdStr, e);
            // Redis 写失败,降级为 MySQL 同步写,保证数据不丢
            upsertMysql(memoryIdStr, json);
            return;
        }

        // 4. 异步写 MySQL (不阻塞主流程)
        final String jsonForAsync = json;
        CompletableFuture.runAsync(() -> upsertMysql(memoryIdStr, jsonForAsync), ioExecutor);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String memoryIdStr = memoryId.toString();

        // 1. 删 Redis
        try {
            redisMemoryHelper.delete(memoryIdStr);
        } catch (Exception e) {
            log.warn("[DualMemory] Redis 删除失败 memoryId={}", memoryIdStr, e);
        }

        // 2. 删 MySQL
        try {
            LambdaQueryWrapper<ChatMemory> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ChatMemory::getMemoryId, memoryIdStr);
            chatMemoryMapper.delete(wrapper);
        } catch (Exception e) {
            log.error("[DualMemory] MySQL 删除失败 memoryId={}", memoryIdStr, e);
        }
    }

    /**
     * MySQL upsert (存在则更新,不存在则插入)
     * 异步调用,失败只记日志不抛异常
     */
    private void upsertMysql(String memoryIdStr, String json) {
        try {
            LambdaQueryWrapper<ChatMemory> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ChatMemory::getMemoryId, memoryIdStr);
            ChatMemory exist = chatMemoryMapper.selectOne(wrapper);

            if (exist == null) {
                ChatMemory newRecord = new ChatMemory()
                        .setMemoryId(memoryIdStr)
                        .setMesagesJson(json);
                chatMemoryMapper.insert(newRecord);
            } else {
                exist.setMesagesJson(json);
                chatMemoryMapper.updateById(exist);
            }
        } catch (Exception e) {
            log.error("[DualMemory] MySQL upsert 失败 memoryId={}", memoryIdStr, e);
        }
    }

    /**
     * 优雅关闭线程池
     */
    @jakarta.annotation.PreDestroy
    public void shutdown() {
        log.info("[DualMemory] 关闭 IO 线程池,等待剩余任务完成...");
        ioExecutor.shutdown();
        try {
            if (!ioExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                ioExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            ioExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
