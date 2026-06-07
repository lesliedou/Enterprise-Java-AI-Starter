package com.enterprise.ai.starter.core.strategy;

import com.enterprise.ai.starter.core.model.LlmProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OpenAI 聊天策略实现
 */
@Component
public class OpenAiChatStrategy extends AbstractLangChain4jStrategy {

    @Value("${app.llm.openai.api-key:demo}")
    private String apiKey;

    @Override
    public LlmProvider getProvider() {
        return LlmProvider.OPENAI;
    }

    @Override
    protected ChatLanguageModel getChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    protected StreamingChatLanguageModel getStreamingChatLanguageModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .build();
    }
}
