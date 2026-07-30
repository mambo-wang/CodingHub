---
title: "Mcp工具集"
type: Entity
description: "后端通过 MCP 协议（`/mcp` Streamable HTTP + `/sse`）对外暴露的 AI 可调用工具集，使 AI 客户端能查询/管理平台资源（工具、帖子、知识库等）。写类工具内部用调用方传入的 `username/password` 调 `userService.login` 鉴权。"
aliases: [McpToolSet, MCP工具, mcp-tool-set]
category: "integration"
source_refs: [openspec/changes/archive/2026-06-15-add-mcp-tool-modify-delete/design.md, openspec/changes/archive/2026-07-11-add-knowledge-mcp-tools/design.md, openspec/changes/archive/2026-07-11-tool-ui-and-mcp-fixes/design.md]
tags: [mcp, ai, openspec]
title: "MCP 工具集"
---
# MCP 工具集

## Definition
后端通过 MCP 协议（`/mcp` Streamable HTTP + `/sse`）对外暴露的 AI 可调用工具集，使 AI 客户端能查询/管理平台资源（工具、帖子、知识库等）。写类工具内部用调用方传入的 `username/password` 调 `userService.login` 鉴权。

## Key Attributes
- 命名前缀 `h3_coding_hub_*`，总数随需求演进（知识库 MCP 后达 17，工具增改删后更多）。
- 查/写二分：读操作公开；写操作（tool_create/modify/file_delete、kb_* 写）复用业务 Service，MCP 层不重复逻辑。
- 文档上传采用「REST 引导」模式（返回上传地址，客户端 HTTP 直传）。
- 不绕经 `InteractionController`；可用 `admin/Cloud@1234` 内联鉴权。

## Relationships
- 知识库相关见 [知识库](知识库.md)（kb_* 工具）；工具写能力见 [[工具 UI 与 MCP 修复设计]]、[[MCP 工具增改删设计]]。
- `/mcp/**`、`/sse/**` 在 SecurityConfig 为 permitAll（MCP 调用无需 token）。

## Source References
- [2026-06-15-add-mcp-tool-modify-delete](../sources/2026-06-15-add-mcp-tool-modify-delete.md)
- [2026-07-11-add-knowledge-mcp-tools](../sources/2026-07-11-add-knowledge-mcp-tools.md)
- [2026-07-11-tool-ui-and-mcp-fixes](../sources/2026-07-11-tool-ui-and-mcp-fixes.md)