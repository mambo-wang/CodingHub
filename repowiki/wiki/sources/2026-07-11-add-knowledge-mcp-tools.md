---
title: "2026-07-11-Add-Knowledge-Mcp-Tools"
type: Source
description: "通过 MCP 协议暴露知识库核心操作，新增 6 个独立工具（`kb_list/search/create/update/delete/upload_document`），使 AI 客户端能管理知识库。读操作公开，写操作复用 `username/password` 内联认证，直接复用 `KnowledgeBaseService`，MCP 层不重复业务逻辑。"
aliases: [知识库MCP工具设计, kb-mcp-tools-design]
origin: "openspec/changes/archive/2026-07-11-add-knowledge-mcp-tools/design.md"
source_type: "md"
tags: [mcp, knowledge, openspec, design]
title: "知识库 MCP 工具设计"
version: "2026-07-11"
---
# 知识库 MCP 工具设计

## Summary
通过 MCP 协议暴露知识库核心操作，新增 6 个独立工具（`kb_list/search/create/update/delete/upload_document`），使 AI 客户端能管理知识库。读操作公开，写操作复用 `username/password` 内联认证，直接复用 `KnowledgeBaseService`，MCP 层不重复业务逻辑。

## Key Points
- 工具命名遵循 `h3_coding_hub_kb_*` 前缀；MCP 工具总数由 11 增至 17。
- 文档上传采用「REST API 引导」模式（返回上传地址，客户端 HTTP 直传），与 `tool_file_upload` 一致，不直传二进制。
- 搜索为单库搜索（需 kbId），先 `kb_list` 再指定 `kbId` 搜；不实现跨库全局搜索。
- Non-Goals：不加配置管理/列表删除工具（留待后续），不改前端，不传文件二进制。

## Relevance
扩展 [[MCP工具集]]；与 [[知识库]]、[[RAG直连架构]] 配合，AI 助手可经 MCP 管理知识库。

## Referenced By
- [[MCP工具集]]
- [[知识库]]