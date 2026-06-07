package com.enterprise.ai.starter.web.filter;

import com.enterprise.ai.starter.web.dto.ChatApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 网关权限过滤器
 * 校验请求头中的 X-App-Key
 *
 * @author enterprise-ai-expert
 */
@Slf4j
@Component
public class AppKeyWebFilter implements WebFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String HEADER_APP_KEY = "X-App-Key";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 仅拦截 AI 对话相关接口
        if (!exchange.getRequest().getURI().getPath().startsWith("/api/v1/ai/chat")) {
            return chain.filter(exchange);
        }

        List<String> appKeys = exchange.getRequest().getHeaders().get(HEADER_APP_KEY);
        if (appKeys == null || appKeys.isEmpty() || !isValid(appKeys.get(0))) {
            log.warn("鉴权失败: 缺失或无效的 X-App-Key");
            return unauthorized(exchange);
        }

        return chain.filter(exchange);
    }

    private boolean isValid(String appKey) {
        // 生产环境应从 Redis 或数据库校验，此处演示简化逻辑
        return appKey != null && !appKey.isBlank();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ChatApiResponse<Void> error = ChatApiResponse.error(401, "Invalid or missing X-App-Key");
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(error);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }
}
