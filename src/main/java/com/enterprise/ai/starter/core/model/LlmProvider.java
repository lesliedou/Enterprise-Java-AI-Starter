package com.enterprise.ai.starter.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 大语言模型供应商枚举类
 *
 * @author enterprise-ai-expert
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum LlmProvider {

    /**
     * OpenAI 官方模型
     */
    OPENAI("OpenAI"),

    /**
     * DeepSeek 深度求索
     */
    DEEPSEEK("DeepSeek"),

    /**
     * Anthropic Claude
     */
    CLAUDE("Claude"),

    /**
     * 阿里通义千问
     */
    QWEN("Qwen"),

    /**
     * 智谱 AI (清言/ChatGLM)
     */
    ZHIPU("Zhipu"),

    /**
     * 测试 Mock 模型
     */
    MOCK("Mock");

    private final String description;

    public String getCode() {
        return this.name().toLowerCase();
    }
}
