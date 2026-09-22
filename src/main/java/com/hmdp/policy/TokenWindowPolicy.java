package com.hmdp.policy;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 token 的会话窗口裁剪策略
 *
 * 设计原则:
 * 1. 从消息列表末尾向前累加 token,保留最新内容(用户最近说过的话不能丢)
 * 2. SystemMessage 永远保留(人设/系统提示不能丢),且不占预算(相当于"白送")
 * 3. 超过预算时丢弃最早的非 System 消息
 *
 * 用 jtokkit (OpenAI 同款 BPE 算法) 精确计算 token,比按字符数估算准
 */
@Slf4j
@Component
public class TokenWindowPolicy {

    private final EncodingRegistry registry;
    private final Encoding encoding;
    private final int maxTokens;

    /**
     * @param maxTokens token 预算上限,默认 4000 (DeepSeek deepseek-chat 模型上下文 32K,留充足空间给回复)
     */
    public TokenWindowPolicy(@Value("${hmdp.chat.memory.max-tokens:4000}") int maxTokens) {
        this.registry = Encodings.newDefaultEncodingRegistry();
        this.encoding = registry.getEncoding(EncodingType.CL100K_BASE);  // GPT-3.5/4 同款分词器
        this.maxTokens = maxTokens;
    }

    /**
     * 按预算裁剪消息列表
     * @param messages 原始消息列表(可能超过预算)
     * @return 裁剪后的列表,第一条永远是 SystemMessage (如果原始有的话)
     */
    public List<ChatMessage> trim(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 抽出所有 SystemMessage (通常只有 1 条,但兼容多条)
        List<ChatMessage> systemMessages = new ArrayList<>();
        List<ChatMessage> nonSystemMessages = new ArrayList<>();
        for (ChatMessage m : messages) {
            if (m instanceof SystemMessage) {
                systemMessages.add(m);
            } else {
                nonSystemMessages.add(m);
            }
        }

        // 2. 计算系统消息占用 token (不占预算,但要算总量,避免总长超模型限制)
        int systemTokens = systemMessages.stream()
                .mapToInt(this::countTokens)
                .sum();

        // 3. 从末尾向前累加非系统消息,直到超预算
        List<ChatMessage> keptNonSystem = new ArrayList<>();
        int usedTokens = 0;
        for (int i = nonSystemMessages.size() - 1; i >= 0; i--) {
            ChatMessage m = nonSystemMessages.get(i);
            int msgTokens = countTokens(m);
            if (usedTokens + msgTokens > maxTokens) {
                log.debug("[TokenWindow] 裁剪 {} 条消息,保留 {} 条,系统消息 token={},非系统 token={}",
                        i + 1, keptNonSystem.size(), systemTokens, usedTokens);
                break;
            }
            usedTokens += msgTokens;
            keptNonSystem.add(0, m);  // 头插,保持原顺序
        }

        // 4. 拼装结果: SystemMessage 在前,非系统消息按原顺序在后
        List<ChatMessage> result = new ArrayList<>(systemMessages.size() + keptNonSystem.size());
        result.addAll(systemMessages);
        result.addAll(keptNonSystem);
        return result;
    }

    /**
     * 估算单条消息的 token 数
     * 用 ChatMessage.text() 取文本,jtokkit 精确分词
     */
    private int countTokens(ChatMessage m) {
        try {
            // 用反射或 toString 兜底,因为 ChatMessage 接口不一定有 text() 方法
            // 这里简化处理:用 toString() 取文本
            String text = m.toString();
            return encoding.countTokens(text);
        } catch (Exception e) {
            log.warn("[TokenWindow] 估算 token 失败,降级为字符数估算: {}", e.getMessage());
            // 降级:1 字符 ≈ 1.5 token (中文比例)
            return (int) (m.toString().length() * 1.5);
        }
    }
}
