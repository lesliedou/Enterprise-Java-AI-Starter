package com.enterprise.ai.starter.core.annotation;

import java.lang.annotation.*;

/**
 * 大模型调用保护注解
 * 集成限流、熔断防护
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LlmProtection {
    
    /**
     * 限流 QPS
     */
    int qps() default 10;

    /**
     * 令牌桶容量
     */
    int capacity() default 20;

    /**
     * 是否开启自动降级备用模型
     */
    boolean fallbackEnabled() default true;
}
