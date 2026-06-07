package com.enterprise.ai.starter.core.router;

import com.enterprise.ai.starter.core.model.LlmChannelConfig;
import java.util.List;

/**
 * 渠道配置仓库接口
 * 可通过数据库、Redis 或本地配置实现
 *
 * @author enterprise-ai-expert
 */
public interface ChannelConfigRepository {

    /**
     * 获取所有已启用的渠道配置
     *
     * @return List<LlmChannelConfig>
     */
    List<LlmChannelConfig> findAllEnabled();

    /**
     * 根据 ID 获取渠道配置
     *
     * @param channelId 渠道 ID
     * @return LlmChannelConfig
     */
    LlmChannelConfig findById(String channelId);

    /**
     * 保存或更新渠道配置
     *
     * @param config 渠道配置
     */
    void save(LlmChannelConfig config);

    /**
     * 删除渠道配置
     *
     * @param channelId 渠道 ID
     */
    void deleteById(String channelId);
}
