package com.enterprise.ai.starter.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天请求体
 *
 * @author enterprise-ai-expert
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * 用户消息内容
     */
    private String message;

    /**
     * 历史消息 (可选)
     */
    private List<ChatMessage> history;

    /**
     * 温度参数
     */
    private Double temperature;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
