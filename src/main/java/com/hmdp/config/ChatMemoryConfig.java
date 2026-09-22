package com.hmdp.config;

import com.hmdp.repository.DualChatMemoryStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 会话记忆配置
 * 关键设计:
 * 1. 每个 memoryId 对应独立的 MessageWindowChatMemory (按用户+会话隔离)
 * 2. 通过 ChatMemoryProvider 让 LangChain4j 在请求时动态获取/创建对应 memoryId 的 ChatMemory
 * 3. MessageWindowChatMemory.maxMessages=50 作为兜底轮数限制
 *    (token 裁剪在 DualChatMemoryStore 里基于 jtokkit 精确控制,这里是第二层保护)
 */
@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemoryProvider chatMemoryProvider(DualChatMemoryStore store) {
        // 每次请求根据 memoryId 现取/现建一个 MessageWindowChatMemory
        // MessageWindowChatMemory 内部会调用 store.getMessages/updateMessages
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(50)              // 兜底轮数: 最多 50 条消息 (约 25 轮对话)
                .chatMemoryStore(store)       // 关键: 接入我们的双层 store
                .build();
    }
}
