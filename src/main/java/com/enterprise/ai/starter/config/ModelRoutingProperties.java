package com.enterprise.ai.starter.config;

import com.enterprise.ai.starter.core.model.LlmProvider;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 模型路由配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.llm.routing")
public class ModelRoutingProperties {

    /**
     * 是否启用动态路由
     */
    private boolean enabled = true;

    /**
     * 路由节点列表
     */
    private List<RouteNode> nodes;

    @Data
    public static class RouteNode {
        /**
         * 模型供应商
         */
        private LlmProvider provider;
        
        /**
         * 权重 (1-100)
         */
        private int weight;
        
        /**
         * 优先级 (数字越小优先级越高)
         */
        private int priority;
        
        /**
         * 是否可用
         */
        private boolean active = true;
    }
}
