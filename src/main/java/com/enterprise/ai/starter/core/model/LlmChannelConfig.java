package com.enterprise.ai.starter.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 渠道配置 DTO
 *
 * @author enterprise-ai-expert
 * @since 1.0.0
 */
@Data
@Builder
public class LlmChannelConfig {

    /**
     * 渠道唯一标识
     */
    private String id;

    /**
     * 渠道名称
     */
    private String name;

    /**
     * 模型供应商
     */
    private LlmProvider provider;

    /**
     * API 密钥
     */
    private String apiKey;

    /**
     * 接口基础路径
     */
    private String baseUrl;

    /**
     * 模型名称 (如 gpt-4, deepseek-chat)
     */
    private String modelName;

    /**
     * 权重 (1-100)
     */
    private Integer weight;

    /**
     * 优先级 (数值越小优先级越高)
     */
    private Integer priority;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 超时时间 (毫秒)
     */
    private Long timeoutMillis;
}
