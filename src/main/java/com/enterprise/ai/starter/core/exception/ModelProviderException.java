package com.enterprise.ai.starter.core.exception;

/**
 * 模型供应商异常
 *
 * @author enterprise-ai-expert
 */
public class ModelProviderException extends RuntimeException {
    public ModelProviderException(String message) {
        super(message);
    }

    public ModelProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
