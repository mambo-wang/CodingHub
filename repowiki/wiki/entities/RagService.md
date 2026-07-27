---
title: RagService RAG 服务
type: entity
---

# RagService RAG 服务

## 定义

RagService 是独立的 Python 服务，提供文档向量化、语义检索、自适应分块能力。同时暴露 MCP 和 REST 双协议接口。

## 代码位置

- 服务: `rag/` (Python)
- Java 客户端: `backend/src/main/java/com/iaihub/toolbox/config/RagConfig.java`
- 文档: `rag/README_CN.md`

## 技术栈

- **向量数据库**: zvec (轻量级嵌入式)
- **Embedding 模型**: Qwen3-Embedding
- **分块策略**: [[rag-adaptive-chunking]] 按文档结构智能切分
- **检索**: 混合检索（向量相似度 + BM25 关键词）

## 核心行为

- **文档索引**: 接收文档 → 自适应分块 → 向量化 → 存入 zvec
- **语义搜索**: query embedding → 向量检索 + BM25 → RRF 融合排序
- **双协议**: MCP Server (AI 客户端) + REST API (Java 后端直连)
- **异步处理**: 文档索引异步执行，状态回调

## 接口

### REST API (Java 后端直连)
- `POST /api/index` — 索引文档
- `POST /api/search` — 语义搜索
- `DELETE /api/index/{doc_id}` — 删除索引
- `GET /api/health` — 健康检查

### MCP 工具
- 通过 [McpServer](McpServer.md) 的 kb_search 工具间接调用

## 关联实体

[KnowledgeBase](KnowledgeBase.md) · [McpServer](McpServer.md)

## 设计决策来源

- rag-direct-api (2026-07-03)
- rag-adaptive-chunking (2026-07-26)
- knowledge-base-module (2026-06-27)
