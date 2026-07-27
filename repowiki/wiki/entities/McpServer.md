---
title: McpServer 嵌入式 MCP 服务
type: entity
---

# McpServer 嵌入式 MCP 服务

## 定义

McpServer 是嵌入在 Spring Boot 应用中的 MCP (Model Context Protocol) 服务端，通过 Streamable HTTP/SSE 向 AI 客户端暴露 18 个工具。

## 代码位置

- 包: `backend/src/main/java/com/iaihub/toolbox/mcp/`
- 配置: `backend/src/main/java/com/iaihub/toolbox/config/McpConfig.java`
- 端点: `POST /mcp` (JSON-RPC 2.0), `GET /mcp/sse` (SSE)
- 健康检查: `GET /mcp/health`

## 工具清单（18 tools）

### 工具广场
- `h3_coding_hub_tool_search` — 搜索工具
- `h3_coding_hub_tool_get` — 获取工具详情
- `h3_coding_hub_tool_files` — 获取工具附件
- `h3_coding_hub_tool_create` — 创建工具
- `h3_coding_hub_tool_modify` — 修改工具
- `h3_coding_hub_tool_download` — 下载附件
- `h3_coding_hub_tool_file_upload` — 上传附件
- `h3_coding_hub_tool_file_delete` — 删除附件

### 论坛
- `h3_coding_hub_post_search` — 搜索帖子
- `h3_coding_hub_post_get` — 获取帖子详情
- `h3_coding_hub_post_create` — 创建帖子

### 知识库
- `h3_coding_hub_kb_list` — 知识库列表
- `h3_coding_hub_kb_search` — 语义搜索
- `h3_coding_hub_kb_create` — 创建知识库
- `h3_coding_hub_kb_update` — 更新知识库
- `h3_coding_hub_kb_delete` — 删除知识库
- `h3_coding_hub_kb_upload_document` — 上传文档
- `h3_coding_hub_kb_document_status` — 文档状态

## 核心行为

- **协议**: JSON-RPC 2.0 over HTTP + SSE 事件流
- **认证**: 复用 [[jwt-dual-token-auth]] Bearer Token
- **权限**: 写操作需 ADMIN 角色（[[rbac-permission]]）
- **直连 RAG**: 知识库搜索通过 [[RagService]] REST API 而非 MCP 嵌套

## 关联实体

[Tool](Tool.md) · [ForumPost](ForumPost.md) · [KnowledgeBase](KnowledgeBase.md) · [[RagService]]

## 设计决策来源

- mcp-tool-modify-delete (2026-06-05)
- kb-mcp-tools (2026-06-29)
- rag-direct-api (2026-07-03)
- tool-ui-mcp-fixes (2026-07-09)
