package com.enterprise.ai.starter.core.event;

import com.enterprise.ai.starter.core.model.LlmProvider;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Token 消耗事件
 * 用于异步扣费与审计
 *
 * @author enterprise-ai-expert
 */
@Getter
public class TokenConsumedEvent extends ApplicationEvent {

    private final String channelId;
    private final LlmProvider provider;
    private final String modelName;
    private final int promptTokens;
    private final int completionTokens;
    private final int totalTokens;
    private final String traceId;

    public TokenConsumedEvent(Object source, String channelId, LlmProvider provider, String modelName, 
                              int promptTokens, int completionTokens, String traceId) {
        super(source);
        this.channelId = channelId;
        this.provider = provider;
        this.modelName = modelName;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = promptTokens + completionTokens;
        this.traceId = traceId;
    }
}
