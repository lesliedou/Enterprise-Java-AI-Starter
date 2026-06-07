package com.enterprise.ai.starter.core.strategy;

import com.enterprise.ai.starter.core.model.LlmProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天模型工厂
 * 使用策略模式管理所有供应商实现
 */
@Component
public class ChatModelFactory {

    private final Map<LlmProvider, ChatModelStrategy> strategies = new ConcurrentHashMap<>();

    public ChatModelFactory(List<ChatModelStrategy> strategyList) {
        strategyList.forEach(strategy -> strategies.put(strategy.getProvider(), strategy));
    }

    /**
     * 根据供应商获取对应的策略实现
     *
     * @param provider 供应商枚举
     * @return 聊天模型策略
     */
    public ChatModelStrategy getStrategy(LlmProvider provider) {
        ChatModelStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new RuntimeException("未找到对应的模型供应商实现: " + provider);
        }
        return strategy;
    }
}
