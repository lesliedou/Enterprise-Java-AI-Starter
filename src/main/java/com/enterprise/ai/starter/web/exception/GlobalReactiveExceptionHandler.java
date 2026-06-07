package com.enterprise.ai.starter.web.exception;

import com.enterprise.ai.starter.core.exception.ModelProviderException;
import com.enterprise.ai.starter.core.exception.NoAvailableChannelException;
import com.enterprise.ai.starter.web.dto.ChatApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 全局响应式异常处理器
 * 统一处理 AI 链路中的业务异常与系统异常
 *
 * @author enterprise-ai-expert
 */
@Slf4j
@Order(-2)
@Component
@RequiredArgsConstructor
public class GlobalReactiveExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.error("系统发生异常: ", ex);
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ChatApiResponse<Void> apiResponse;

        if (ex instanceof NoAvailableChannelException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            apiResponse = ChatApiResponse.error(503, ex.getMessage());
        } else if (ex instanceof ModelProviderException) {
            status = HttpStatus.BAD_GATEWAY;
            apiResponse = ChatApiResponse.error(502, "AI 供应商服务不可用: " + ex.getMessage());
        } else {
            apiResponse = ChatApiResponse.error(500, "服务器内部故障，请联系系统管理员");
        }

        response.setStatusCode(status);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(apiResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("异常处理序列化失败", e);
            return Mono.error(e);
        }
    }
}
