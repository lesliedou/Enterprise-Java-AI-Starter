package com.enterprise.ai.starter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 应用配置 DTO
 *
 * @author enterprise-ai-expert
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfigResponse {

    private String appId;
    private String appKey;
    private Integer qpsLimit;
    private Long tokenBalance;
    private Boolean isEnabled;
    private String description;
}
