package com.enterprise.ai.starter.core.aspect;

import com.enterprise.ai.starter.core.annotation.LlmProtection;
import com.enterprise.ai.starter.core.exception.AiServiceException;
import com.enterprise.ai.starter.core.limiter.RedisRateLimiter;
import com.enterprise.ai.starter.core.model.LlmProvider;
import com.enterprise.ai.starter.core.router.DynamicModelRouter;
import com.enterprise.ai.starter.core.strategy.ChatModelFactory;
import com.enterprise.ai.starter.core.strategy.ChatModelStrategy;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 大模型保护切面
 * 整合 Redis 限流与 Resilience4j 熔断超时
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LlmProtectionAspect {

    private final RedisRateLimiter redisRateLimiter;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final DynamicModelRouter modelRouter;
    private final ChatModelFactory modelFactory;

    @Around("@annotation(llmProtection)")
    public Object protect(ProceedingJoinPoint joinPoint, LlmProtection llmProtection) throws Throwable {
        String appKey = "default_enterprise_app"; 
        
        // 1. Redis 限流校验
        Boolean allowed = redisRateLimiter.isAllowed(appKey, llmProtection.capacity(), llmProtection.qps()).block();
        if (Boolean.FALSE.equals(allowed)) {
            throw new AiServiceException("请求过于频繁，触发企业级限流", "RATE_LIMIT_EXCEEDED");
        }

        // 2. 获取当前 Provider 参数
        LlmProvider provider = null;
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof LlmProvider) {
                provider = (LlmProvider) arg;
                break;
            }
        }
        
        String providerCode = (provider != null) ? provider.getCode() : "default";

        // 处理 Flux 流式响应
        if (joinPoint.getSignature().toLongString().contains("Flux")) {
            return handleFlux(joinPoint, llmProtection, providerCode);
        }

        // 处理普通同步响应
        return handleSync(joinPoint, llmProtection, providerCode);
    }

    private Object handleSync(ProceedingJoinPoint joinPoint, LlmProtection llmProtection, String providerCode) throws Throwable {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(providerCode);
        TimeLimiter tl = timeLimiterRegistry.timeLimiter(providerCode);
        
        try {
            // 使用 Resilience4j 装饰同步调用
            return cb.executeCallable(() -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Throwable e) {
            if (llmProtection.fallbackEnabled()) {
                log.warn("Provider [{}] 异常，触发自动降级: {}", providerCode, e.getMessage());
                return doFallbackSync(joinPoint);
            }
            throw e;
        }
    }

    private Flux<String> handleFlux(ProceedingJoinPoint joinPoint, LlmProtection llmProtection, String providerCode) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(providerCode);
        TimeLimiter tl = timeLimiterRegistry.timeLimiter(providerCode);

        try {
            return ((Flux<String>) joinPoint.proceed())
                    .transformDeferred(CircuitBreakerOperator.of(cb))
                    .transformDeferred(TimeLimiterOperator.of(tl))
                    .onErrorResume(e -> {
                        log.error("Provider [{}] Flux 调用异常，尝试降级: {}", providerCode, e.getMessage());
                        return doFallbackFlux(joinPoint);
                    });
        } catch (Throwable e) {
            return Flux.error(e);
        }
    }

    private String doFallbackSync(ProceedingJoinPoint joinPoint) {
        // 简单的重试/降级策略：重新路由并调用
        LlmProvider fallbackProvider = modelRouter.route();
        ChatModelStrategy strategy = modelFactory.getStrategy(fallbackProvider);
        log.info("同步降级选中备用模型: {}", fallbackProvider);
        return strategy.chat((String) joinPoint.getArgs()[0]);
    }

    private Flux<String> doFallbackFlux(ProceedingJoinPoint joinPoint) {
        LlmProvider fallbackProvider = modelRouter.route();
        ChatModelStrategy strategy = modelFactory.getStrategy(fallbackProvider);
        log.info("流式降级选中备用模型: {}", fallbackProvider);
        return strategy.streamChat((String) joinPoint.getArgs()[0]);
    }
}
