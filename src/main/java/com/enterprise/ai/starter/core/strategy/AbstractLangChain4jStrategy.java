package com.enterprise.ai.starter.core.strategy;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 基于 LangChain4j 的抽象策略基类
 */
public abstract class AbstractLangChain4jStrategy implements ChatModelStrategy {

    protected abstract ChatLanguageModel getChatModel();

    protected abstract StreamingChatLanguageModel getStreamingChatLanguageModel();

    @Override
    public String chat(String message) {
        return getChatModel().generate(message);
    }

    @Override
    public Flux<String> streamChat(String message) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        
        getStreamingChatLanguageModel().generate(message, new dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage>() {
            @Override
            public void onNext(String token) {
                sink.tryEmitNext(token);
            }

            @Override
            public void onComplete(dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> response) {
                sink.tryEmitComplete();
            }

            @Override
            public void onError(Throwable error) {
                sink.tryEmitError(error);
            }
        });

        return sink.asFlux();
    }
}
