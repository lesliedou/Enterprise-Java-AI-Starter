package com.enterprise.ai.starter.core.service;

import reactor.core.publisher.Flux;

/**
 * 聊天服务接口
 * 定义了标准对话和 SSE 流式对话的规范
 */
public interface ChatService {

    /**
     * 标准同步响应
     *
     * @param message 用户输入消息
     * @return 模型生成的回答
     */
    String chat(String message);

    /**
     * SSE 流式响应 (基于 WebFlux Flux)
     *
     * @param message 用户输入消息
     * @return 字符流响应
     */
    Flux<String> streamChat(String message);
}
