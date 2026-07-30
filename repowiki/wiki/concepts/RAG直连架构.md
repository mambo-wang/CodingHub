---
title: "Rag直连架构"
type: Concept
description: "知识库文档操作（上传/列表/删除/config）从「Java 后端代理」改为「前端与 MCP 客户端直连 RAG Python 服务」的架构简化，消除 Java 代理层导致的长耗时超时。Java 后端仅保留 KB 元数据 CRUD 与搜索代理。"
aliases: [DirectRag, RAG直连, direct-rag]
domain: "architecture"
source_refs: [openspec/changes/archive/2026-07-11-direct-rag-document-api/design.md, openspec/changes/archive/2026-07-11-kb-ux-improvements/design.md]
tags: [rag, architecture, proxy, openspec]
title: "RAG 直连架构"
---
# RAG 直连架构

## Definition
知识库文档操作（上传/列表/删除/config）从「Java 后端代理」改为「前端与 MCP 客户端直连 RAG Python 服务」的架构简化，消除 Java 代理层导致的长耗时超时。Java 后端仅保留 KB 元数据 CRUD 与搜索代理。

## Key Points
- `kb_document` 表不再写 MySQL（RAG 列表已返元数据，丢失的上传者/时间无产品价值）；表保留兼容。
- `KbResponse` 动态返回 `ragBaseUrl` + `documentsUrl`（不硬编码）；MCP `kb_upload_document` 返直传 URL + curl 示例。
- `kb_search` 保持 Java 代理（秒级无超时）；RAG 配 CORS `allow_origins=["*"]`。
- 取舍：文档操作无权限控制（内部系统）；RAG 宕机时文档操作全失败（KB CRUD 不受影响）。

## Related
- 属于 [知识库](../entities/知识库.md) 文档管线；与 [异步批量上传](异步批量上传.md) 协同（客户端直传）。
- 前端下载见 [[知识库 UX 改进设计]]。

## Source References
- [2026-07-11-direct-rag-document-api](../sources/2026-07-11-direct-rag-document-api.md)
- [2026-07-11-kb-ux-improvements](../sources/2026-07-11-kb-ux-improvements.md)