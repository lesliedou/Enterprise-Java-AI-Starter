package com.enterprise.ai.starter.core.strategy;

import com.enterprise.ai.starter.core.model.LlmChannelConfig;
import com.enterprise.ai.starter.core.model.LlmProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * DeepSeek 供应商策略实现 (兼容 OpenAI 协议)
 *
 * @author enterprise-ai-expert
 */
@Component
public class DeepSeekProviderStrategy implements LlmProviderStrategy {

    @Override
    public LlmProvider getProvider() {
        return LlmProvider.DEEPSEEK;
    }

    @Override
    public ChatLanguageModel createChatModel(LlmChannelConfig config) {
        return OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .timeout(Duration.ofMillis(config.getTimeoutMillis()))
                .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatLanguageModel(LlmChannelConfig config) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .timeout(Duration.ofMillis(config.getTimeoutMillis()))
                .build();
    }
}
