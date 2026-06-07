package com.enterprise.ai.starter.config;

import com.enterprise.ai.starter.core.model.LlmChannelConfig;
import com.enterprise.ai.starter.core.router.ChannelConfigRepository;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于配置文件的渠道仓库实现
 *
 * @author enterprise-ai-expert
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.ai")
public class YamlChannelConfigRepository implements ChannelConfigRepository {

    private List<LlmChannelConfig> channels = new ArrayList<>();

    @Override
    public List<LlmChannelConfig> findAllEnabled() {
        return channels.stream()
                .filter(LlmChannelConfig::getIsEnabled)
                .collect(Collectors.toList());
    }

    @Override
    public LlmChannelConfig findById(String channelId) {
        return channels.stream()
                .filter(c -> c.getId().equals(channelId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void save(LlmChannelConfig config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId(java.util.UUID.randomUUID().toString());
            channels.add(config);
        } else {
            for (int i = 0; i < channels.size(); i++) {
                if (channels.get(i).getId().equals(config.getId())) {
                    channels.set(i, config);
                    return;
                }
            }
            channels.add(config);
        }
    }

    @Override
    public void deleteById(String channelId) {
        channels.removeIf(c -> c.getId().equals(channelId));
    }
}
