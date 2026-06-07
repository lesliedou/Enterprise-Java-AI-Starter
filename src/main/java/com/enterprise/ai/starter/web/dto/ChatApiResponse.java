package com.enterprise.ai.starter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应结果
 *
 * @author enterprise-ai-expert
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatApiResponse<T> {

    /**
     * 响应码 (200 为成功)
     */
    private Integer code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 追踪 ID
     */
    private String traceId;

    public static <T> ChatApiResponse<T> success(T data) {
        return ChatApiResponse.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .build();
    }

    public static <T> ChatApiResponse<T> error(Integer code, String message) {
        return ChatApiResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
}
