# Enterprise Java AI Starter — Bugfix 技术设计文档

## Overview

本次修复针对系统中 6 类结构性缺陷，目标是在不破坏现有 OpenAI / DeepSeek / Mock 三条路径的前提下，补全多模型支持、规范化异常与响应体系、消除双重路由冲突，并使 AOP 切面真正生效。

**核心策略：最小侵入、向后兼容。**  
所有改动均遵循开闭原则——新增 `ClaudeProviderStrategy` / `QwenProviderStrategy` / `ZhipuProviderStrategy` 等实现类，而非修改工厂；重命名响应包装类时保留旧类作委托或标记 `@Deprecated`；路由重构通过引入 `LlmRouterService` 代理层而非直接删除旧方法。

---

## Glossary

| 术语 | 定义 |
|------|------|
| **Bug_Condition (C)** | 触发缺陷的输入条件集合，本次共 6 个互相独立的条件 C1~C6 |
| **Property (P)** | 修复后对应输入下应满足的正确行为断言 |
| **Preservation (¬C)** | 不触发缺陷的输入路径，修复后行为必须与修复前完全一致 |
| **F** | 原始（未修复）函数或流程 |
| **F'** | 修复后函数或流程 |
| **LlmProviderStrategy** | 供应商策略接口，位于 `core/strategy/LlmProviderStrategy.java` |
| **LlmClientFactory** | 通过 `List<LlmProviderStrategy>` 自动装配所有策略的工厂，位于 `core/strategy/LlmClientFactory.java` |
| **DynamicModelRouter** | 核心路由实现，实现 `LlmRouterService`，位于 `core/router/DynamicModelRouter.java` |
| **DynamicChatServiceImpl** | 旧版服务实现，当前绕过 AOP 代理，位于 `core/service/DynamicChatServiceImpl.java` |
| **LlmRouterService** | 路由服务接口，`SemanticCacheAspect` 已对其切点进行监听 |
| **ApiResponse\<T\>** | 约定的统一响应包装类（替换 `ChatApiResponse<T>`） |
| **AiException** | 新增的 AI 业务异常统一基类 |
| **traceId** | 统一从 `MDC.get("traceId")` 获取的请求追踪标识 |

---

## 架构概览

### 修复后的完整调用链

```
HTTP Request
    │
    ▼
AppKeyWebFilter / RateLimitReactiveFilter
    │
    ▼
AiChatController
    │  调用 LlmRouterService 接口（Spring Bean）
    ▼
┌─────────────────────────────────────────────┐
│           Spring AOP Proxy Layer            │
│  ┌─────────────────────────────────────┐   │
│  │  SemanticCacheAspect                │   │  ← 切点：LlmRouterService.chatBlock/chatStream
│  │  (切点: LlmRouterService 接口方法)  │   │
│  └─────────────────────────────────────┘   │
│  ┌─────────────────────────────────────┐   │
│  │  LlmProtectionAspect                │   │  ← 切点：@LlmProtection 注解（标注在 LlmRouterService impl 上）
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
    │
    ▼
DynamicModelRouter.chatBlock() / chatStream()
    │
    ├── ChannelConfigRepository.findAllEnabled()
    │       └── YamlChannelConfigRepository（读取含 maxRetries 的渠道配置）
    │
    ├── selectChannel()（优先级 + 权重加权随机）
    │
    ├── CircuitBreakerManager.getOrCreate(channelId)
    │
    └── LlmClientFactory.getChatModel(config)
            │
            └── strategyMap.get(provider)
                    ├── OpenAiProviderStrategy    [已有]
                    ├── DeepSeekProviderStrategy  [已有]
                    ├── MockProviderStrategy      [已有]
                    ├── ClaudeProviderStrategy    [新增 BUG-1]
                    ├── QwenProviderStrategy      [新增 BUG-1]
                    └── ZhipuProviderStrategy     [新增 BUG-1]
```

### 异常传播链（修复后）

```
LlmClientFactory.getStrategy(UNSUPPORTED)
    └─→ ModelUnavailableException（继承 AiException）[BUG-3]

DynamicModelRouter.selectChannel()（所有渠道已耗尽）
    └─→ AllModelsFailedException（继承 AiException）[BUG-3]

GlobalReactiveExceptionHandler
    ├── AllModelsFailedException  → HTTP 503
    ├── ModelUnavailableException → HTTP 502
    ├── NoAvailableChannelException → HTTP 503
    └── 其他异常               → HTTP 500
    └── 统一返回 ApiResponse.error(code, message, traceId)  [BUG-4]
```

---

## Bug Details

### Bug Condition 汇总

```
FUNCTION isBugCondition(input)
  INPUT: input 为系统运行时请求或配置状态
  OUTPUT: boolean（true = 存在缺陷）

  C1: input.channelConfig.provider IN [CLAUDE, QWEN, ZHIPU]
      AND NOT exists(LlmProviderStrategy where strategy.getProvider() == input.channelConfig.provider)

  C2: LlmProvider.valueOf(input.provider).displayName == null
      OR LlmProvider.valueOf(input.provider).displayName == LlmProvider.valueOf(input.provider).description

  C3: (thrown exception instanceof AllModelsFailedException == false
       AND all channels exhausted == true)
      OR (thrown exception instanceof ModelUnavailableException == false
          AND single provider call failed == true)
      OR (NoAvailableChannelException.superclass != AiException)

  C4: response.class == ChatApiResponse
      OR response.successCode == 200 (应为 0)
      OR response.traceId source != MDC.get("traceId")
      OR sseErrorFormat != "event: error\ndata: {...}\n\n"

  C5: LlmChannelConfig.maxRetries == null
      AND per-channel retry control required == true

  C6: DynamicChatServiceImpl.chat() calls modelRouter.route()
      AND DynamicChatServiceImpl.chat() calls ChatModelFactory directly
      AND @LlmProtection annotated on non-proxy method callWithProtection()
      AND @SemanticCache annotated on non-proxy method (but NOT on LlmRouterService interface method)

  RETURN C1 OR C2 OR C3 OR C4 OR C5 OR C6
END FUNCTION
```

### Bug 具体表现示例

**C1（策略缺失）**
- 输入：渠道配置 `provider: CLAUDE`，发起聊天请求
- 实际：`LlmClientFactory.getStrategy(CLAUDE)` → `strategyMap.get(CLAUDE) == null` → 抛出 `ModelProviderException("不支持的模型供应商: CLAUDE")`
- 期望：正常创建 Claude 客户端并返回响应

**C2（枚举字段缺失）**
- 输入：序列化 `LlmProvider.QWEN` 为 JSON
- 实际：JSON 中无 `displayName` 字段，或 `displayName == description == "Qwen"`
- 期望：`{"name":"QWEN","displayName":"通义千问 (Qwen)","description":"阿里云通义千问系列模型（DashScope SDK）"}`

**C3（异常体系）**
- 输入：3 个渠道全部熔断，再次发起请求
- 实际：`DynamicModelRouter` 抛出 `NoAvailableChannelException`（语义不明确），`GlobalReactiveExceptionHandler` 无法区分"全部失败"与"无渠道"
- 期望：抛出 `AllModelsFailedException`，HTTP 503，错误信息明确表达"所有模型均不可用"

**C4（响应不统一）**
- 输入：调用 `/api/v1/ai/chat` 成功
- 实际：响应体 `{"code":200,"message":"success","data":{...}}`，`traceId` 为 null（`ChatApiResponse.success()` 未填充）
- 期望：`{"code":0,"message":"success","data":{...},"traceId":"xxx-yyy"}`

**C5（maxRetries 缺失）**
- 输入：YAML 中配置 `maxRetries: 2`
- 实际：`LlmChannelConfig` 无该字段，Jackson 反序列化时忽略，渠道级重试不可配置
- 期望：`config.getMaxRetries()` 返回 2，路由器以此覆盖全局重试策略

**C6（双重路由 / AOP 不生效）**
- 输入：发起正常聊天请求
- 实际调用链：`DynamicChatServiceImpl.chat()` → `modelRouter.route()` → `ChatModelFactory.getStrategy(provider)` → 直接调用（绕过 `DynamicModelRouter.chatBlock/chatStream`）；`@LlmProtection` 标注在 `callWithProtection()` 上，该方法由 `this.callWithProtection()` 调用，不经过 Spring AOP 代理，切面不生效
- 期望：请求统一经 `DynamicModelRouter.chatBlock/chatStream`，`@LlmProtection` 和 `@SemanticCache` 切面均被触发

---

## Expected Behavior

### Preservation Requirements（必须保持不变的行为）

**Unchanged Behaviors:**
- `OpenAiProviderStrategy` 的 `createChatModel` / `createStreamingChatLanguageModel` 逻辑不变
- `DeepSeekProviderStrategy` 的 OpenAI 兼容接口调用逻辑不变
- `MockProviderStrategy` 的模拟响应逻辑不变
- `DynamicModelRouter.selectChannel()` 的优先级+权重加权随机算法不变
- `CircuitBreakerManager` 的熔断器创建和状态管理逻辑不变
- `YamlChannelConfigRepository` 的配置加载和 CRUD 逻辑不变
- `TokenBillingListener` / `TokenConsumedEvent` 的 Token 审计机制不变
- `AiChatController` 的 SSE 格式（正常 token 事件格式）不变
- `SemanticCacheAspect` 的切点（已指向 `LlmRouterService`）不变

**Non-affected Scope:**
所有 `provider ∈ {OPENAI, DEEPSEEK, MOCK}` 的渠道请求，在修复后必须产生与修复前完全相同的响应行为。

---

## Hypothesized Root Cause

**BUG-1 根因：** 项目初始化时只创建了 OpenAI / DeepSeek / Mock 三个 `@Component`，LangChain4j 没有针对 Claude、Qwen、Zhipu 的统一 builder（不同于 OpenAI），开发者未补全三个策略实现。`LlmClientFactory` 的构造函数在 `strategies.forEach(s -> strategyMap.put(s.getProvider(), s))` 时自然找不到这三个 key。

**BUG-2 根因：** 枚举设计时只考虑了内部使用，`description` 字段同时承担了"展示名"和"技术描述"两个语义，未拆分。

**BUG-3 根因：** 异常类各自继承 `RuntimeException`，`AiServiceException` 作为基类但未被 `ModelProviderException` / `NoAvailableChannelException` 继承，且缺少表达"全部失败"语义的 `AllModelsFailedException` 和表达"单一供应商失败"语义的 `ModelUnavailableException`。

**BUG-4 根因：** `ChatApiResponse<T>` 的 `success()` 工厂方法硬编码了 `code=200`（HTTP 状态码混入业务码），`traceId` 在工厂方法中未填充。SSE 错误路径直接将 `SseMessage.error()` 包装为 JSON，而非遵循 SSE `event: error` 格式规范。

**BUG-5 根因：** `LlmChannelConfig` 建模时遗漏了 `maxRetries` 字段，`DynamicModelRouter` 在重试时没有读取渠道级配置，只有 `excludedIds` 机制（降级选其他渠道），没有对同一渠道的重试次数上限控制。

**BUG-6 根因：** `DynamicChatServiceImpl` 是旧架构残留。其 `chat()` 方法调用 `modelRouter.route()` 返回 `LlmProvider`，再经 `ChatModelFactory.getStrategy(provider)` 直接执行，完全绕过了已实现的 `DynamicModelRouter.chatBlock()`。`@LlmProtection` 标注在 `callWithProtection()` 上，但该方法通过 `this.callWithProtection()` 调用（非代理调用），Spring AOP 不拦截。

---

## Correctness Properties

Property 1: Bug Condition — 全供应商策略完整注册

_For any_ 渠道配置 `config` 满足 `config.provider ∈ {CLAUDE, QWEN, ZHIPU}` 且 `config.apiKey` 非空、`config.baseUrl` 格式合法，修复后的 `LlmClientFactory.getChatModel(config)` SHALL 成功返回对应供应商的 `ChatLanguageModel` 实例，不抛出任何异常，且后续 `generate(message)` 调用能够正常转发请求。

**Validates: Requirements 2.1, 2.2, 2.10, 2.11**

---

Property 2: Preservation — 已有供应商行为不变

_For any_ 渠道配置 `config` 满足 `config.provider ∈ {OPENAI, DEEPSEEK, MOCK}`，修复后的 `LlmClientFactory.getChatModel(config)` SHALL 产生与修复前完全相同的 `ChatLanguageModel` 实现类实例，`generate(message)` 的返回结果与原实现一致。

**Validates: Requirements 3.1, 3.2, 3.3**

---

Property 3: Bug Condition — 枚举双字段独立性

_For any_ `provider ∈ LlmProvider.values()`，修复后 `provider.getDisplayName()` 和 `provider.getDescription()` SHALL 均不为 null，且两者的值 SHALL 不相等，JSON 序列化结果中 `displayName` 和 `description` 字段 SHALL 同时存在且值不同。

**Validates: Requirements 2.3, 2.12**

---

Property 4: Bug Condition — 异常体系完整性

_For any_ 系统运行状态满足"所有已启用渠道均已耗尽"时，修复后的 `DynamicModelRouter` SHALL 抛出 `AllModelsFailedException`（`AllModelsFailedException instanceof AiException == true`），且 `GlobalReactiveExceptionHandler` SHALL 将其映射为 HTTP 503。_For any_ 单一供应商调用失败时，SHALL 抛出 `ModelUnavailableException`（`ModelUnavailableException instanceof AiException == true`）。

**Validates: Requirements 2.4, 2.5, 2.6**

---

Property 5: Bug Condition — 统一响应格式

_For any_ 非 SSE 响应路径（同步或错误），修复后的响应 SHALL 满足：`response.code == 0`（成功）或约定业务错误码，`response.traceId` 与当前请求 MDC `traceId` 一致，响应类为 `ApiResponse<T>`。_For any_ SSE 错误事件，SHALL 以 `event: error\ndata: {"code":xxx,...}\n\n` 格式输出。

**Validates: Requirements 2.7, 2.13, 2.14**

---

Property 6: Bug Condition — 渠道级 maxRetries 生效

_For any_ 渠道配置包含 `maxRetries: N`（`0 ≤ N ≤ 10`），修复后 `DynamicModelRouter` 在该渠道发生可重试错误时 SHALL 重试至多 N 次，而非无限降级。_For any_ `maxRetries > 10` 或 `maxRetries < 0`，系统 SHALL 在启动时抛出 `IllegalArgumentException`。

**Validates: Requirements 2.8, 2.15**

---

Property 7: Bug Condition — 路由单一化与 AOP 切面生效

_For any_ 经 `AiChatController` 进入的请求，修复后的调用链 SHALL 经过 Spring AOP 代理，`@LlmProtection` 和 `@SemanticCache` 切面 SHALL 均被触发（可通过日志或 Actuator metrics 验证），所有模型调用 SHALL 统一经由 `DynamicModelRouter.chatBlock()` / `chatStream()`。

**Validates: Requirements 2.9, 2.16, 2.17**

---

## Fix Implementation

### BUG-1：补全 CLAUDE / QWEN / ZHIPU 三个供应商策略

**新增文件：**

`core/strategy/ClaudeProviderStrategy.java`
```java
@Component
public class ClaudeProviderStrategy implements LlmProviderStrategy {

    @Override
    public LlmProvider getProvider() { return LlmProvider.CLAUDE; }

    @Override
    public ChatLanguageModel createChatModel(LlmChannelConfig config) {
        // Claude 通过 Anthropic 的 OpenAI 兼容端点（baseUrl 指向 https://api.anthropic.com/v1）
        validateConfig(config);
        return OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .timeout(Duration.ofMillis(config.getTimeoutMillis()))
                .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatLanguageModel(LlmChannelConfig config) {
        validateConfig(config);
        return OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .timeout(Duration.ofMillis(config.getTimeoutMillis()))
                .build();
    }

    @PostConstruct  // 在 Bean 初始化时校验，而非首次请求时
    // 注意：校验逻辑移到 YamlChannelConfigRepository 的 @PostConstruct 中集中处理
    private void validateConfig(LlmChannelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalArgumentException(
                "渠道 [" + config.getId() + "] CLAUDE 供应商缺少 apiKey 配置");
        }
    }
}
```

`core/strategy/QwenProviderStrategy.java`
```java
@Component
public class QwenProviderStrategy implements LlmProviderStrategy {

    @Override
    public LlmProvider getProvider() { return LlmProvider.QWEN; }

    @Override
    public ChatLanguageModel createChatModel(LlmChannelConfig config) {
        // 通义千问使用 DashScope 兼容的 OpenAI 端点
        // baseUrl: https://dashscope.aliyuncs.com/compatible-mode/v1
        return OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .timeout(Duration.ofMillis(config.getTimeoutMillis()))
                .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatLanguageModel(LlmChannelConfig config) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .timeout(Duration.ofMillis(config.getTimeoutMillis()))
                .build();
    }
}
```

`core/strategy/ZhipuProviderStrategy.java`
```java
@Component
public class ZhipuProviderStrategy implements LlmProviderStrategy {

    @Override
    public LlmProvider getProvider() { return LlmProvider.ZHIPU; }

    @Override
    public ChatLanguageModel createChatModel(LlmChannelConfig config) {
        // 智谱 AI 使用 OpenAI 协议兼容接口
        // baseUrl: https://open.bigmodel.cn/api/paas/v4/
        return OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .timeout(Duration.ofMillis(config.getTimeoutMillis()))
                .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatLanguageModel(LlmChannelConfig config) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .timeout(Duration.ofMillis(config.getTimeoutMillis()))
                .build();
    }
}
```

**修改文件：** `config/YamlChannelConfigRepository.java`

增加 `@PostConstruct` 启动校验：
```java
@PostConstruct
public void validateChannels() {
    for (LlmChannelConfig config : channels) {
        if (Boolean.TRUE.equals(config.getIsEnabled())) {
            if (config.getApiKey() == null || config.getApiKey().isBlank()) {
                throw new IllegalArgumentException(
                    "渠道 [" + config.getId() + "] 供应商 [" + config.getProvider() + 
                    "] 的 apiKey 不能为空");
            }
            if (config.getMaxRetries() != null && 
                (config.getMaxRetries() < 0 || config.getMaxRetries() > 10)) {
                throw new IllegalArgumentException(
                    "渠道 [" + config.getId() + "] 的 maxRetries=" + config.getMaxRetries() + 
                    " 超出合法范围 [0, 10]");
            }
        }
    }
}
```

---

### BUG-2：LlmProvider 枚举补全 displayName 字段

**修改文件：** `core/model/LlmProvider.java`

```java
@Getter
@AllArgsConstructor
public enum LlmProvider {

    OPENAI(
        "OpenAI GPT",
        "OpenAI 官方 GPT 系列模型（gpt-4o 等）"
    ),
    DEEPSEEK(
        "DeepSeek",
        "深度求索 DeepSeek 系列模型（兼容 OpenAI 协议）"
    ),
    CLAUDE(
        "Claude (Anthropic)",
        "Anthropic Claude 系列模型（claude-3-5-sonnet 等）"
    ),
    QWEN(
        "通义千问 (Qwen)",
        "阿里云通义千问系列模型（DashScope SDK）"
    ),
    ZHIPU(
        "智谱 GLM",
        "智谱 AI GLM 系列模型（兼容 OpenAI 协议）"
    ),
    MOCK(
        "Mock",
        "测试用 Mock 模型，返回预设响应，不调用真实 API"
    );

    /** 用户界面展示名称 */
    private final String displayName;

    /** 技术描述，包含协议和版本信息 */
    private final String description;

    public String getCode() {
        return this.name().toLowerCase();
    }
}
```

**迁移说明：** 旧字段 `description` 的一个参数构造器被替换为双参数构造器。所有通过 Lombok `@AllArgsConstructor` 生成的构造器调用均需同步更新（全局搜索确认只有枚举定义本身使用该构造器）。

---

### BUG-3：补全异常体系

**新增文件：** `core/exception/AiException.java`（统一基类，替代 `AiServiceException` 的语义角色）

```java
package com.enterprise.ai.starter.core.exception;

/**
 * AI 服务异常统一基类
 * 所有 AI 业务异常均应继承本类，便于全局异常处理器统一捕获
 */
@Getter
public class AiException extends RuntimeException {

    private final String errorCode;

    public AiException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public AiException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
```

**修改文件：** `core/exception/AiServiceException.java`（继承 `AiException`，保持向后兼容）

```java
public class AiServiceException extends AiException {
    public AiServiceException(String message) {
        super(message, "AI_SERVICE_ERROR");
    }
    public AiServiceException(String message, String errorCode) {
        super(message, errorCode);
    }
    public AiServiceException(String message, Throwable cause) {
        super(message, "AI_SERVICE_ERROR", cause);
    }
    public AiServiceException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
```

**修改文件：** `core/exception/ModelProviderException.java`（继承 `AiException`）

```java
public class ModelProviderException extends AiException {
    public ModelProviderException(String message) {
        super(message, "MODEL_PROVIDER_ERROR");
    }
    public ModelProviderException(String message, Throwable cause) {
        super(message, "MODEL_PROVIDER_ERROR", cause);
    }
}
```

**修改文件：** `core/exception/NoAvailableChannelException.java`（继承 `AiException`）

```java
public class NoAvailableChannelException extends AiException {
    public NoAvailableChannelException(String message) {
        super(message, "NO_AVAILABLE_CHANNEL");
    }
}
```

**新增文件：** `core/exception/ModelUnavailableException.java`

```java
/**
 * 单一供应商不可用异常
 * 携带供应商名称与原始错误，支持异常链追溯
 */
@Getter
public class ModelUnavailableException extends AiException {

    private final LlmProvider provider;

    public ModelUnavailableException(LlmProvider provider, String reason) {
        super("供应商 [" + provider.getDisplayName() + "] 不可用: " + reason, "MODEL_UNAVAILABLE");
        this.provider = provider;
    }

    public ModelUnavailableException(LlmProvider provider, String reason, Throwable cause) {
        super("供应商 [" + provider.getDisplayName() + "] 不可用: " + reason, "MODEL_UNAVAILABLE", cause);
        this.provider = provider;
    }
}
```

**新增文件：** `core/exception/AllModelsFailedException.java`

```java
/**
 * 全部模型均不可用异常
 * 当所有已启用渠道均调用失败或熔断时抛出
 */
@Getter
public class AllModelsFailedException extends AiException {

    private final int attemptedChannels;

    public AllModelsFailedException(int attemptedChannels) {
        super("所有 AI 渠道（共 " + attemptedChannels + " 个）均已不可用，请稍后重试或检查渠道配置",
              "ALL_MODELS_FAILED");
        this.attemptedChannels = attemptedChannels;
    }
}
```

**修改文件：** `core/router/DynamicModelRouter.java`

在 `selectChannel()` 中将 `NoAvailableChannelException` 替换为 `AllModelsFailedException`（当 `excludedIds` 非空时，说明已有渠道尝试过）：

```java
private LlmChannelConfig selectChannel(String channelId, Set<String> excludedIds) {
    // ... 原有选择逻辑 ...
    if (availableChannels.isEmpty()) {
        if (!excludedIds.isEmpty()) {
            // 已经尝试过部分渠道，全部失败
            throw new AllModelsFailedException(excludedIds.size());
        }
        // 一开始就没有可用渠道
        throw new NoAvailableChannelException("系统中没有启用的 AI 渠道，请检查配置");
    }
    // ...
}
```

---

### BUG-4：统一响应包装

**新增文件：** `web/dto/ApiResponse.java`（新的统一响应类）

```java
package com.enterprise.ai.starter.web.dto;

import lombok.*;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * 统一 API 响应包装类
 * code=0 表示成功，其他值表示业务错误
 * traceId 统一从 MDC("traceId") 获取
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /** 业务状态码：0 成功，非 0 失败 */
    private Integer code;

    /** 消息描述 */
    private String message;

    /** 响应业务数据 */
    private T data;

    /** 请求追踪 ID，统一来源：MDC > X-Request-Id Header > UUID */
    private String traceId;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(0)
                .message("success")
                .data(data)
                .traceId(resolveTraceId())
                .build();
    }

    public static <T> ApiResponse<T> error(Integer code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .traceId(resolveTraceId())
                .build();
    }

    public static <T> ApiResponse<T> error(Integer code, String message, String traceId) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .traceId(traceId)
                .build();
    }

    /** traceId 解析优先级：MDC > 传入值 > UUID */
    public static String resolveTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) return traceId;
        return UUID.randomUUID().toString();
    }
}
```

**修改文件：** `web/dto/ChatApiResponse.java`（标记为废弃，委托给 `ApiResponse`）

```java
/**
 * @deprecated 使用 {@link ApiResponse} 替代
 */
@Deprecated(since = "1.1.0", forRemoval = true)
public class ChatApiResponse<T> extends ApiResponse<T> {
    // 保留空类体以防止现有代码编译失败
    // 所有工厂方法通过继承自动可用
}
```

> **注意：** 如果 `ChatApiResponse` 字段与 `ApiResponse` 不兼容（例如旧的 `code=200`），则不做继承，直接标记 `@Deprecated` 并在控制器层全量替换为 `ApiResponse`。

**修改文件：** `web/exception/GlobalReactiveExceptionHandler.java`

```java
@Override
public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    log.error("系统发生异常: ", ex);
    ServerHttpResponse response = exchange.getResponse();

    if (response.isCommitted()) {
        return Mono.error(ex);
    }

    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    HttpStatus status;
    Integer businessCode;
    String message;

    if (ex instanceof AllModelsFailedException e) {
        status = HttpStatus.SERVICE_UNAVAILABLE;
        businessCode = 503;
        message = e.getMessage();
    } else if (ex instanceof NoAvailableChannelException) {
        status = HttpStatus.SERVICE_UNAVAILABLE;
        businessCode = 503;
        message = "当前无可用的 AI 渠道";
    } else if (ex instanceof ModelUnavailableException e) {
        status = HttpStatus.BAD_GATEWAY;
        businessCode = 502;
        message = "AI 供应商服务不可用: " + e.getMessage();
    } else if (ex instanceof ModelProviderException) {
        status = HttpStatus.BAD_GATEWAY;
        businessCode = 502;
        message = "AI 供应商服务不可用: " + ex.getMessage();
    } else {
        status = HttpStatus.INTERNAL_SERVER_ERROR;
        businessCode = 500;
        message = "服务器内部故障，请联系系统管理员";
    }

    response.setStatusCode(status);
    ApiResponse<Void> apiResponse = ApiResponse.error(businessCode, message);
    // traceId 已在 ApiResponse.error() 中通过 MDC 自动填充

    try {
        byte[] bytes = objectMapper.writeValueAsBytes(apiResponse);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    } catch (JsonProcessingException e) {
        return Mono.error(e);
    }
}
```

**修改文件：** `core/exception/GlobalExceptionHandler.java`

```java
@ExceptionHandler(AiException.class)  // 捕获所有 AiException 子类
public ResponseEntity<ApiResponse<Void>> handleAiException(AiException e) {
    log.error("AI 业务异常 [{}]: {}", e.getErrorCode(), e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(500, e.getMessage()));
}

@ExceptionHandler(Exception.class)
public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
    log.error("系统未知异常", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(500, "服务器内部错误，请稍后重试"));
}
```

**修改文件：** `web/controller/AiChatController.java`

SSE 错误事件格式修正，符合 `event: error\ndata: {...}\n\n` 规范：

```java
// 原来：
.onErrorResume(e -> Flux.just(toSseEvent(SseMessage.error(e.getMessage()))));

// 修改为：
.onErrorResume(e -> {
    log.error("流式对话执行异常", e);
    // 使用 SSE event 类型区分错误事件
    String errorData = toErrorJson(e);
    return Flux.just(ServerSentEvent.<String>builder()
            .event("error")  // event: error
            .data(errorData)  // data: {"code":500,"message":"...","traceId":"..."}
            .build());
});

private String toErrorJson(Throwable e) {
    try {
        return objectMapper.writeValueAsString(
            Map.of("code", 500, "message", e.getMessage(), 
                   "traceId", ApiResponse.resolveTraceId()));
    } catch (JsonProcessingException ex) {
        return "{\"code\":500,\"message\":\"序列化失败\"}";
    }
}
```

同时将非流式响应的 `ChatApiResponse.success()` 替换为 `ApiResponse.success()`。

---

### BUG-5：LlmChannelConfig 补全 maxRetries 字段

**修改文件：** `core/model/LlmChannelConfig.java`

```java
/**
 * 渠道级最大重试次数（默认 3，范围 0~10）
 * 优先于全局重试配置；为 null 时使用全局配置
 */
@Builder.Default
private Integer maxRetries = 3;
```

**修改文件：** `core/router/DynamicModelRouter.java`

在 `chatBlockWithRetry` 和 `chatStreamWithRetry` 中引入渠道级重试计数器：

```java
// 在 onErrorResume 中检查当前渠道的重试次数
.onErrorResume(e -> {
    int channelRetries = retryCount.getOrDefault(config.getId(), 0);
    int maxRetries = config.getMaxRetries() != null ? config.getMaxRetries() : 3;

    if (channelRetries < maxRetries && isRetryable(e)) {
        log.warn("渠道 [{}] 第 {}/{} 次重试, 错误: {}", 
                config.getName(), channelRetries + 1, maxRetries, e.getMessage());
        retryCount.put(config.getId(), channelRetries + 1);
        return chatBlockWithRetry(channelId, request, excludedIds, retryCount);
    }

    log.warn("渠道 [{}] 达到最大重试次数 {} 或不可重试, 降级到其他渠道", 
            config.getName(), maxRetries);
    excludedIds.add(config.getId());
    return chatBlockWithRetry(null, request, excludedIds, new HashMap<>());
});

// isRetryable 判断是否值得重试（网络超时、HTTP 5xx）
private boolean isRetryable(Throwable e) {
    return e instanceof java.net.SocketTimeoutException
        || e instanceof java.util.concurrent.TimeoutException
        || e instanceof CallNotPermittedException == false;  // 熔断直接降级不重试
}
```

---

### BUG-6：消除双重路由，使 AOP 切面真正生效

**核心分析：**

`SemanticCacheAspect` 已经正确地切到 `LlmRouterService` 接口方法（`execution(* com.enterprise.ai.starter.core.router.LlmRouterService.chatBlock(..))`），因此缓存切面本身没有问题。问题在于 `DynamicChatServiceImpl.chat()` 根本没有调用 `LlmRouterService.chatBlock()`，而是走旧路径，所以切面从未触发。

`LlmProtectionAspect` 的切点是 `@annotation(llmProtection)`，即标注了 `@LlmProtection` 的方法。当前标注在 `callWithProtection()` 上，但该方法通过 `this.callWithProtection()` 调用（同类方法调用不走代理）。修复方案是将 `@LlmProtection` 切点移到 Spring Bean 边界，即标注在 `LlmRouterService` 的实现方法上，或引入独立的保护 Service Bean。

**修改文件：** `core/service/DynamicChatServiceImpl.java`

重构为横切关注点协调者，委托 `LlmRouterService`（通过 Spring 注入，走代理）：

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicChatServiceImpl implements ChatService {

    // 注入 LlmRouterService 的 Spring 代理 Bean（而非 this）
    // 语义缓存切面已切到 LlmRouterService 接口，注入后自动生效
    private final LlmRouterService routerService;
    private final TokenCalculator tokenCalculator;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String chat(String message) {
        // 构建 ChatRequest 并委托给 LlmRouterService（经过 Spring AOP 代理）
        ChatRequest request = ChatRequest.builder().message(message).build();
        ChatResponse response = routerService.chatBlock(null, request).block();
        return response != null ? response.getContent() : "";
    }

    @Override
    public Flux<String> streamChat(String message) {
        ChatRequest request = ChatRequest.builder().message(message).build();
        return routerService.chatStream(null, request);
    }
}
```

**修改文件：** `core/router/DynamicModelRouter.java`

在 `chatBlock` / `chatStream` 方法上添加 `@LlmProtection`（注意：此处通过接口代理调用，AOP 会生效）：

```java
@Override
@LlmProtection(qps = 5, capacity = 10)
public Mono<ChatResponse> chatBlock(String channelId, ChatRequest request) {
    return chatBlockWithRetry(channelId, request, new HashSet<>(), new HashMap<>());
}

@Override
@LlmProtection(qps = 5, capacity = 10)
public Flux<String> chatStream(String channelId, ChatRequest request) {
    return chatStreamWithRetry(channelId, request, new HashSet<>(), new HashMap<>());
}
```

**修改文件：** `core/router/DynamicModelRouter.java`（保留旧 `route()` 方法，标记废弃）

```java
/**
 * @deprecated 请使用 {@link #chatBlock(String, ChatRequest)} 或 {@link #chatStream(String, ChatRequest)}
 *             此方法仅返回 LlmProvider 枚举，不具备完整的路由、熔断、重试能力
 */
@Deprecated(since = "1.1.0", forRemoval = false)
public LlmProvider route() {
    return channelRepository.findAllEnabled().stream()
            .findFirst()
            .map(LlmChannelConfig::getProvider)
            .orElse(LlmProvider.MOCK);
}
```

**修改文件：** `core/aspect/LlmProtectionAspect.java`

调整切点以匹配 `@LlmProtection` 注解位于 `DynamicModelRouter` 接口实现方法上的场景；移除对 `DynamicModelRouter.route()` 和 `ChatModelFactory` 的直接依赖（Aspect 本身不应直接调用路由方法）：

```java
// 保持 @Around("@annotation(llmProtection)") 不变
// 移除 DynamicModelRouter 和 ChatModelFactory 的注入依赖（已不再需要）
// 降级逻辑修改为：抛出 AiServiceException 触发上层错误处理，而非自行路由
private Object doFallbackSync(ProceedingJoinPoint joinPoint) {
    throw new AiServiceException("限流或熔断触发，请求被拒绝", "RATE_LIMIT_EXCEEDED");
}
```

---

## Testing Strategy

### Validation Approach

测试分三个阶段：
1. **Exploratory（探索性）**：在未修复代码上运行，确认缺陷复现
2. **Fix Checking**：修复后对 `C(X)` 输入验证 `P(result)` 成立
3. **Preservation Checking**：对 `¬C(X)` 输入验证 `F(X) == F'(X)`

### Exploratory Bug Condition Checking

**目标**：在修复前运行，确认并记录各缺陷的失败模式。

**测试计划：**

1. **策略缺失探索**：构造 `provider=CLAUDE` 的 `LlmChannelConfig`，调用 `LlmClientFactory.getChatModel(config)`，断言抛出 `ModelProviderException`（将在修复后变为正常返回）
2. **枚举字段探索**：反射获取 `LlmProvider.QWEN.getDisplayName()`，预期 `NoSuchMethodException`（修复前枚举无此方法）
3. **异常类型探索**：模拟所有渠道熔断，触发 `selectChannel()`，断言抛出 `NoAvailableChannelException`（修复后应为 `AllModelsFailedException`）
4. **AOP 不生效探索**：注入 `DynamicChatServiceImpl`，调用 `chat()`，通过 `SpyBean` 或日志验证 `SemanticCacheAspect` 未被触发

**预期反例：**
- `LlmClientFactory` 对 CLAUDE/QWEN/ZHIPU 抛出 `ModelProviderException`
- `LlmProvider` 无 `displayName` 方法
- 全渠道失败时抛出 `NoAvailableChannelException` 而非 `AllModelsFailedException`
- `SemanticCacheAspect` 日志中无缓存检查记录

### Fix Checking

**目标**：`∀ input where isBugCondition(input) == true`，验证修复后 `P(result)` 成立。

```
FOR ALL config WHERE config.provider IN [CLAUDE, QWEN, ZHIPU] DO
  result := LlmClientFactory_fixed.getChatModel(config)
  ASSERT result != null
  ASSERT result instanceof ChatLanguageModel
END FOR

FOR ALL provider IN LlmProvider.values() DO
  ASSERT provider.getDisplayName() != null
  ASSERT provider.getDisplayName() != provider.getDescription()
END FOR

FOR ALL scenario WHERE allChannelsExhausted(scenario) DO
  exception := DynamicModelRouter_fixed.chatBlock(null, request)
  ASSERT exception instanceof AllModelsFailedException
END FOR

FOR ALL response WHERE isNonSseResponse(response) DO
  ASSERT response.code == 0 OR response.code > 0 (business error codes)
  ASSERT response.traceId != null
  ASSERT response.class == ApiResponse
END FOR
```

### Preservation Checking

**目标**：`∀ input where isBugCondition(input) == false`，验证 `F(input) == F'(input)`。

```
FOR ALL config WHERE config.provider IN [OPENAI, DEEPSEEK, MOCK] DO
  result_original := LlmClientFactory_original.getChatModel(config)
  result_fixed   := LlmClientFactory_fixed.getChatModel(config)
  ASSERT result_original.class == result_fixed.class
END FOR

FOR ALL request WHERE provider IN [OPENAI, DEEPSEEK, MOCK] DO
  response_original := DynamicModelRouter_original.chatBlock(null, request).block()
  response_fixed    := DynamicModelRouter_fixed.chatBlock(null, request).block()
  ASSERT response_original.content == response_fixed.content
END FOR
```

**推荐使用属性测试（PBT）进行 Preservation Checking**，因为它能自动生成大量 `config` 和 `request` 组合，覆盖边界情况。

### Unit Tests

- `LlmClientFactoryTest`：验证 6 个 provider 均能创建 `ChatLanguageModel`，`UNSUPPORTED` provider 抛出 `ModelUnavailableException`
- `LlmProviderTest`：验证所有枚举常量的 `displayName` 非空、与 `description` 不同，Jackson 序列化包含三个字段
- `AiExceptionHierarchyTest`：验证 `AllModelsFailedException instanceof AiException`、`ModelUnavailableException instanceof AiException`、`NoAvailableChannelException instanceof AiException`
- `ApiResponseTest`：验证 `ApiResponse.success(data).code == 0`，`resolveTraceId()` 从 MDC 正确获取
- `DynamicModelRouterRetryTest`：验证 `maxRetries=2` 渠道只重试 2 次，第 3 次失败时降级
- `DynamicModelRouterExhaustTest`：验证所有渠道失败时抛出 `AllModelsFailedException`
- `DynamicChatServiceImplAopTest`：注入 `DynamicChatServiceImpl`，调用 `chat()`，验证 `SemanticCacheAspect` 被触发（使用 `@SpyBean` 或 Mockito 验证方法调用）
- `GlobalReactiveExceptionHandlerTest`：验证 `AllModelsFailedException` → 503，`ModelUnavailableException` → 502，响应体为 `ApiResponse` 格式

### Property-Based Tests

使用 jqwik 或等效 PBT 框架：

- **P1（策略注册完整性）**：随机生成 `LlmChannelConfig`，`provider ∈ ALL_VALUES`，除 UNSUPPORTED 外均应成功创建客户端
- **P2（响应格式一致性）**：随机生成请求内容（任意字符串），`ApiResponse.success(data).code` 恒为 0，`traceId` 恒不为 null
- **P3（Preservation 属性）**：对 `provider ∈ {OPENAI, DEEPSEEK, MOCK}` 的随机配置，修复前后工厂返回相同实现类
- **P4（maxRetries 边界）**：随机生成 `maxRetries ∈ [0,10]`，重试逻辑应在精确次数后降级；`maxRetries > 10` 应在启动时失败
- **P5（枚举字段唯一性）**：对所有 `LlmProvider` 枚举值，`displayName != description` 恒成立

### Integration Tests

- **端到端 MOCK 渠道测试**：启动完整 Spring Boot 上下文，发起 `POST /api/v1/ai/chat`（`provider=MOCK`），验证响应格式为 `ApiResponse`，`code=0`，`traceId` 非空
- **SSE 流式错误格式测试**：模拟 MOCK 渠道在流中途抛出异常，验证 SSE 事件包含 `event: error` 字段
- **AOP 切面集成测试**：通过 Spring Test 注入 `DynamicChatServiceImpl`（完整代理），调用 `chat()` 后验证 MDC、缓存切面、保护切面均被触发（日志验证）
- **渠道降级集成测试**：配置两个渠道，第一个 MOCK 配置为立即熔断，验证请求自动降级到第二个渠道并成功响应

---

## 文件变更汇总

| 操作 | 文件路径 | 说明 |
|------|----------|------|
| 新增 | `core/strategy/ClaudeProviderStrategy.java` | BUG-1：Claude 策略实现 |
| 新增 | `core/strategy/QwenProviderStrategy.java` | BUG-1：Qwen 策略实现 |
| 新增 | `core/strategy/ZhipuProviderStrategy.java` | BUG-1：Zhipu 策略实现 |
| 修改 | `core/model/LlmProvider.java` | BUG-2：增加 displayName 字段 |
| 新增 | `core/exception/AiException.java` | BUG-3：统一异常基类 |
| 修改 | `core/exception/AiServiceException.java` | BUG-3：继承 AiException |
| 修改 | `core/exception/ModelProviderException.java` | BUG-3：继承 AiException |
| 修改 | `core/exception/NoAvailableChannelException.java` | BUG-3：继承 AiException |
| 新增 | `core/exception/ModelUnavailableException.java` | BUG-3：单一供应商不可用 |
| 新增 | `core/exception/AllModelsFailedException.java` | BUG-3：全部模型失败 |
| 新增 | `web/dto/ApiResponse.java` | BUG-4：统一响应包装类 |
| 修改 | `web/dto/ChatApiResponse.java` | BUG-4：标记 @Deprecated |
| 修改 | `web/exception/GlobalReactiveExceptionHandler.java` | BUG-3+4：处理新异常，使用 ApiResponse |
| 修改 | `core/exception/GlobalExceptionHandler.java` | BUG-4：使用 ApiResponse |
| 修改 | `web/controller/AiChatController.java` | BUG-4：SSE 错误格式，使用 ApiResponse |
| 修改 | `core/model/LlmChannelConfig.java` | BUG-5：增加 maxRetries 字段 |
| 修改 | `config/YamlChannelConfigRepository.java` | BUG-1+5：启动校验逻辑 |
| 修改 | `core/router/DynamicModelRouter.java` | BUG-3+5+6：异常改造，重试逻辑，deprecate route() |
| 修改 | `core/service/DynamicChatServiceImpl.java` | BUG-6：委托给 LlmRouterService |
| 修改 | `core/aspect/LlmProtectionAspect.java` | BUG-6：移除直接路由依赖 |
