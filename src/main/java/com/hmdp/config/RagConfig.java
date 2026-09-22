package com.hmdp.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 配置
 * 关键设计：
 * 1. EmbeddingModel / EmbeddingStore 由 langchain4j-community-dashscope / langchain4j-community-redis
 *    starter 自动装配注入（application.yaml 中 langchain4j.community.* 配置）
 * 2. ContentRetriever 注入到 AiChatService 的 @AiService(contentRetriever = "blogContentRetriever")
 *    触发 RAG：用户问题 → embedding → Redis Stack 余弦相似度检索 Top-K → 注入 context
 * 3. maxResults=3：避免上下文过长；minScore=0.75：过滤低相关 blog 测评，避免污染回答
 */
@Configuration
public class RagConfig {

    @Bean
    public ContentRetriever blogContentRetriever(EmbeddingModel embeddingModel,
                                                 EmbeddingStore<TextSegment> embeddingStore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(3)
                .minScore(0.75)
                .build();
    }
}
