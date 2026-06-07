package com.enterprise.ai.starter.core.limiter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * 基于 Redis Lua 脚本实现的分布式令牌桶限流器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final ReactiveStringRedisTemplate redisTemplate;
    
    private static final RedisScript<Long> RATE_LIMIT_SCRIPT;

    static {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/ratelimit.lua"));
        script.setResultType(Long.class);
        RATE_LIMIT_SCRIPT = script;
    }

    /**
     * 判断是否允许请求
     *
     * @param appKey 企业后台分发的 AppKey
     * @param capacity 令牌桶容量 (最大并发或最大突发)
     * @param rate 每秒生成令牌速率 (QPS)
     * @return Mono<Boolean> 是否允许
     */
    public Mono<Boolean> isAllowed(String appKey, int capacity, int rate) {
        String key = "ai:ratelimit:" + appKey;
        List<String> keys = Collections.singletonList(key);
        
        String now = String.valueOf(Instant.now().getEpochSecond());
        
        return redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                keys,
                List.of(String.valueOf(capacity), String.valueOf(rate), now, "1")
        ).next()
        .map(result -> result == 1)
        .doOnError(e -> log.error("Redis 限流脚本执行异常", e))
        .onErrorReturn(true); // Redis 故障时降级为允许请求，保证业务连续性
    }
}
