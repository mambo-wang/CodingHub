---
title: KnowledgeBase 知识库
type: entity
---

# KnowledgeBase 知识库

## 定义

知识库是 RAG 检索增强模块的核心实体，管理文档集合及其向量索引。采用 MySQL 存元数据 + Python 服务存向量的混合架构。

## 代码位置

- 实体: `backend/src/main/java/com/iaihub/toolbox/model/kb/KnowledgeBase.java`, `KbDocument.java`
- 服务: `backend/src/main/java/com/iaihub/toolbox/service/kb/KnowledgeBaseService.java`
- 控制器: `backend/src/main/java/com/iaihub/toolbox/controller/kb/KnowledgeBaseController.java`
- RAG 服务: `rag/` (Python, MCP + REST API)
- 前端: `frontend/src/pages/knowledge/`, `frontend/src/components/knowledge/`

## 关键字段

### KnowledgeBase
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 知识库名称 |
| description | String | 描述 |
| owner | User | 创建者 |
| documentCount | Integer | 文档数 |
| status | KbStatus | ACTIVE / DELETED |

### KbDocument
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| knowledgeBase | KnowledgeBase | 所属知识库 |
| fileName | String | 文件名 |
| fileSize | Long | 文件大小 |
| chunkCount | Integer | 分块数 |
| status | DocStatus | PENDING/PROCESSING/READY/FAILED |

## 核心行为

- **文档管理**: 上传/删除/状态跟踪（6 态状态机）
- **向量索引**: 调用 [[RagService]] 进行分块和向量化
- **语义搜索**: 通过 [[RagService]] 混合检索（向量 + BM25）
- **MCP 暴露**: [[McpServer]] 提供 kb_search / kb_list 工具
- **自适应分块**: [[rag-adaptive-chunking]] 按文档结构智能切分

## API 端点

- `GET /api/v1/knowledge` — 知识库列表
- `POST /api/v1/knowledge` — 创建知识库
- `POST /api/v1/knowledge/{id}/documents` — 上传文档
- `GET /api/v1/knowledge/{id}/documents` — 文档列表
- `POST /api/v1/knowledge/{id}/search` — 语义搜索

## 关联实体

[User](User.md) · [[RagService]] · [[McpServer]]

## 设计决策来源

- knowledge-base-module (2026-06-27)
- kb-mcp-tools (2026-06-29)
- kb-ux-improvements (2026-07-07)
- rag-direct-api (2026-07-03)
- rag-adaptive-chunking (2026-07-26)
