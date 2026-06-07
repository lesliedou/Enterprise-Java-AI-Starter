package com.enterprise.ai.starter.core.strategy;

import com.enterprise.ai.starter.core.exception.ModelProviderException;
import com.enterprise.ai.starter.core.model.LlmChannelConfig;
import com.enterprise.ai.starter.core.model.LlmProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 大模型客户端工厂
 * 采用策略模式管理不同供应商，并提供缓存功能
 *
 * @author enterprise-ai-expert
 */
@Slf4j
@Component
public class LlmClientFactory {

    private final Map<LlmProvider, LlmProviderStrategy> strategyMap = new ConcurrentHashMap<>();
    private final Map<String, ChatLanguageModel> chatModelCache = new ConcurrentHashMap<>();
    private final Map<String, StreamingChatLanguageModel> streamingChatModelCache = new ConcurrentHashMap<>();

    public LlmClientFactory(List<LlmProviderStrategy> strategies) {
        strategies.forEach(strategy -> strategyMap.put(strategy.getProvider(), strategy));
    }

    /**
     * 获取或创建同步聊天客户端
     *
     * @param config 渠道配置
     * @return ChatLanguageModel
     */
    public ChatLanguageModel getChatModel(LlmChannelConfig config) {
        return chatModelCache.computeIfAbsent(config.getId(), id -> {
            log.info("正在为渠道 [{}] 初始化同步聊天客户端...", config.getName());
            return getStrategy(config.getProvider()).createChatModel(config);
        });
    }

    /**
     * 获取或创建流式聊天客户端
     *
     * @param config 渠道配置
     * @return StreamingChatLanguageModel
     */
    public StreamingChatLanguageModel getStreamingChatLanguageModel(LlmChannelConfig config) {
        return streamingChatModelCache.computeIfAbsent(config.getId(), id -> {
            log.info("正在为渠道 [{}] 初始化流式聊天客户端...", config.getName());
            return getStrategy(config.getProvider()).createStreamingChatLanguageModel(config);
        });
    }

    /**
     * 清除指定渠道的客户端缓存
     *
     * @param channelId 渠道 ID
     */
    public void clearCache(String channelId) {
        chatModelCache.remove(channelId);
        streamingChatModelCache.remove(channelId);
    }

    private LlmProviderStrategy getStrategy(LlmProvider provider) {
        LlmProviderStrategy strategy = strategyMap.get(provider);
        if (strategy == null) {
            throw new ModelProviderException("不支持的模型供应商: " + provider);
        }
        return strategy;
    }
}
