---
title: "2026-07-11-Add-Knowledge-Base"
type: Source
description: "新增知识库（KB）模块，实现知识库生命周期管理（CRUD）、文档上传/列表/删除、参数配置（chunk_mode/size/overlap/rerank）与基于知识库的语义搜索（返回片段，非对话）。采用「MySQL 元数据 + RAG 向量存储」混合模式，Java 后端作权限/代理中间层。"
aliases: [知识库模块设计, knowledge-base-design]
origin: "openspec/changes/archive/2026-07-11-add-knowledge-base/design.md"
source_type: "md"
tags: [knowledge, rag, openspec, design]
title: "知识库模块设计"
version: "2026-07-11"
---
# 知识库模块设计

## Summary
新增知识库（KB）模块，实现知识库生命周期管理（CRUD）、文档上传/列表/删除、参数配置（chunk_mode/size/overlap/rerank）与基于知识库的语义搜索（返回片段，非对话）。采用「MySQL 元数据 + RAG 向量存储」混合模式，Java 后端作权限/代理中间层。

## Key Points
- MySQL 存 `knowledge_base`/`kb_document` 元数据；RAG Python 服务（`:8000`）负责向量与检索。Java 用内置 `java.net.http.HttpClient`（零依赖）经 `RagApiClient` 代理 RAG API。
- RAG collection 名 = 用户输入名（MySQL 查重防重名，409）；`kb_document.status` 软删除；搜素免登录。
- 搜索返回 chunk 文本片段（含来源/分数），前端卡片展示；不接 LLM 问答、不扩展 unified-interactions、不做标签。
- 部署：独立启动 RAG（`python server.py --port 8000`），`app.rag.base-url` 配置，后端 `ddl-auto:update` 自动建表。

## Relevance
对应 [[知识库]] 实体模块，与 [[RAG自适应分块]]、[[异步批量上传]]、[[RAG直连架构]] 紧密相关。

## Referenced By
- [[知识库]]
- [[RAG自适应分块]]
- [[异步批量上传]]
- [[RAG直连架构]]