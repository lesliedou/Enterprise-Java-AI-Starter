package com.enterprise.ai.starter.core.util;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Token 计算服务
 * 基于 JTokkit 实现离线精准计算
 *
 * @author enterprise-ai-expert
 */
@Slf4j
@Service
public class TokenCalculatorService {

    private final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    /**
     * 计算文本 Token 数量
     *
     * @param modelName 模型名称 (如 gpt-4, deepseek-chat)
     * @param text      待计算文本
     * @return Token 数量
     */
    public int countTokens(String modelName, String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        try {
            Encoding encoding = getEncoding(modelName);
            return encoding.countTokens(text);
        } catch (Exception e) {
            log.error("Token 计算失败, model: {}, error: {}", modelName, e.getMessage());
            // 降级使用 cl100k_base (GPT-4 默认)
            return registry.getEncoding(ModelType.GPT_4.getEncodingType()).countTokens(text);
        }
    }

    /**
     * 根据模型名称匹配 Encoding
     */
    private Encoding getEncoding(String modelName) {
        Optional<ModelType> modelType = ModelType.fromName(modelName);
        if (modelType.isPresent()) {
            return registry.getEncoding(modelType.get().getEncodingType());
        }
        
        // DeepSeek 等兼容模型通常使用 cl100k_base
        if (modelName.toLowerCase().contains("deepseek") || modelName.toLowerCase().contains("gpt")) {
            return registry.getEncoding(ModelType.GPT_4.getEncodingType());
        }
        
        // 默认返回通用编码器
        return registry.getEncoding(ModelType.GPT_3_5_TURBO.getEncodingType());
    }
}
