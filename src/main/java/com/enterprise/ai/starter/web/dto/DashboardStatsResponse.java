package com.enterprise.ai.starter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 大盘统计响应 DTO
 *
 * @author enterprise-ai-expert
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    /**
     * 今日 Token 总消耗
     */
    private Long todayTokenUsage;

    /**
     * 实时请求 QPS
     */
    private Double realtimeQps;

    /**
     * 语义缓存命中率 (百分比)
     */
    private Double cacheHitRate;

    /**
     * 渠道健康状态
     */
    private ChannelHealth health;

    /**
     * 各渠道 Token 消耗分布
     */
    private List<ChannelUsageRatio> usageDistributions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelHealth {
        private Integer onlineCount;
        private Integer totalCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelUsageRatio {
        private String name;
        private Double ratio;
        private String color;
    }
}
