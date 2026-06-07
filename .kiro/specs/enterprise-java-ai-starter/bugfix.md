# Bugfix Requirements Document

## Introduction

Enterprise Java-AI Starter 是一套基于 Spring Boot 3.3 + Java 21 + LangChain4j + WebFlux + Resilience4j 的企业级 AI 转发网关脚手架。当前项目虽已存在部分基础骨架（`LlmProvider` 枚举、`ChatService` 接口、`DynamicModelRouter`、`AiChatController` 等），但核心的多模型抽象层与动态路由模块存在若干结构性缺失与实现缺口，导致系统在运行时无法通过统一接口正确调用全部五种大模型供应商（CLAUDE、QWEN、ZHIPU 缺少策略实现），无法保证异常体系的完整性与一致性，也无法确保统一响应包装的规范对齐。这些缺失直接影响了系统的可用性、可扩展性和企业级高可用保障目标。

---

## Bug Analysis

### Current Behavior (Defect)

**1. 多模型供应商策略缺失**

1.1 WHEN 系统启动并注册所有 `LlmProviderStrategy` Bean 时，THEN 仅有 `OPENAI`、`DEEPSEEK`、`MOCK` 三个策略实现被注册到 `LlmClientFactory`，`CLAUDE`、`QWEN`、`ZHIPU` 三个枚举值没有对应的策略实现类，导致工厂抛出 `ModelProviderException("不支持的模型供应商: CLAUDE/QWEN/ZHIPU")`

1.2 WHEN 调用方通过渠道配置将 `provider` 设置为 `CLAUDE`、`QWEN` 或 `ZHIPU` 并发起请求时，THEN `LlmClientFactory.getChatModel()` 因找不到对应策略而抛出异常，请求直接失败，无法降级到其他可用渠道

1.10 WHEN `provider` 为 `CLAUDE`/`QWEN`/`ZHIPU` 的渠道配置中 `apiKey` 或 `baseUrl` 为空或非法时，THEN 系统 SHALL 在启动校验阶段（`@PostConstruct`）抛出 `IllegalArgumentException` 并打印明确的配置错误信息，而非在首次请求时才抛出运行时异常

**2. 枚举字段不完整**

1.3 WHEN 上层调用方或前端需要展示模型的 `displayName`（用户友好的展示名）时，THEN `LlmProvider` 枚举仅有 `description` 字段，没有独立的 `displayName` 字段，无法满足接口语义上对"展示名称"的明确区分

1.11 WHEN 提供者列表接口序列化 `LlmProvider` 枚举为 JSON 时，THEN 序列化结果中 `displayName` 与 `description` 字段值相同（因为两者均来自同一 `description` 字段），前端无法区分"用于界面展示的友好名称"和"技术描述文本"

**3. 异常体系不规范**

1.4 WHEN 所有模型渠道均不可用或均已熔断时，THEN 系统缺少 `AllModelsFailedException` 专用异常类，`DynamicModelRouter` 只能抛出语义模糊的 `NoAvailableChannelException`，全局异常处理器无法对"全部模型失败"场景进行差异化的 HTTP 状态码映射和错误响应

1.5 WHEN 某个单一模型供应商发生调用失败（超时、鉴权错误、限流等）时，THEN 系统缺少 `ModelUnavailableException` 专用异常类，导致供应商级别的失败被笼统地包装为 `RuntimeException`，丧失了错误溯源能力

1.6 WHEN 系统需要一个统一的业务异常基类来聚合所有 AI 服务异常时，THEN 现有的 `AiServiceException` 类名与用户需求约定的 `AiException` 不一致，且 `ModelProviderException`、`NoAvailableChannelException` 均未继承自统一基类，异常体系呈现碎片化状态

**4. 统一响应包装不一致**

1.7 WHEN 控制器返回成功或错误响应时，THEN 使用的是 `ChatApiResponse<T>` 包装类，与需求约定的泛型统一响应类 `ApiResponse<T>` 命名不一致，全局异常处理器与业务控制器之间的响应结构存在差异，客户端无法依赖一致的响应格式进行解析

1.12 WHEN SSE 流式响应（`Content-Type: text/event-stream`）发生异常时，THEN 错误信息被包装为 `ApiResponse` JSON 对象附加在流末尾，客户端的 SSE 解析器因收到非 `data:` 格式内容而无法正确处理错误

1.13 WHEN 追踪 ID（`traceId`）填充到 `ApiResponse` 时，THEN `traceId` 来源不一致（部分来自 `ThreadLocal`，部分来自 MDC），导致同一请求链路在不同响应中携带不同的追踪标识

**5. 渠道配置模型缺失字段**

1.8 WHEN 渠道配置需要定义单渠道级别的最大重试次数时，THEN `LlmChannelConfig` 中缺少 `maxRetries` 字段，`DynamicModelRouter` 的重试逻辑只能使用全局策略，无法实现渠道粒度的精细化重试控制

**6. 双重路由职责混乱**

1.9 WHEN 外部请求经过 `AiChatController` → `DynamicChatServiceImpl` → `DynamicModelRouter` 的调用链时，THEN `DynamicChatServiceImpl` 通过旧版 `DynamicModelRouter.route()` 方法自行选择 Provider 并调用 `ChatModelFactory`，而 `DynamicModelRouter.chatBlock/chatStream` 又独立维护一套基于 `LlmClientFactory` 的渠道选择逻辑，两套路由逻辑并存，相互冲突，导致限流切面（`@LlmProtection`）和语义缓存切面（`@SemanticCache`）在实际请求路径中不生效，且渠道配置（`LlmChannelConfig`）和旧版 `LlmProvider` 路由无法联动

---

### Expected Behavior (Correct)

**1. 全供应商策略完整注册**

2.1 WHEN 渠道配置中 `provider` 为 `CLAUDE`、`QWEN` 或 `ZHIPU` 且配置合法（`apiKey` 非空、`baseUrl` 格式合法）时，THEN 系统 SHALL 成功处理请求并返回该供应商的响应，HTTP 状态码为 200，响应体中 `code` 字段为 `0`（成功）

2.10 WHEN 渠道配置中 `provider` 为 `CLAUDE`、`QWEN` 或 `ZHIPU` 且 `apiKey` 为空时，THEN 系统 SHALL 在启动时抛出配置校验异常，日志中 SHALL 包含明确的供应商名称和缺失的配置键名

2.11 WHEN 系统注册 `LlmProviderStrategy` 时，THEN 新增 `ClaudeProviderStrategy`（使用 Anthropic OpenAI 兼容接口）、`QwenProviderStrategy`（使用 DashScope SDK）、`ZhipuProviderStrategy`（使用 OpenAI 协议兼容接口）三个实现类，每个实现类 SHALL 实现 `LlmProviderStrategy` 接口的 `supports(LlmProvider)` 和 `buildChatModel(LlmChannelConfig)` 方法

2.2 WHEN 渠道配置中 `provider` 为 `CLAUDE`、`QWEN` 或 `ZHIPU` 时，THEN 系统 SHALL 正确创建对应供应商的 `ChatLanguageModel` 和 `StreamingChatLanguageModel`，其中 Claude 使用 Anthropic 协议兼容实现（通过 OpenAI 兼容接口或专用 builder），Qwen 使用 DashScope SDK，Zhipu 使用 OpenAI 协议兼容接口

**2. 枚举字段补全**

2.3 WHEN `LlmProvider` 枚举被调用或序列化时，THEN 每个枚举常量 SHALL 同时具有 `displayName`（用户友好名称）和 `description`（技术描述）两个**独立且值不相同**的字段，具体值如下：
- `OPENAI`：`displayName = "OpenAI GPT"`, `description = "OpenAI 官方 GPT 系列模型（gpt-4o 等）"`
- `DEEPSEEK`：`displayName = "DeepSeek"`, `description = "深度求索 DeepSeek 系列模型（兼容 OpenAI 协议）"`
- `CLAUDE`：`displayName = "Claude (Anthropic)"`, `description = "Anthropic Claude 系列模型（claude-3-5-sonnet 等）"`
- `QWEN`：`displayName = "通义千问 (Qwen)"`, `description = "阿里云通义千问系列模型（DashScope SDK）"`
- `ZHIPU`：`displayName = "智谱 GLM"`, `description = "智谱 AI GLM 系列模型（兼容 OpenAI 协议）"`

2.12 WHEN `LlmProvider` 枚举通过 Jackson 序列化为 JSON 时，THEN 序列化结果 SHALL 包含 `name`、`displayName`、`description` 三个字段，其中 `displayName` 与 `description` 的值 SHALL 不相同

**3. 完整规范的异常体系**

2.4 WHEN 所有已启用渠道均调用失败或熔断时，THEN 系统 SHALL 抛出 `AllModelsFailedException`（继承自 `AiException`），全局异常处理器 SHALL 将其映射为 HTTP 503 状态码并返回语义明确的错误信息

2.5 WHEN 某个供应商调用发生错误时，THEN 系统 SHALL 抛出 `ModelUnavailableException`（继承自 `AiException`），携带供应商名称、错误原因等上下文信息，支持异常链追溯

2.6 WHEN 整个 AI 异常体系需要统一基类时，THEN `AiException` SHALL 作为所有 AI 业务异常的父类，`ModelUnavailableException`、`AllModelsFailedException` 以及现有的 `NoAvailableChannelException` 均 SHALL 继承自 `AiException`

**4. 统一响应包装规范化**

2.7 WHEN 任意控制器或全局异常处理器返回**非 SSE**响应时，THEN 所有响应 SHALL 使用统一的 `ApiResponse<T>` 泛型包装类，包含 `code`（业务状态码，成功为 `0`）、`message`（消息）、`data`（业务数据）、`traceId`（追踪 ID，统一从 MDC 的 `traceId` 键获取）四个标准字段，`GlobalExceptionHandler` 和 `GlobalReactiveExceptionHandler` 均 SHALL 使用 `ApiResponse.error(code, message, traceId)` 工厂方法构建错误响应

2.13 WHEN SSE 流式响应（`Content-Type: text/event-stream`）发生错误时，THEN 错误事件 SHALL 以 `event: error\ndata: {"code":xxx,"message":"...","traceId":"..."}\n\n` 格式推送，而非将 `ApiResponse` 包装类嵌入 SSE 数据流

2.14 WHEN `ApiResponse` 中的 `traceId` 字段被填充时，THEN `traceId` SHALL 统一从 MDC（`org.slf4j.MDC.get("traceId")`）获取，若 MDC 中不存在则使用请求头 `X-Request-Id` 的值，若均不存在则生成 UUID；所有代码路径（同步响应、SSE 响应、异常响应）SHALL 使用同一个工具方法填充 `traceId`

**5. 渠道配置补全**

2.8 WHEN 渠道配置文件（YAML）中设置 `maxRetries` 字段时，THEN `LlmChannelConfig` SHALL 包含 `maxRetries` 字段（默认值为 `3`，合法范围 `0~10`），`DynamicModelRouter` 在发生可重试错误（网络超时、HTTP 5xx）时 SHALL 优先遵循渠道级别的 `maxRetries` 配置进行重试，全局重试配置仅在渠道未设置 `maxRetries` 时作为兜底

2.15 WHEN `maxRetries` 配置值超出合法范围（`< 0` 或 `> 10`）时，THEN 系统 SHALL 在启动时抛出 `IllegalArgumentException`，日志中 SHALL 包含渠道 ID 和非法的配置值

**6. 路由职责单一化**

2.9 WHEN 外部请求进入系统时，THEN 所有模型调用 SHALL 统一通过 `DynamicModelRouter` 的 `chatBlock(ChatRequest)` / `chatStream(ChatRequest)` 方法执行，`DynamicChatServiceImpl` 的职责 SHALL 收敛为横切关注点协调者（仅负责触发 `@LlmProtection` 限流切面和 `@SemanticCache` 缓存切面），并委托 `LlmRouterService` 作为 AOP 代理边界执行实际的模型调用，消除双重路由冲突

2.16 WHEN `DynamicChatServiceImpl` 通过 `LlmRouterService` 委托模型调用时，THEN `@LlmProtection` 和 `@SemanticCache` 切面 SHALL 实际生效（可通过 Actuator metrics 或日志验证切面被触发），不再因调用链绕过 Spring AOP 代理而失效

2.17 WHEN `DynamicChatServiceImpl` 被重构后，THEN 旧版 `DynamicModelRouter.route(LlmProvider)` 方法 SHALL 被标记为 `@Deprecated` 并保留（不删除），以确保现有未迁移的调用方不因编译失败而中断

---

### Unchanged Behavior (Regression Prevention)

3.1 WHEN `provider` 为 `OPENAI` 且渠道配置有效时，THEN 系统 SHALL CONTINUE TO 通过 `OpenAiProviderStrategy` 正确创建模型客户端并完成同步/流式调用

3.2 WHEN `provider` 为 `DEEPSEEK` 且渠道配置有效时，THEN 系统 SHALL CONTINUE TO 通过 `DeepSeekProviderStrategy` 使用 OpenAI 协议兼容接口完成同步/流式调用

3.3 WHEN `provider` 为 `MOCK` 时，THEN 系统 SHALL CONTINUE TO 通过 `MockProviderStrategy` 返回预设的模拟响应，不调用任何真实 API

3.4 WHEN 请求体中 `stream = false` 时，THEN `AiChatController` SHALL CONTINUE TO 返回 `application/json` 格式的同步响应，包含完整的 AI 回答内容

3.5 WHEN 请求体中 `stream = true`（或默认值）时，THEN `AiChatController` SHALL CONTINUE TO 返回 `text/event-stream` 格式的 SSE 流式响应，每个 token 作为独立事件推送

3.6 WHEN 某个渠道发生调用失败或熔断时，THEN `DynamicModelRouter` SHALL CONTINUE TO 自动排除该渠道并降级选择其他可用渠道进行重试

3.7 WHEN 渠道配置在 `application.yml` 的 `app.ai.channels` 节点下定义时，THEN `YamlChannelConfigRepository` SHALL CONTINUE TO 正确加载配置并通过 `findAllEnabled()` 返回所有启用渠道

3.8 WHEN 渠道调用失败率超过阈值（50%）时，THEN `CircuitBreakerManager` 为该渠道创建的 Resilience4j 熔断器 SHALL CONTINUE TO 按配置的策略（滑动窗口10次、等待10秒进入半开）执行状态转换

3.9 WHEN Token 消耗事件发生时，THEN `TokenBillingListener` 和 `TokenConsumedEvent` 机制 SHALL CONTINUE TO 正常工作，不因路由层重构而中断

3.10 WHEN `app.yml` 中渠道按 `weight` 和 `priority` 配置时，THEN `DynamicModelRouter` 的加权随机选择与优先级排序逻辑 SHALL CONTINUE TO 按现有算法运行（优先级数值越小越优先，同优先级内按权重随机）
