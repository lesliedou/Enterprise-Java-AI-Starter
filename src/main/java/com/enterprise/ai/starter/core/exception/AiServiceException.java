package com.enterprise.ai.starter.core.exception;

import lombok.Getter;

/**
 * 业务异常基类
 * 用于统一处理 AI 服务中的各类异常
 */
@Getter
public class AiServiceException extends RuntimeException {

    private final String errorCode;

    public AiServiceException(String message) {
        super(message);
        this.errorCode = "AI_SERVICE_ERROR";
    }

    public AiServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "AI_SERVICE_ERROR";
    }

    public AiServiceException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
