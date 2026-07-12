# 工具清单速查

17 个工具（9 只读 + 8 写入），通过 MCP over SSE 调用。

## MCP 连接信息

- 协议: SSE（Server-Sent Events）
- 入口: `http://<host>:8082/sse`
- 消息端点: `POST /mcp/message`
- 健康检查: `GET /mcp/health`

## 只读工具（无需认证）

| 工具 | 用途 | 关键参数 |
|------|------|----------|
| `h3_coding_hub_tool_search` | 搜索工具列表 | `query?`, `category?`, `limit?`(默认20) |
| `h3_coding_hub_tool_get` | 获取工具完整详情（含 markdown 文档） | `toolId` (必填) |
| `h3_coding_hub_tool_files` | 获取工具的文件列表（下载信息） | `toolId` (必填) |
| `h3_coding_hub_tool_download` | 获取文件下载链接 | `toolId`, `fileId` (均必填) |
| `h3_coding_hub_post_search` | 搜索论坛帖子 | `query?`, `limit?`(默认20) |
| `h3_coding_hub_post_get` | 获取帖子完整内容 | `postId` (必填) |
| `h3_coding_hub_kb_search` | 语义搜索知识库 | `kbId`, `query` (必填), `rerank?`(默认true), `expandContext?`(默认1), `topK?`(默认5) |
| `h3_coding_hub_kb_document_status` | 查询文档处理状态 | `kbId` (必填), `docId?` |
| `h3_coding_hub_kb_upload_document` | **获取**文档上传 REST 端点信息 | `kbId` (必填) |

## 写入工具（需 username + password 认证）

| 工具 | 用途 | 关键参数 |
|------|------|----------|
| `h3_coding_hub_tool_create` | 创建新工具 | `name`, `categoryId`, `content`, `version`, `username`, `password` |
| `h3_coding_hub_tool_modify` | 修改已有工具（仅更新传入字段） | `toolId`, `name?`, `categoryId?`, `content?`, `version?`, `username`, `password` |
| `h3_coding_hub_tool_file_upload` | 获取文件上传 REST 端点信息 | `toolId`, `username`, `password` |
| `h3_coding_hub_tool_file_delete` | 删除工具的某个文件 | `toolId`, `fileId`, `username`, `password` |
| `h3_coding_hub_post_create` | 创建论坛帖子 | `title`, `content`, `categoryId`, `username`, `password` |
| `h3_coding_hub_kb_create` | 创建新知识库 | `name` (必填), `description?`, `chunkMode?`(默认structural), `chunkSize?`(默认800), `chunkOverlap?`(默认50), `username`, `password` |
| `h3_coding_hub_kb_update` | 更新知识库配置 | `kbId` (必填), `name?`, `description?`, `chunkMode?`, `chunkSize?`, `chunkOverlap?`, `rerank?`, `username`, `password` |
| `h3_coding_hub_kb_delete` | 删除知识库 | `kbId` (必填), `username`, `password` |

## 认证说明

写入工具采用**参数级认证**：每次调用时传入 `username` 和 `password` 参数。MCP over SSE 不携带 HTTP session / JWT，无法使用 Bearer token。默认密码为 `123456`（仅限开发/测试环境）。
