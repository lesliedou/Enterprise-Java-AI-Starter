package com.enterprise.ai.starter.core.cache;

import dev.langchain4j.data.embedding.Embedding;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * 向量检索服务接口
 *
 * @author enterprise-ai-expert
 */
public interface VectorSearchService {

    /**
     * 检索最相似的缓存结果
     *
     * @param queryEmbedding 查询向量
     * @param minScore       最小相似度阈值
     * @return 匹配结果
     */
    Mono<Optional<SearchResult>> findNearest(Embedding queryEmbedding, double minScore);

    /**
     * 异步存储向量索引与内容 Key
     *
     * @param embedding 向量
     * @param cacheKey  Redis 缓存 Key
     * @return Mono<Void>
     */
    Mono<Void> store(Embedding embedding, String cacheKey);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SearchResult {
        private String cacheKey;
        private double score;
    }
}
