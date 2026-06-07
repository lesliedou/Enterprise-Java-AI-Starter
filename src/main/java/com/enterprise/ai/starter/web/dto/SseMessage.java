package com.enterprise.ai.starter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 流式消息体
 *
 * @author enterprise-ai-expert
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseMessage {

    /**
     * 内容片段
     */
    private String content;

    /**
     * 当前状态: GENERATING, DONE, ERROR
     */
    private String status;

    /**
     * 业务状态常量
     */
    public static final String STATUS_GENERATING = "GENERATING";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_ERROR = "ERROR";

    public static SseMessage generating(String content) {
        return SseMessage.builder()
                .content(content)
                .status(STATUS_GENERATING)
                .build();
    }

    public static SseMessage done() {
        return SseMessage.builder()
                .content("")
                .status(STATUS_DONE)
                .build();
    }

    public static SseMessage error(String message) {
        return SseMessage.builder()
                .content(message)
                .status(STATUS_ERROR)
                .build();
    }
}
