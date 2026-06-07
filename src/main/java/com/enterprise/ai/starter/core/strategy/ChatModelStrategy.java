package com.enterprise.ai.starter.core.strategy;

import com.enterprise.ai.starter.core.model.LlmProvider;
import reactor.core.publisher.Flux;

/**
 * 聊天模型策略接口
 * 不同的供应商（OpenAI, DeepSeek等）实现此接口
 */
public interface ChatModelStrategy {

    /**
     * 获取当前策略对应的供应商
     *
     * @return LlmProvider
     */
    LlmProvider getProvider();

    /**
     * 执行同步聊天
     *
     * @param message 消息内容
     * @return 响应内容
     */
    String chat(String message);

    /**
     * 执行流式聊天
     *
     * @param message 消息内容
     * @return 响应流
     */
    Flux<String> streamChat(String message);
}
