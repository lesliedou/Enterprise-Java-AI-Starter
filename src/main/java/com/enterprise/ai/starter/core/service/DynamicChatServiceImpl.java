package com.enterprise.ai.starter.core.service;

import com.enterprise.ai.starter.core.annotation.LlmProtection;
import com.enterprise.ai.starter.core.annotation.SemanticCache;
import com.enterprise.ai.starter.core.event.TokenUsageEvent;
import com.enterprise.ai.starter.core.model.LlmProvider;
import com.enterprise.ai.starter.core.router.DynamicModelRouter;
import com.enterprise.ai.starter.core.strategy.ChatModelFactory;
import com.enterprise.ai.starter.core.strategy.ChatModelStrategy;
import com.enterprise.ai.starter.core.util.TokenCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 动态聊天服务实现类
 * 整合路由、高可用保护、Token 计费与语义缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicChatServiceImpl implements ChatService {

    private final DynamicModelRouter modelRouter;
    private final ChatModelFactory modelFactory;
    private final TokenCalculator tokenCalculator;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @SemanticCache(threshold = 0.95)
    public String chat(String message) {
        LlmProvider provider = modelRouter.route();
        return callWithProtection(provider, message);
    }

    @LlmProtection(qps = 5, capacity = 10)
    public String callWithProtection(LlmProvider provider, String message) {
        ChatModelStrategy strategy = modelFactory.getStrategy(provider);
        String response = strategy.chat(message);
        
        // 计算并发布 Token 使用事件
        int promptTokens = tokenCalculator.calculateTokens(message, provider.getCode());
        int completionTokens = tokenCalculator.calculateTokens(response, provider.getCode());
        eventPublisher.publishEvent(new TokenUsageEvent(this, "default_app", provider, promptTokens, completionTokens));
        
        return response;
    }

    @Override
    @SemanticCache(threshold = 0.95)
    public Flux<String> streamChat(String message) {
        LlmProvider provider = modelRouter.route();
        return streamWithProtection(provider, message);
    }

    @LlmProtection(qps = 5, capacity = 10)
    public Flux<String> streamWithProtection(LlmProvider provider, String message) {
        ChatModelStrategy strategy = modelFactory.getStrategy(provider);
        
        int promptTokens = tokenCalculator.calculateTokens(message, provider.getCode());
        StringBuilder fullContent = new StringBuilder();

        return strategy.streamChat(message)
                .doOnNext(fullContent::append)
                .doOnComplete(() -> {
                    int completionTokens = tokenCalculator.calculateTokens(fullContent.toString(), provider.getCode());
                    eventPublisher.publishEvent(new TokenUsageEvent(this, "default_app", provider, promptTokens, completionTokens));
                });
    }
}
