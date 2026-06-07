package com.enterprise.ai.starter.core.router;

import com.enterprise.ai.starter.core.event.TokenConsumedEvent;
import com.enterprise.ai.starter.core.exception.NoAvailableChannelException;
import com.enterprise.ai.starter.core.model.ChatRequest;
import com.enterprise.ai.starter.core.model.ChatResponse;
import com.enterprise.ai.starter.core.model.LlmChannelConfig;
import com.enterprise.ai.starter.core.model.LlmProvider;
import com.enterprise.ai.starter.core.resilience.CircuitBreakerManager;
import com.enterprise.ai.starter.core.strategy.LlmClientFactory;
import com.enterprise.ai.starter.core.util.TokenCalculatorService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态模型路由器实现类
 * 负责渠道选择、负载均衡、自动熔断、降级重试以及 Token 精准审计
 *
 * @author enterprise-ai-expert
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicModelRouter implements LlmRouterService {

    private final LlmClientFactory clientFactory;
    private final ChannelConfigRepository channelRepository;
    private final CircuitBreakerManager circuitBreakerManager;
    private final TokenCalculatorService tokenCalculator;
    private final ApplicationEventPublisher eventPublisher;
    private final Random random = new Random();

    public LlmProvider route() {
        return channelRepository.findAllEnabled().stream()
                .findFirst()
                .map(LlmChannelConfig::getProvider)
                .orElse(LlmProvider.MOCK);
    }

    @Override
    public Mono<ChatResponse> chatBlock(String channelId, ChatRequest request) {
        return chatBlockWithRetry(channelId, request, new HashSet<>());
    }

    /**
     * 带重试机制的同步调用
     */
    private Mono<ChatResponse> chatBlockWithRetry(String channelId, ChatRequest request, Set<String> excludedIds) {
        return Mono.defer(() -> {
            LlmChannelConfig config = selectChannel(channelId, excludedIds);
            CircuitBreaker cb = circuitBreakerManager.getOrCreate(config.getId());
            long startTime = System.currentTimeMillis();

            return Mono.fromCallable(() -> clientFactory.getChatModel(config).generate(request.getMessage()))
                    .transformDeferred(CircuitBreakerOperator.of(cb))
                    .map(content -> {
                        // 离线计算 Token 并发布事件
                        int promptTokens = tokenCalculator.countTokens(config.getModelName(), request.getMessage());
                        int completionTokens = tokenCalculator.countTokens(config.getModelName(), content);
                        eventPublisher.publishEvent(new TokenConsumedEvent(this, config.getId(), config.getProvider(), 
                                config.getModelName(), promptTokens, completionTokens, UUID.randomUUID().toString()));
                        
                        return ChatResponse.builder()
                                .content(content)
                                .channelId(config.getId())
                                .build();
                    })
                    .onErrorResume(e -> {
                        long duration = System.currentTimeMillis() - startTime;
                        log.warn("渠道 [{}] 调用失败或熔断 (耗时 {}ms), 错误: {}. 正在尝试降级...", 
                                config.getName(), duration, e.getMessage());
                        
                        excludedIds.add(config.getId());
                        return chatBlockWithRetry(null, request, excludedIds);
                    });
        });
    }

    @Override
    public Flux<String> chatStream(String channelId, ChatRequest request) {
        return chatStreamWithRetry(channelId, request, new HashSet<>());
    }

    /**
     * 带重试机制的流式调用
     */
    private Flux<String> chatStreamWithRetry(String channelId, ChatRequest request, Set<String> excludedIds) {
        return Flux.defer(() -> {
            try {
                LlmChannelConfig config = selectChannel(channelId, excludedIds);
                CircuitBreaker cb = circuitBreakerManager.getOrCreate(config.getId());
                long startTime = System.currentTimeMillis();
                
                StringBuilder completionContent = new StringBuilder();

                return Flux.<String>create(sink -> {
                    clientFactory.getStreamingChatLanguageModel(config).generate(request.getMessage(), new StreamingResponseHandler<AiMessage>() {
                        @Override
                        public void onNext(String token) {
                            completionContent.append(token);
                            sink.next(token);
                        }

                        @Override
                        public void onComplete(Response<AiMessage> response) {
                            // 流结束，计算 Token 并审计
                            int promptTokens = tokenCalculator.countTokens(config.getModelName(), request.getMessage());
                            int completionTokens = tokenCalculator.countTokens(config.getModelName(), completionContent.toString());
                            eventPublisher.publishEvent(new TokenConsumedEvent(this, config.getId(), config.getProvider(), 
                                    config.getModelName(), promptTokens, completionTokens, UUID.randomUUID().toString()));
                            
                            sink.complete();
                        }

                        @Override
                        public void onError(Throwable error) {
                            sink.error(error);
                        }
                    });
                })
                .transformDeferred(CircuitBreakerOperator.of(cb))
                .onErrorResume(e -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.warn("流式渠道 [{}] 异常 (耗时 {}ms), 错误: {}. 正在尝试降级重试...", 
                            config.getName(), duration, e.getMessage());
                    
                    excludedIds.add(config.getId());
                    return chatStreamWithRetry(null, request, excludedIds);
                });
            } catch (NoAvailableChannelException e) {
                return Flux.error(e);
            }
        });
    }

    /**
     * 增强版渠道选择逻辑
     * 支持排除已失败的渠道，用于实现降级重试
     */
    private LlmChannelConfig selectChannel(String channelId, Set<String> excludedIds) {
        if (channelId != null && !channelId.isBlank() && !excludedIds.contains(channelId)) {
            LlmChannelConfig config = channelRepository.findById(channelId);
            if (config != null && config.getIsEnabled()) {
                return config;
            }
        }

        List<LlmChannelConfig> availableChannels = channelRepository.findAllEnabled().stream()
                .filter(c -> !excludedIds.contains(c.getId()))
                .collect(Collectors.toList());

        if (availableChannels.isEmpty()) {
            throw new NoAvailableChannelException("无可用的 AI 渠道或所有可用渠道已耗尽");
        }

        // 1. 按优先级排序
        Integer minPriority = availableChannels.stream()
                .map(LlmChannelConfig::getPriority)
                .min(Integer::compare)
                .get();

        // 2. 过滤最高优先级集合
        List<LlmChannelConfig> topPriorityChannels = availableChannels.stream()
                .filter(c -> c.getPriority().equals(minPriority))
                .collect(Collectors.toList());

        // 3. 按权重随机选择
        int totalWeight = topPriorityChannels.stream().mapToInt(LlmChannelConfig::getWeight).sum();
        if (totalWeight <= 0) return topPriorityChannels.get(0);
        
        int r = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (LlmChannelConfig config : topPriorityChannels) {
            currentWeight += config.getWeight();
            if (r < currentWeight) {
                return config;
            }
        }
        return topPriorityChannels.get(0);
    }
}
