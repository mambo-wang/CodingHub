# Implementation Plan: 嵌入式 MCP Server

**Branch**: `[001-ai-tool-square]` | **Date**: 2026-05-31 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-embedded-mcp-server/spec.md`

## Summary

在 Spring Boot 应用中嵌入 MCP Server，通过 HTTP + SSE 协议为 AI IDE 提供工具广场的检索和查询能力。MCP Server 随应用自动启动，运行在独立端口（8082），无需认证，支持工具检索/内容查询、文件下载、帖子检索/内容查询功能。

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.2.5)

**Primary Dependencies**:
- Spring Boot Starter Web (MVC)
- Spring Boot Starter Data JPA
- Spring Boot Starter Security (仅用于配置豁免 MCP 端点)
- MySQL Connector 8.3.0
- Logstash JSON Logback Encoder

**Storage**: MySQL 8.x (复用现有 tool, post, category 表结构)

**Testing**: JUnit 5 + Spring Boot Test + H2 Memory Database

**Target Platform**: Linux Server (Java 17 Runtime)

**Project Type**: Web Service (REST API + MCP Protocol)

**Performance Goals**:
- MCP 连接建立: < 3s (SC-001)
- 工具检索响应: < 500ms (SC-002)
- 帖子检索响应: < 1s (SC-004)
- 支持至少 5 个并发连接 (SC-003)

**Constraints**:
- MCP Server 启动失败不影响主应用 (SC-006)
- MCP Server 无需身份验证 (FR-010)

**Scale/Scope**: 100-500 个工具，500-2000 个帖子

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 代码质量标准 | ✅ | 使用 Lombok 简化代码，遵循单职责原则 |
| 测试标准 | ⚠️ | 需要为 MCP Server 添加单元测试和集成测试 |
| 用户体验一致性 | ✅ | N/A - 后端服务无直接 UI |
| 性能要求 | ✅ | 符合 SC-001 至 SC-006 定义的目标 |
| 可观测性 | ✅ | 已配置 Logstash JSON 日志格式 |
| 简洁 & YAGNI | ✅ | 仅实现 MCP 协议规定的工具，无过度设计 |

**结论**: 可通过 Gate，测试标准需要额外关注。

## Project Structure

### Documentation (this feature)

```text
specs/003-embedded-mcp-server/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
backend/src/main/java/com/iaihub/toolbox/
├── config/
│   └── McpServerConfig.java          # MCP Server 自动配置
├── mcp/
│   ├── McpServer.java               # MCP Server 核心类
│   ├── McpResourceHandler.java       # MCP 资源处理器
│   ├── McpToolHandler.java          # MCP 工具处理器
│   └── protocol/
│       ├── McpMessage.java          # MCP 消息模型
│       └── McpResponse.java         # MCP 响应模型
├── service/
│   └── McpSearchService.java        # MCP 搜索服务
├── controller/
│   └── McpController.java           # MCP HTTP 端点
└── ToolSquareApplication.java       # 启动类（已修改）

backend/src/test/java/com/iaihub/toolbox/
├── mcp/
│   ├── McpServerTest.java
│   ├── McpResourceHandlerTest.java
│   └── McpSearchServiceTest.java
└── integration/
    └── McpIntegrationTest.java
```

**Structure Decision**: 在 backend 模块下新增 `mcp/` 包目录，包含 MCP 协议处理核心逻辑。采用 Spring Boot 原生组件（无额外第三方 MCP 依赖），通过 HttpHandler + SSE 实现协议通信。

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 无 | - | - |