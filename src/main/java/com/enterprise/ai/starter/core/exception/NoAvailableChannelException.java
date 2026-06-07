package com.enterprise.ai.starter.core.exception;

/**
 * 无可用渠道异常
 *
 * @author enterprise-ai-expert
 */
public class NoAvailableChannelException extends RuntimeException {
    public NoAvailableChannelException(String message) {
        super(message);
    }
}
