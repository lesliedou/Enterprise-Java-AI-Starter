package com.enterprise.ai.starter.config;

import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 相关基础组件配置
 */
@Configuration
public class AiAutoConfiguration {

    /**
     * 注入一个本地轻量级向量化模型
     * 生产环境推荐使用 OpenAIEmbeddingModel 或 DashScopeEmbeddingModel
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }
}
