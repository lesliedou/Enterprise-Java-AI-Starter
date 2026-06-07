package com.enterprise.ai.starter.web.controller;

import com.enterprise.ai.starter.core.model.ChatRequest;
import com.enterprise.ai.starter.core.router.LlmRouterService;
import com.enterprise.ai.starter.web.dto.ChatApiResponse;
import com.enterprise.ai.starter.web.dto.ChatQuery;
import com.enterprise.ai.starter.web.dto.SseMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AI 对话核心控制器
 * 支持标准响应与 SSE 流式响应
 *
 * @author enterprise-ai-expert
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final LlmRouterService routerService;
    private final ObjectMapper objectMapper;

    /**
     * 统一对话接口
     */
    @PostMapping(value = "/chat", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Mono chat(@RequestBody ChatQuery query) {
        log.info("收到对话请求: appId={}, stream={}", query.getAppId(), query.getStream());

        ChatRequest request = ChatRequest.builder()
                .message(query.getMessage())
                .temperature(query.getTemperature())
                .build();

        if (Boolean.FALSE.equals(query.getStream())) {
            return routerService.chatBlock(query.getChannelId(), request)
                    .map(ChatApiResponse::success)
                    .map(ResponseEntity::ok);
        }

        // 处理 SSE 流式响应
        return routerService.chatStream(query.getChannelId(), request)
                .map(token -> toSseEvent(SseMessage.generating(token)))
                .concatWith(Mono.just(toSseEvent(SseMessage.done())))
                .onErrorResume(e -> {
                    log.error("流式对话执行异常", e);
                    return Flux.just(toSseEvent(SseMessage.error(e.getMessage())));
                });
    }


    /**
     * 将业务消息体封装为 ServerSentEvent
     */
    private ServerSentEvent<String> toSseEvent(SseMessage message) {
        try {
            return ServerSentEvent.<String>builder()
                    .data(objectMapper.writeValueAsString(message))
                    .build();
        } catch (JsonProcessingException e) {
            log.error("SSE 序列化失败", e);
            return ServerSentEvent.<String>builder()
                    .data("{\"status\":\"ERROR\",\"content\":\"Serialization error\"}")
                    .build();
        }
    }
}
