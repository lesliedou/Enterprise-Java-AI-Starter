package com.enterprise.ai.starter.web.controller;

import com.enterprise.ai.starter.web.dto.AppConfigResponse;
import com.enterprise.ai.starter.web.dto.ChatApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 应用管理控制器
 *
 * @author enterprise-ai-expert
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/apps")
public class AppAdminController {

    /**
     * 获取应用列表及其限流配置
     */
    @GetMapping
    public Mono<ChatApiResponse<List<AppConfigResponse>>> listApps() {
        // 模拟数据，实际应从数据库/Redis读取
        List<AppConfigResponse> apps = List.of(
                AppConfigResponse.builder()
                        .appId("app-001")
                        .appKey("admin-master-key")
                        .qpsLimit(10)
                        .tokenBalance(1000000L)
                        .isEnabled(true)
                        .description("主管理应用")
                        .build(),
                AppConfigResponse.builder()
                        .appId("app-002")
                        .appKey("test-key-123")
                        .qpsLimit(2)
                        .tokenBalance(50000L)
                        .isEnabled(true)
                        .description("测试沙箱")
                        .build()
        );
        return Mono.just(ChatApiResponse.success(apps));
    }
}
