package com.enterprise.ai.starter.core.cache;

import dev.langchain4j.data.embedding.Embedding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * 向量检索 Mock 实现
 * 实际环境应对接 Milvus, Pinecone 或 pgvector
 *
 * @author enterprise-ai-expert
 */
@Slf4j
@Service
public class MockVectorSearchService implements VectorSearchService {

    @Override
    public Mono<Optional<SearchResult>> findNearest(Embedding queryEmbedding, double minScore) {
        log.debug("执行向量语义检索, 阈值: {}", minScore);
        // 模拟未命中逻辑
        return Mono.just(Optional.empty());
    }

    @Override
    public Mono<Void> store(Embedding embedding, String cacheKey) {
        log.info("异步存储语义索引: cacheKey={}", cacheKey);
        return Mono.empty();
    }
}
