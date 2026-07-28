---
title: "Mcpresourcehandler"
type: Entity
description: "> 代码：`backend/src/main/java/com/iaihub/toolbox/mcp/McpResourceHandler.java`"
aliases: [MCP Resources, Resource Handler]
source_refs: [mcp-server-best-practices]
---

# McpResourceHandler（MCP Resource 处理器）

> 代码：`backend/src/main/java/com/iaihub/toolbox/mcp/McpResourceHandler.java`

## 职责

为 CodingHub MCP Server 提供 3 个 Resources 与 Resource Template，对标 SKILL 的「参考文件」能力，补齐 MCP 三大原语中的 Resources，是 [[MCP与SKILL能力对等]] 的组成部分。

## 资源清单

| Resource URI | 内容 | 特点 |
|--------------|------|------|
| `codinghub://categories` | 工具分类列表 | 只读快照 |
| `codinghub://tags` | 标签列表 | 只读快照 |
| `codinghub://stats` | 平台统计数据 | 只读快照 |
| `codinghub://tool/{id}`（Template） | 单个工具详情 | 参数化 URI 模板 |

## 设计要点（来自 [mcp-server-best-practices](../sources/mcp-server-best-practices.md)）

- **Resources 保持只读**：不含业务逻辑，仅提供上下文数据（application-controlled）
- 与 Tools 的边界：查数据用 Resource，改数据用 Tool——避免用 Tool 冒充 Resource 浪费模型的工具选择注意力
- 订阅更新通过 [[McpNotificationService]] 推送 `notifications/resources/updated`

## 关联

- 所属模块：[MCP服务](../modules/MCP服务.md)
- 兄弟组件：[McpPromptProvider](McpPromptProvider.md)（Prompts 原语）、[[McpNotificationService]]（订阅通知）
