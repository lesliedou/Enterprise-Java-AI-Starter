package com.enterprise.ai.starter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话查询请求 DTO
 *
 * @author enterprise-ai-expert
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatQuery {

    /**
     * 用户提问内容 (必填)
     */
    private String message;

    /**
     * 是否使用流式响应 (默认 true)
     */
    @Builder.Default
    private Boolean stream = true;

    /**
     * 业务应用 ID (用于多租户/应用隔离)
     */
    private String appId;

    /**
     * 会话 ID (用于上下文追溯)
     */
    private String sessionId;

    /**
     * 模型温度参数
     */
    private Double temperature;
    
    /**
     * 指定渠道 ID (可选)
     */
    private String channelId;
}
