package com.enterprise.ai.starter.core.router;

import com.enterprise.ai.starter.core.model.ChatRequest;
import com.enterprise.ai.starter.core.model.ChatResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 路由服务接口
 *
 * @author enterprise-ai-expert
 */
public interface LlmRouterService {

    /**
     * 同步聊天 (响应式 Mono 封装)
     *
     * @param channelId 指定渠道 ID (可选)
     * @param request   聊天请求
     * @return Mono<ChatResponse>
     */
    Mono<ChatResponse> chatBlock(String channelId, ChatRequest request);

    /**
     * 流式聊天 (SSE 响应式 Flux)
     *
     * @param channelId 指定渠道 ID (可选)
     * @param request   聊天请求
     * @return Flux<String>
     */
    Flux<String> chatStream(String channelId, ChatRequest request);
}
