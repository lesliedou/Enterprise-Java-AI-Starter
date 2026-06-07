# Enterprise Java-AI Starter 🚀

[![Spring Boot Version](https://img.shields.io/badge/Spring%20Boot-3.3+-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java Version](https://img.shields.io/badge/Java-17%20%2F%2021-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-Dual--License-orange.svg)]()

**面向企业级商业系统的 Java AI 智能化改造基础设施与转发网关。**

---

## 📌 为什么需要本项目？

当前大模型落地 B 端企业系统（如电商、ERP、CRM）时，传统 Java 团队往往面临以下四大痛点：

1. **公网 API 极不稳定：** 频繁遭遇 `429 Too Many Requests` 限流或 `50x` 服务崩溃，严重影响 B 端业务稳定性。
2. **Token 成本高昂：** 重复多发的相似问题不断消耗公网 Token，缺乏生产级的语义缓存机制。
3. **生态严重割裂：** AI 实验多基于 Python，而企业核心商业资产、高并发架构皆由 Java 承载，强行拼凑导致运维成本飙升。
4. **计费与安全空缺：** 缺乏离线精准 Token 审计、多租户流控、商业合规安全隔离（Guardrails）机制。

`Enterprise Java-AI Starter` 基于 **Java 原生生态（LangChain4j）** 与 **全链路响应式架构（WebFlux）** 打造，作为一套**非侵入式**的 AI 中台/网关组件，帮助 Java 团队在不重构核心业务的前提下，秒级赋能高可用 AI 能力。

---

## ✨ 核心特性

* ⚖️ **多模型动态路由 (Model Router)：** 统一封装 OpenAI、DeepSeek、Anthropic 及国内主流模型 API。支持按【优先级】与【加权轮询】动态分发请求。
* 🛡️ **大厂级熔断降级 (Failover)：** 集成 Resilience4j 熔断器。当主力渠道（如 DeepSeek）限流或宕机时，**毫秒级无缝自动切换**至备用渠道，前端流式对话（SSE）完全无感知。
* 💰 **分布式语义缓存 (Semantic Cache)：** 基于向量相似度匹配（$\ge 0.93$），相似提问直接命中 Redis 缓存返回，**最高帮企业节省 90% 的 Token 成本**，响应时间缩短至 50ms 内。
* 📊 **离线精准 Token 计费：** 引入 `jtokkit` 离线分词器。在流式响应结束时异步精准计算 Prompt/Completion Token，发布 Spring 响应式解耦事件扣减钱包额度。
* ⚡ **高性能响应式流 (SSE)：** 全链路基于 Spring WebFlux + Netty 异步非阻塞架构，单机轻松扛住万级长连接流式吐字响应。
* 🖥️ **现代开发者后台：** 配套 Next.js 14 + Shadcn UI 后台，提供多渠道配置、实时 QPS 看板、Token 消耗大盘及知识库（RAG）管理。

---

## 🏗️ 系统架构蓝图

```text
[ 客户端系统 / Next.js 管理端 ]
              │ (HTTP / Server-Sent Events)
              ▼
    [ Reactive AppKey 令牌桶限流 ]
              │
              ▼
    [ 分布式语义缓存 (Semantic Cache) ] ──(命中)──> [ Redis / 50ms 秒回 ]
              │ (未命中)
              ▼
    [ Resilience4j 动态路由网关 ]
       ├── 渠道 A (主力: DeepSeek) ──(故障/429)──> [ 触发毫秒级自动降级 ]
       └── 渠道 B (备用: 智谱 / Qwen) ──────────────────────┐
              │                                             ▼
              └───────────────────────────────────> [ 顺畅流式输出 (SSE) ]
📦 快速开始1. 后端配置 (application.yml)引入本组件后，仅需在配置文件中声明渠道矩阵，即可激活高可用防线：YAMLenterprise:
  ai:
    ratelimit:
      enabled: true
      default-qps: 5
    cache:
      semantic-enabled: true
      similarity-threshold: 0.93
    channels:
      - id: "ch-deepseek"
        provider: "DEEPSEEK"
        model-name: "deepseek-chat"
        api-key: "sk-xxxxxx"
        base-url: "[https://api.deepseek.com](https://api.deepseek.com)"
        priority: 1
        weight: 90
      - id: "ch-zhipu"
        provider: "ZHIPU"
        model-name: "glm-4"
        api-key: "sk-xxxxxx"
        base-url: "[https://open.bigmodel.cn](https://open.bigmodel.cn)"
        priority: 2
        weight: 10
2. 业务代码集成无侵入接入，在现有的 Spring Bean 中直接注入通用 AI 核心总线：Java@RestController
@RequestMapping("/api/v1/ai")
public class ChatController {

    @Autowired
    private DynamicModelRouter modelRouter;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody ChatQuery query, 
                                                    @RequestHeader("X-App-Key") String appKey) {
        return modelRouter.chatStream(query)
            .map(token -> ServerSentEvent.builder(token)
                .event("message")
                .build())
            .onErrorResume(e -> Flux.just(ServerSentEvent.builder("服务繁忙，请稍后再试")
                .event("error")
                .build()));
    }
}
💎 版本对比 (Open Core vs Pro)功能特性开源社区版 (Lite)商业标准版 (Pro)多模型基础接入 (流式/非流式)AppKey 基础鉴权多渠道加权负载均衡❌Resilience4j 自动故障转移/降级❌分布式语义缓存 (Vector Cache)❌JTokkit 离线精准 Token 扣费❌Next.js 14 + Shadcn UI 管理后台❌企业级 RAG 知识库高级管线❌商业闭源授权执照❌购买/获取源码链接[GitHub][前往 Gumroad / 独立站购买]📄 开源协议 & 商业授权本项目开源核心部分基于 Apache 2.0 协议开放。包含高可用熔断、语义缓存及 Next.js 后台的商业 Pro 版本，需遵循 商业闭源授权执照。严禁未经授权公开发布商业版源码或进行二次二次转售。
© 2026 Enterprise Java-AI Infrastructure. Designed by High-Availability Architects.
📦 快速开始1. 后端配置 (application.yml)引入本组件后，仅需在配置文件中声明渠道矩阵，即可激活高可用防线：YAMLenterprise:
  ai:
    ratelimit:
      enabled: true
      default-qps: 5
    cache:
      semantic-enabled: true
      similarity-threshold: 0.93
    channels:
      - id: "ch-deepseek"
        provider: "DEEPSEEK"
        model-name: "deepseek-chat"
        api-key: "sk-xxxxxx"
        base-url: "[https://api.deepseek.com](https://api.deepseek.com)"
        priority: 1
        weight: 90
      - id: "ch-zhipu"
        provider: "ZHIPU"
        model-name: "glm-4"
        api-key: "sk-xxxxxx"
        base-url: "[https://open.bigmodel.cn](https://open.bigmodel.cn)"
        priority: 2
        weight: 10
2. 业务代码集成无侵入接入，在现有的 Spring Bean 中直接注入通用 AI 核心总线：Java@RestController
@RequestMapping("/api/v1/ai")
public class ChatController {

    @Autowired
    private DynamicModelRouter modelRouter;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody ChatQuery query, 
                                                    @RequestHeader("X-App-Key") String appKey) {
        return modelRouter.chatStream(query)
            .map(token -> ServerSentEvent.builder(token)
                .event("message")
                .build())
            .onErrorResume(e -> Flux.just(ServerSentEvent.builder("服务繁忙，请稍后再试")
                .event("error")
                .build()));
    }
}
💎 版本对比 (Open Core vs Pro)功能特性开源社区版 (Lite)商业标准版 (Pro)多模型基础接入 (流式/非流式)AppKey 基础鉴权多渠道加权负载均衡❌Resilience4j 自动故障转移/降级❌分布式语义缓存 (Vector Cache)❌JTokkit 离线精准 Token 扣费❌Next.js 14 + Shadcn UI 管理后台❌企业级 RAG 知识库高级管线❌商业闭源授权执照❌购买/获取源码链接[GitHub][前往 Gumroad / 独立站购买]📄 开源协议 & 商业授权本项目开源核心部分基于 Apache 2.0 协议开放。包含高可用熔断、语义缓存及 Next.js 后台的商业 Pro 版本，需遵循 商业闭源授权执照。严禁未经授权公开发布商业版源码或进行二次二次转售。© 2026 Enterprise Java-AI Infrastructure. Designed by High-Availability Architects.