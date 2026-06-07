package com.enterprise.ai.starter.core.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Token 计费监听器
 * 异步处理 Token 扣减逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBillingListener {

    private final ReactiveStringRedisTemplate redisTemplate;

    /**
     * 异步处理 Token 使用事件
     * 在生产环境中，这里会调用数据库或 Redis 扣减企业账户额度
     */
    @Async
    @EventListener
    public void handleTokenUsage(TokenUsageEvent event) {
        log.info("收到 Token 使用事件: AppKey={}, Provider={}, TotalTokens={}", 
                event.getAppKey(), event.getProvider(), event.getTotalTokens());

        String balanceKey = "ai:account:balance:" + event.getAppKey();
        String usageKey = "ai:account:usage:" + event.getAppKey();

        // 模拟 Redis 扣减额度
        redisTemplate.opsForValue().decrement(balanceKey, event.getTotalTokens())
                .doOnSuccess(remain -> log.debug("账户 {} 剩余额度: {}", event.getAppKey(), remain))
                .subscribe();

        // 模拟 Redis 累加使用量
        redisTemplate.opsForValue().increment(usageKey, event.getTotalTokens())
                .subscribe();
                
        // 可以在此处扩展写入 MySQL 流水表逻辑
    }
}
