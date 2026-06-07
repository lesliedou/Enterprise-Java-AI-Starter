package com.enterprise.ai.starter.web.controller;

import com.enterprise.ai.starter.core.model.LlmChannelConfig;
import com.enterprise.ai.starter.core.router.ChannelConfigRepository;
import com.enterprise.ai.starter.core.strategy.LlmClientFactory;
import com.enterprise.ai.starter.web.dto.ChatApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 渠道管理后台控制器
 *
 * @author enterprise-ai-expert
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/channels")
@RequiredArgsConstructor
public class LlmChannelAdminController {

    private final ChannelConfigRepository channelRepository;
    private final LlmClientFactory clientFactory;

    /**
     * 获取所有渠道列表
     */
    @GetMapping
    public Mono<ChatApiResponse<List<LlmChannelConfig>>> listChannels() {
        return Mono.just(ChatApiResponse.success(channelRepository.findAllEnabled()));
    }

    /**
     * 新增或修改渠道
     */
    @PostMapping
    public Mono<ChatApiResponse<LlmChannelConfig>> saveChannel(@RequestBody LlmChannelConfig config) {
        log.info("保存渠道配置: {}", config.getName());
        channelRepository.save(config);
        // 清除客户端缓存以使配置生效
        if (config.getId() != null) {
            clientFactory.clearCache(config.getId());
        }
        return Mono.just(ChatApiResponse.success(config));
    }

    /**
     * 切换渠道状态或重置
     */
    @PatchMapping("/{id}/status")
    public Mono<ChatApiResponse<Void>> updateStatus(@PathVariable String id, @RequestParam Boolean enabled) {
        log.info("更新渠道状态: id={}, enabled={}", id, enabled);
        LlmChannelConfig config = channelRepository.findById(id);
        if (config != null) {
            config.setIsEnabled(enabled);
            channelRepository.save(config);
            clientFactory.clearCache(id);
        }
        return Mono.just(ChatApiResponse.success(null));
    }

    /**
     * 删除渠道
     */
    @DeleteMapping("/{id}")
    public Mono<ChatApiResponse<Void>> deleteChannel(@PathVariable String id) {
        log.info("删除渠道: id={}", id);
        channelRepository.deleteById(id);
        clientFactory.clearCache(id);
        return Mono.just(ChatApiResponse.success(null));
    }
}
