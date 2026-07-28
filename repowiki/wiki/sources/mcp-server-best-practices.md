---
title: "Mcp-Server-Best-Practices"
type: Source
description: "> 源文件：`raw/sources/mcp-server-best-practices.md`（导入自 `docs/MCP Server 开发最佳实践与踩坑记录.md`）"
aliases: [MCP最佳实践, MCP踩坑记录, MCP best practices]
origin: "docs/MCP Server 开发最佳实践与踩坑记录.md"
---
# MCP Server 开发最佳实践与踩坑记录（源文档摘要）

> 源文件：[raw/sources/mcp-server-best-practices.md](../../raw/sources/mcp-server-best-practices.md)（导入自 `docs/MCP Server 开发最佳实践与踩坑记录.md`）

## 文档概览

该文档沉淀了 CodingHub 与 CodeWiki-CN 两个项目的 MCP Server 开发经验，分为通用最佳实践（一~七章）、实战踩坑记录（第八章）、MCP 与 SKILL 能力对等实践（第九章）和核心哲学（第十章）。

## 核心要点

### 通用最佳实践

- **单一职责边界**：每个 MCP Server 围绕一个能力域构建，功能膨胀时拆分
- **三大原语解耦**：Tools / Resources / Prompts 保持独立，Resources 只读不含业务逻辑
- **Server Instructions 必写**：`instructions` 字段是给 LLM 的使用手册
- **工具设计**：唯一 name + 面向 LLM 的 description + inputSchema；无状态幂等；双重可读输出（结构化 JSON + 文本块）；高风险操作人在环路；单 Server 工具数控制在 15-20 个内
- **错误分层**：协议层用 JSON-RPC 错误码，运行时错误用 `isError: true` + content 详情
- **安全**：HTTP 传输强制 OAuth 2.1、凭证不落日志、输入验证、最小权限
- **性能**：异步 + 超时取消、连接池、缓存、启动预热（warmup）

### 实战三坑（详见概念页）

1. **SSE 连接泄漏**（CodingHub）→ 迁移 [Streamable-HTTP传输模式](../concepts/Streamable-HTTP传输模式.md)
2. **MCP 通道传大数据撑爆上下文**（CodeWiki-CN）→ file-side-channel，见 [MCP控制通道与数据通道分离](../concepts/MCP控制通道与数据通道分离.md)
3. **Streamable HTTP 无法传二进制**（CodingHub）→ MCP 返回端点信息、HTTP 执行传输，见 [MCP控制通道与数据通道分离](../concepts/MCP控制通道与数据通道分离.md)

### MCP 与 SKILL 能力对等

通过 18 个工具 + 6 个 Prompt（[McpPromptProvider](../entities/McpPromptProvider.md)）+ 3 个 Resource（[McpResourceHandler](../entities/McpResourceHandler.md)，含 [McpNotificationService](../entities/McpNotificationService.md) 订阅通知）实现与 SKILL 完全对等，详见 [MCP与SKILL能力对等](../concepts/MCP与SKILL能力对等.md)。

### 核心哲学

> 把 MCP Server 当作给 LLM 用的 API 来设计：清晰契约（Schema）、明确边界（单一职责）、防御性编程、对 LLM Agent 友好的接口。**MCP 是控制通道，不是数据通道；SSE 已死，Streamable HTTP 当立。**三大原语缺一不可。

## 相关模块

- [MCP服务](../modules/MCP服务.md) — 本文档经验的主要落地模块
