package com.enterprise.ai.starter.core.strategy;

import com.enterprise.ai.starter.core.model.LlmProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 聊天策略实现 (基于 OpenAI 协议兼容)
 */
@Component
public class DeepSeekChatStrategy extends AbstractLangChain4jStrategy {

    @Value("${app.llm.deepseek.api-key:demo}")
    private String apiKey;

    @Value("${app.llm.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Override
    public LlmProvider getProvider() {
        return LlmProvider.DEEPSEEK;
    }

    @Override
    protected ChatLanguageModel getChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    protected StreamingChatLanguageModel getStreamingChatLanguageModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }
}
