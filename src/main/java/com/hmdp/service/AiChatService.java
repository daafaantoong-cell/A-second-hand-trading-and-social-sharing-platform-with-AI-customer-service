package com.hmdp.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        contentRetriever = "blogContentRetriever"      // RAG：注入 Redis Stack 向量检索
)

public interface AiChatService {
    /**
     * 流式对话
     * @param memoryId   会话 ID,由调用方传入(userId:sessionId 格式)
     * @param text       用户输入
     */
    @SystemMessage(fromResource = "system.txt")
    Flux<String> fluxChat(@MemoryId Object memoryId, @UserMessage String text);
}
