package com.enterprise.ai.starter.core.strategy;

import com.enterprise.ai.starter.core.model.LlmChannelConfig;
import com.enterprise.ai.starter.core.model.LlmProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

/**
 * 大模型供应商策略接口
 *
 * @author enterprise-ai-expert
 */
public interface LlmProviderStrategy {

    /**
     * 获取供应商类型
     *
     * @return LlmProvider
     */
    LlmProvider getProvider();

    /**
     * 创建同步聊天模型
     *
     * @param config 渠道配置
     * @return ChatLanguageModel
     */
    ChatLanguageModel createChatModel(LlmChannelConfig config);

    /**
     * 创建流式聊天模型
     *
     * @param config 渠道配置
     * @return StreamingChatLanguageModel
     */
    StreamingChatLanguageModel createStreamingChatLanguageModel(LlmChannelConfig config);
}
