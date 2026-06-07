package com.enterprise.ai.starter.core.util;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Token 计算工具类
 * 基于 jtokkit 实现离线精准计算
 */
@Slf4j
@Component
public class TokenCalculator {

    private final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    /**
     * 计算文本的 Token 数量
     *
     * @param text 文本内容
     * @param modelName 模型名称 (如 gpt-3.5-turbo, gpt-4)
     * @return Token 数量
     */
    public int calculateTokens(String text, String modelName) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        try {
            // 尝试根据模型名称获取编码方式，默认为 CL100K_BASE (GPT-3.5/4 使用)
            Encoding encoding = getEncodingForModel(modelName);
            return encoding.countTokens(text);
        } catch (Exception e) {
            log.error("Token 计算失败, model: {}, error: {}", modelName, e.getMessage());
            // 降级使用默认编码
            return registry.getEncoding(ModelType.GPT_3_5_TURBO.getEncodingType()).countTokens(text);
        }
    }

    private Encoding getEncodingForModel(String modelName) {
        Optional<ModelType> modelType = ModelType.fromName(modelName);
        if (modelType.isPresent()) {
            return registry.getEncoding(modelType.get().getEncodingType());
        }
        // 对于不识别的模型名（如国内模型），通常可以使用 GPT-3.5 的编码器作为近似估算
        return registry.getEncoding(ModelType.GPT_3_5_TURBO.getEncodingType());
    }
}
