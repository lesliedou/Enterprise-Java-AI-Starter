package com.enterprise.ai.starter.core.annotation;

import java.lang.annotation.*;

/**
 * 语义缓存注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SemanticCache {
    
    /**
     * 相似度阈值
     */
    double threshold() default 0.93;

    /**
     * 缓存有效期 (秒)
     */
    int ttl() default 3600;
}
