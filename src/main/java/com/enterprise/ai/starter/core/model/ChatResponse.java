package com.enterprise.ai.starter.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天响应体
 *
 * @author enterprise-ai-expert
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * 生成的内容
     */
    private String content;

    /**
     * 消耗的 Token 统计
     */
    private TokenUsage usage;

    /**
     * 使用的渠道 ID
     */
    private String channelId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
}
