---
title: "Streamable-Http传输模式"
type: Concept
description: "> **一句话**：MCP 的 HTTP+SSE 双端点传输已被 2025-03-26 协议版本废弃；新项目一律使用 Streamable HTTP 单端点，存量 SSE 尽快迁移。"
aliases: [Streamable HTTP, SSE 连接泄漏, MCP 传输层]
source_refs: [mcp-server-best-practices]
---

# Streamable HTTP 传输模式（SSE 已死）

> **一句话**：MCP 的 HTTP+SSE 双端点传输已被 2025-03-26 协议版本废弃；新项目一律使用 Streamable HTTP 单端点，存量 SSE 尽快迁移。

## 坑 1：SSE 连接泄漏（CodingHub 实战）

旧版 HTTP+SSE 传输需要两个端点（`GET /sse` 建立长连接 + `POST /message` 发消息），生产中暴露的问题：

- 客户端异常退出（进程 kill、网络切换）时 SSE 长连接不会立即断开，服务端连接表持续膨胀
- 无心跳机制时，僵尸连接可存活数小时，耗尽连接池/文件句柄
- 会话与连接强绑定，连接断了会话即丢，无法恢复

## Streamable HTTP 的改进

| 维度 | HTTP+SSE（废弃） | Streamable HTTP |
|------|------------------|-----------------|
| 端点 | 2 个（/sse + /message） | 1 个（/mcp） |
| 会话 | 绑定连接 | `Mcp-Session-Id` 头，可断线恢复 |
| 响应 | 全走 SSE 流 | 按需：普通 JSON 或升级为 SSE 流 |
| 无状态部署 | 不可能 | 可选（每请求独立） |
| 连接管理 | 长连接常驻 | 短连接为主，仅流式时保持 |

## 在本仓库的落地

- [MCP服务](../modules/MCP服务.md)：`/mcp` 单端点（`McpController` + `McpSdkServerConfig`），`McpConnectionManager` 管理会话生命周期与清理
- [McpNotificationService](../entities/McpNotificationService.md)：订阅通知依赖按需升级的 SSE 流推送
- 经验：迁移后连接数从峰值数百降至个位数，无再现泄漏

## 相关

- 源文档：[mcp-server-best-practices](../sources/mcp-server-best-practices.md)
- 数据传输哲学：[MCP控制通道与数据通道分离](MCP控制通道与数据通道分离.md)（二进制不走 MCP 消息体）
