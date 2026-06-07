package com.enterprise.ai.starter.web.controller;

import com.enterprise.ai.starter.web.dto.ChatApiResponse;
import com.enterprise.ai.starter.web.dto.DashboardStatsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 监控统计控制器
 *
 * @author enterprise-ai-expert
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/stats")
public class DashboardStatsController {

    @GetMapping("/dashboard")
    public Mono<ChatApiResponse<DashboardStatsResponse>> getDashboardStats() {
        // 模拟汇总数据
        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .todayTokenUsage(125400L)
                .realtimeQps(8.5)
                .cacheHitRate(15.2)
                .health(DashboardStatsResponse.ChannelHealth.builder()
                        .onlineCount(2)
                        .totalCount(3)
                        .build())
                .usageDistributions(List.of(
                        new DashboardStatsResponse.ChannelUsageRatio("OpenAI", 70.0, "bg-blue-500"),
                        new DashboardStatsResponse.ChannelUsageRatio("DeepSeek", 20.0, "bg-emerald-500"),
                        new DashboardStatsResponse.ChannelUsageRatio("Other", 10.0, "bg-slate-300")
                ))
                .build();
        
        return Mono.just(ChatApiResponse.success(stats));
    }
}
