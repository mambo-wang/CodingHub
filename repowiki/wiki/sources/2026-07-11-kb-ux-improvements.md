---
title: "2026-07-11-Kb-Ux-Improvements"
type: Source
description: "知识库 UX 增强：搜索结果以 Markdown 渲染（markdown-it + highlight.js，`html:false` 防 XSS），文档列表支持所有类型文件（含文本）源文件下载，本地 `npm run dev` 可用全部 KB 功能。"
aliases: [知识库UX改进设计, kb-ux-improvements-design]
origin: "openspec/changes/archive/2026-07-11-kb-ux-improvements/design.md"
source_type: "md"
tags: [knowledge, ux, openspec, design]
title: "知识库 UX 改进设计"
version: "2026-07-11"
---
# 知识库 UX 改进设计

## Summary
知识库 UX 增强：搜索结果以 Markdown 渲染（markdown-it + highlight.js，`html:false` 防 XSS），文档列表支持所有类型文件（含文本）源文件下载，本地 `npm run dev` 可用全部 KB 功能。

## Key Points
- `KnowledgeSearch.vue` 内独立轻量 markdown-it 实例，不抽取通用组件（PostContent 过重）；`html:false` 防止恶意 HTML。
- 下载端点 `GET /api/collections/{name}/documents/download?filepath=xxx`（query 参数与删除风格一致）；RAG 校验 filepath 在 `_uploads/{col}/` 下防路径遍历。
- `ingest_content` 分块前将文本文件原文件写入 `_uploads/`，统一可下载；前端用 `<a download>` 原生触发。
- Non-Goals：MCP 不支持下载、不抽取通用 MarkdownRenderer、不支持预览/大文件流式下载。

## Relevance
对应 [[知识库]] 的前端体验优化，依赖 [[RAG直连架构]] 的直连下载链路。

## Referenced By
- [[知识库]]
- [[RAG直连架构]]