package com.enterprise.ai.starter.core.event;

import com.enterprise.ai.starter.core.model.LlmProvider;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Token 使用情况事件
 * 用于异步计费和日志记录
 */
@Getter
public class TokenUsageEvent extends ApplicationEvent {

    private final String appKey;
    private final LlmProvider provider;
    private final int promptTokens;
    private final int completionTokens;
    private final int totalTokens;

    public TokenUsageEvent(Object source, String appKey, LlmProvider provider, int promptTokens, int completionTokens) {
        super(source);
        this.appKey = appKey;
        this.provider = provider;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = promptTokens + completionTokens;
    }
}
