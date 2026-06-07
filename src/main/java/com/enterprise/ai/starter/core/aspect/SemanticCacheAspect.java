package com.enterprise.ai.starter.core.aspect;

import com.enterprise.ai.starter.core.cache.VectorSearchService;
import com.enterprise.ai.starter.core.model.ChatRequest;
import com.enterprise.ai.starter.core.model.ChatResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * 语义缓存切面
 * 实现流程：提问向量化 -> 相似度检索 -> 命中返回缓存 / 未命中回源并异步存入
 *
 * @author enterprise-ai-expert
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SemanticCacheAspect {

    private final EmbeddingModel embeddingModel;
    private final VectorSearchService vectorSearchService;
    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${app.ai.cache.semantic-threshold:0.93}")
    private double similarityThreshold;

    @Around("execution(* com.enterprise.ai.starter.core.router.LlmRouterService.chatBlock(..))")
    public Object aroundChatBlock(ProceedingJoinPoint joinPoint) throws Throwable {
        ChatRequest request = (ChatRequest) joinPoint.getArgs()[1];
        String userPrompt = request.getMessage();

        // 1. 提问向量化
        Embedding queryEmbedding = embeddingModel.embed(userPrompt).content();

        // 2. 语义检索
        return vectorSearchService.findNearest(queryEmbedding, similarityThreshold)
                .flatMap(result -> {
                    if (result.isPresent()) {
                        log.info("语义缓存命中 (同步)! 相似度: {}, Key: {}", result.get().getScore(), result.get().getCacheKey());
                        return redisTemplate.opsForValue().get(result.get().getCacheKey())
                                .map(cachedContent -> ChatResponse.builder()
                                        .content(cachedContent)
                                        .channelId("SEMANTIC_CACHE")
                                        .build());
                    }
                    
                    // 3. 未命中，回源调用
                    try {
                        return ((Mono<ChatResponse>) joinPoint.proceed())
                                .doOnNext(response -> asyncStoreCache(queryEmbedding, response.getContent()));
                    } catch (Throwable e) {
                        return Mono.error(e);
                    }
                });
    }

    @Around("execution(* com.enterprise.ai.starter.core.router.LlmRouterService.chatStream(..))")
    public Object aroundChatStream(ProceedingJoinPoint joinPoint) throws Throwable {
        ChatRequest request = (ChatRequest) joinPoint.getArgs()[1];
        String userPrompt = request.getMessage();

        Embedding queryEmbedding = embeddingModel.embed(userPrompt).content();

        return vectorSearchService.findNearest(queryEmbedding, similarityThreshold)
                .flatMapMany(result -> {
                    if (result.isPresent()) {
                        log.info("语义缓存命中 (流式)! 相似度: {}, Key: {}", result.get().getScore(), result.get().getCacheKey());
                        return redisTemplate.opsForValue().get(result.get().getCacheKey())
                                .flatMapMany(cachedContent -> Flux.just(cachedContent));
                    }

                    try {
                        StringBuilder fullContent = new StringBuilder();
                        return ((Flux<String>) joinPoint.proceed())
                                .doOnNext(fullContent::append)
                                .doOnComplete(() -> asyncStoreCache(queryEmbedding, fullContent.toString()));
                    } catch (Throwable e) {
                        return Flux.error(e);
                    }
                });
    }

    private void asyncStoreCache(Embedding embedding, String content) {
        String cacheKey = "ai:cache:semantic:" + UUID.randomUUID();
        log.info("异步存入语义缓存: key={}", cacheKey);
        
        // 存入 Redis (24小时) 并通知向量服务存储索引
        redisTemplate.opsForValue().set(cacheKey, content, Duration.ofHours(24))
                .then(vectorSearchService.store(embedding, cacheKey))
                .subscribe();
    }
}
