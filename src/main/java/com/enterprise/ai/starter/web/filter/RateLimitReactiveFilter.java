package com.enterprise.ai.starter.web.filter;

import com.enterprise.ai.starter.web.dto.ChatApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * 响应式分布式限流过滤器
 * 基于 Redis Lua 脚本实现的令牌桶算法
 *
 * @author enterprise-ai-expert
 */
@Slf4j
@Component
public class RateLimitReactiveFilter implements WebFilter {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisScript<Long> rateLimitScript;

    private static final String HEADER_APP_KEY = "X-App-Key";

    public RateLimitReactiveFilter(ReactiveStringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/token_bucket.lua"));
        script.setResultType(Long.class);
        this.rateLimitScript = script;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!path.startsWith("/api/v1/ai/chat")) {
            return chain.filter(exchange);
        }

        String appKey = exchange.getRequest().getHeaders().getFirst(HEADER_APP_KEY);
        if (appKey == null || appKey.isBlank()) {
            return chain.filter(exchange); // 交给权限过滤器处理
        }

        // 获取限流配置 (生产环境可从 Redis 或配置中心获取)
        int capacity = 20; // 桶容量
        int rate = 5;      // QPS 5
        
        String key = "ai:ratelimit:" + appKey;
        String now = String.valueOf(Instant.now().getEpochSecond());

        return redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(key),
                List.of(String.valueOf(capacity), String.valueOf(rate), now, "1")
        ).next()
        .flatMap(allowed -> {
            if (allowed == 1) {
                return chain.filter(exchange);
            }
            log.warn("触发分布式限流: AppKey={}", appKey);
            return tooManyRequests(exchange);
        });
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ChatApiResponse<Void> error = ChatApiResponse.error(429, "Too many requests, please try again later.");
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(error);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }
}
