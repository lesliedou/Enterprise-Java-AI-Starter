```markdown
# Enterprise Java-AI Starter 🚀

[![Spring Boot Version](https://img.shields.io/badge/Spring%20Boot-3.3+-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java Version](https://img.shields.io/badge/Java-17%20%2F%2021-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-Dual--License-orange.svg)]()

**A production-ready, highly-available Java AI Gateway, Broker, and Scaffolding Framework built for enterprise-grade commercial systems.**

---

## 📌 Why Enterprise Java-AI Starter?

When integrating Large Language Models (LLMs) into mission-critical corporate applications (e.g., E-commerce, ERP, CRM), development teams face critical architectural bottlenecks:

1. **API Instability & Rate Limits:** Public LLM endpoints frequently suffer from `429 Too Many Requests` or `50x Internal Server Errors`, breaking business continuity.
2. **Exploding Token Costs:** Redundant, similar prompts continuously burn capital due to a lack of production-grade semantic caching.
3. **Ecosystem Friction:** While AI experimentation thrives in Python, robust core business systems operate on Java. Forcing them together introduces major operational and infrastructure overhead.
4. **Lack of Billing and Governance:** Out-of-the-box solutions miss offline token auditing, reactive multi-tenant rate limiting, and enterprise-grade guardrails.

`Enterprise Java-AI Starter` bridges this gap using the **native Java AI ecosystem (LangChain4j)** and an **end-to-end reactive architecture (Spring WebFlux)**. It delivers a **non-invasive** AI proxy gateway that gives any Java system robust, cloud-native AI capabilities in minutes.

---

## ✨ Key Features

* ⚖️ **Dynamic Multi-Model Routing:** Unified abstraction layer supporting OpenAI, DeepSeek, Anthropic, and top Chinese LLMs. Requests are dynamically routed using priority tiers and weighted round-robin algorithms.
* 🛡️ **Resilience-as-a-Service (Failover):** Backed by Resilience4j circuit breakers. If a primary channel (e.g., DeepSeek) experiences a rate limit or service blackout, the framework executes a **millisecond-level automated failover** to a backup channel without disrupting client Server-Sent Events (SSE).
* 💰 **Distributed Semantic Cache:** Converts prompts to embeddings ($\ge 0.93$ similarity) to hit Redis caches directly. **Reduces corporate token bills by up to 90%** and slashes response latency to under 50ms.
* 📊 **Offline Token Auditing:** Leverages `jtokkit` for exact, zero-cost offline token serialization. Emits decoupled reactive Spring Events immediately after stream completion for asynchronous account credit deductions.
* ⚡ **High-Performance Reactive Pipeline:** End-to-end asynchronous, non-blocking flow running on Spring WebFlux and Netty, optimized to hold tens of thousands of concurrent long-lived SSE connections.
* 🖥️ **Next.js Developer Portal:** Package includes a full Next.js 14 + Shadcn UI management console showcasing live channel health metrics, real-time QPS trackers, token consumption distributions, and advanced RAG vector pipelines.

---

## 🏗️ Architecture Blueprint

```text
[ Client Systems / Next.js Admin Panel ]
              │ (HTTP / Server-Sent Events)
              ▼
    [ Reactive AppKey Rate Limiter ]
              │
              ▼
    [ Distributed Semantic Cache ] ──(Hit)──> [ Redis / 50ms Ultrafast Response ]
              │ (Miss)
              ▼
    [ Resilience4j Dynamic Router ]
       ├── Primary: DeepSeek ──(Fault/429 Error)──> [ Millisecond Failover Triggered ]
       └── Fallback: Anthropic / Qwen ──────────────────────┐
              │                                             ▼
              └───────────────────────────────────> [ Smooth SSE Token Stream ]
📦 Quick Start
1. Backend Configuration (application.yml)
Activate your high-availability shield by declaring your model matrix inside your property sheets:              
enterprise:
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
      - id: "ch-anthropic"
        provider: "ANTHROPIC"
        model-name: "claude-3-5-sonnet"
        api-key: "sk-xxxxxx"
        base-url: "[https://api.anthropic.com](https://api.anthropic.com)"
        priority: 2
        weight: 10
 2. Business Integration
Inject high-availability AI routines directly into your active beans without rewriting legacy business logic:       

@RestController
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
            .onErrorResume(e -> Flux.just(ServerSentEvent.builder("Service busy. Please try again later.")
                .event("error")
                .build()));
    }
}
💎 Edition Matrix (Open Core vs Pro)FeaturesOpen Core (Lite)Commercial (Pro)Basic Multi-Model Connection (Stream/Block)AppKey Basic Security GuardWeighted Channel Load Balancing❌Resilience4j Auto Circuit Breaking & Failover❌Distributed Semantic Vector Caching❌JTokkit Asynchronous Precision Billing Engine❌Next.js 14 + Shadcn UI Full Developer Suite❌Enterprise RAG Knowledge Base Architecture❌Commercial Source-Code License❌Get Access[GitHub][Buy Pro Version on Gumroad / Stripe]📄 License & ComplianceThe core open-source framework is licensed under the terms of the Apache 2.0 License.The Pro Version (including advanced failover, semantic vector cache, billing modules, and the Next.js control center) is protected under a Commercial Source Code License. Unauthorized public distribution, mirror dumping, or reselling of Pro repositories is strictly prohibited.© 2026 Enterprise Java-AI Infrastructure. Designed by High-Availability Architects.