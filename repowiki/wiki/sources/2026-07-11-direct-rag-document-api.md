---
title: "2026-07-11-Direct-Rag-Document-Api"
type: Source
description: "将知识库文档操作（上传/列表/删除/config）从「Java 后端代理」改为「前端与 MCP 客户端直连 RAG Python 服务」，消除 Java 代理层导致的长耗时超时。Java 后端仅保留 KB 元数据 CRUD 与搜索代理。"
aliases: [直连RAG文档API设计, direct-rag-document-api-design]
origin: "openspec/changes/archive/2026-07-11-direct-rag-document-api/design.md"
source_type: "md"
tags: [rag, direct, proxy, openspec, design]
title: "直连 RAG 文档 API 设计"
version: "2026-07-11"
---
# 直连 RAG 文档 API 设计

## Summary
将知识库文档操作（上传/列表/删除/config）从「Java 后端代理」改为「前端与 MCP 客户端直连 RAG Python 服务」，消除 Java 代理层导致的长耗时超时。Java 后端仅保留 KB 元数据 CRUD 与搜索代理。

## Key Points
- `kb_document` 表不再写入 MySQL（RAG 列表接口已返回文档元数据，丢失的上传者/时间无产品价值）；表保留向后兼容。
- `KbResponse` 新增 `ragBaseUrl` + `documentsUrl` 动态返回 RAG 地址（不硬编码）；MCP `kb_upload_document` 返回 RAG 直传 URL + curl 示例。
- `kb_search` 保持 Java 代理（秒级响应无超时问题）；RAG 已配 CORS `allow_origins=["*"]`。
- 风险：文档操作无权限控制（内部系统接受）；RAG 宕机时文档操作全失败（KB CRUD 不受影响）。

## Relevance
定义 [[RAG直连架构]] 概念，是 [[知识库]] 文档管线的简化演进，与 [[异步批量上传]] 协同。

## Referenced By
- [[RAG直连架构]]
- [[知识库]]