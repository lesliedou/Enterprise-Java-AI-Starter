package com.enterprise.ai.starter.core.strategy;

import com.enterprise.ai.starter.core.model.LlmChannelConfig;
import com.enterprise.ai.starter.core.model.LlmProvider;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mock 模型供应商策略实现
 * 用于测试环境，无需真实 API Key
 */
@Slf4j
@Component
public class MockProviderStrategy implements LlmProviderStrategy {

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    @Override
    public LlmProvider getProvider() {
        return LlmProvider.MOCK;
    }

    @Override
    public ChatLanguageModel createChatModel(LlmChannelConfig config) {
        return (messages) -> {
            log.info("[MOCK] 收到同步对话请求, config: {}", config);
            return Response.from(AiMessage.from("这是一条来自 Mock 模型的测试响应。内容是：[Mock Output]"));
        };
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatLanguageModel(LlmChannelConfig config) {
        return (messages, handler) -> {
            log.info("[MOCK] 收到流式对话请求, config: {}", config);
            String fullText = "这是一条来自 Mock 模型的流式测试响应。我们正在模拟 AI 生成过程...";
            String[] words = fullText.split("");
            
            for (int i = 0; i < words.length; i++) {
                final String word = words[i];
                final int index = i;
                executor.schedule(() -> {
                    handler.onNext(word);
                    if (index == words.length - 1) {
                        handler.onComplete(Response.from(AiMessage.from(fullText)));
                    }
                }, i * 50, TimeUnit.MILLISECONDS);
            }
        };
    }
}
