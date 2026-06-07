package com.enterprise.ai.starter.core.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态熔断管理器
 * 为每个渠道动态创建并管理 Resilience4j 熔断器
 *
 * @author enterprise-ai-expert
 */
@Slf4j
@Component
public class CircuitBreakerManager {

    private final CircuitBreakerRegistry registry;
    private final Map<String, CircuitBreaker> circuitBreakerMap = new ConcurrentHashMap<>();

    public CircuitBreakerManager() {
        // 初始化默认配置
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 失败率阈值 50%
                .slowCallRateThreshold(50) // 慢调用比例阈值 50%
                .slowCallDurationThreshold(Duration.ofSeconds(3)) // 慢调用定义：大于 3 秒
                .slidingWindowSize(10) // 滑动窗口大小 10 次
                .minimumNumberOfCalls(5) // 最小调用次数
                .waitDurationInOpenState(Duration.ofSeconds(10)) // 熔断后等待 10 秒进入半开状态
                .permittedNumberOfCallsInHalfOpenState(3) // 半开状态允许通过 3 次请求
                .build();
        this.registry = CircuitBreakerRegistry.of(defaultConfig);
    }

    /**
     * 获取或创建指定渠道的熔断器
     *
     * @param channelId 渠道 ID
     * @return CircuitBreaker
     */
    public CircuitBreaker getOrCreate(String channelId) {
        return circuitBreakerMap.computeIfAbsent(channelId, id -> {
            log.info("为渠道 [{}] 创建新的熔断器实例", id);
            CircuitBreaker cb = registry.circuitBreaker(id);
            // 监听熔断事件打印日志
            cb.getEventPublisher().onStateTransition(event -> 
                log.warn("渠道 [{}] 熔断器状态转换: {} -> {}", 
                        id, event.getStateTransition().getFromState(), event.getStateTransition().getToState()));
            return cb;
        });
    }
}
