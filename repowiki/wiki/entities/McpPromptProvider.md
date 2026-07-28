---
title: "Mcppromptprovider"
type: Entity
description: "> 代码：`backend/src/main/java/com/iaihub/toolbox/mcp/McpPromptProvider.java`"
aliases: [MCP Prompt模板, Prompt Provider]
source_refs: [mcp-server-best-practices]
---

# McpPromptProvider（MCP Prompt 模板提供者）

> 代码：`backend/src/main/java/com/iaihub/toolbox/mcp/McpPromptProvider.java`

## 职责

为 CodingHub MCP Server 提供 6 个 Prompt 模板，对标 SKILL 的「场景化工作流指引」能力，是 [[MCP与SKILL能力对等]] 的关键拼图之一（MCP 原语 Prompts）。

## 6 个 Prompt 模板

| Prompt | 用途 |
|--------|------|
| `publish_tool_guide` | 发布工具的完整流程指引 |
| `install_tool_guide` | 安装工具的流程指引 |
| `update_tool_guide` | 更新已发布工具的流程指引 |
| `forum_post_guide` | 发帖工作流指引 |
| `kb_ingest_guide` | 知识库文档导入指引 |
| `search_and_evaluate` | 搜索并评估工具的方法论 |

## 设计要点（来自 [mcp-server-best-practices](../sources/mcp-server-best-practices.md)）

- Prompt 是「用户显式选择」的原语（user-controlled），与 Tools（model-controlled）、Resources（application-controlled）职责分离
- 模板内容为 SOP 式流程指引，客户端（如 CodeBuddy / Claude）通过 `prompts/list` + `prompts/get` 获取
- 与工具 description 的区别：description 告诉模型「何时调用」，Prompt 告诉模型「完整工作流怎么走」

## 关联

- 所属模块：[MCP服务](../modules/MCP服务.md)
- 兄弟组件：[[McpResourceHandler]]（Resources 原语）、[[McpNotificationService]]（订阅通知）
- 注册位置：`McpSdkServerConfig`（18 tools + prompts + resources 统一装配）
