---
name: codinghub
description: CodingHub 工具广场操作指南。当用户要求搜索/安装/发布/更新 CodingHub 工具，发帖到论坛，或通过 MCP 与 CodingHub 平台交互时使用。前提是已配置 CodingHub MCP 连接（SSE 模式）。
version: 1.0.0
---

# CodingHub 操作指南

通过 MCP（Model Context Protocol）与 CodingHub 工具广场交互，支持工具发现、安装、发布、更新和论坛交流。

## MCP 连接信息

- 协议: SSE（Server-Sent Events）
- 入口: `http://<host>:8082/sse`
- 消息端点: `POST /mcp/message`
- 健康检查: `GET /mcp/health`
- 工具总数: 11 个（6 只读 + 5 写入）

## 工具清单速查

### 只读工具（无需认证）

| 工具 | 用途 | 关键参数 |
|------|------|----------|
| `h3_coding_hub_tool_search` | 搜索工具列表 | `query?`, `category?`, `limit?`(默认20) |
| `h3_coding_hub_tool_get` | 获取工具完整详情（含 markdown 文档） | `toolId` (必填) |
| `h3_coding_hub_tool_files` | 获取工具的文件列表（下载信息） | `toolId` (必填) |
| `h3_coding_hub_tool_download` | 获取文件下载链接 | `toolId`, `fileId` (均必填) |
| `h3_coding_hub_post_search` | 搜索论坛帖子 | `query?`, `limit?`(默认20) |
| `h3_coding_hub_post_get` | 获取帖子完整内容 | `postId` (必填) |

### 写入工具（需 username + password 认证）

| 工具 | 用途 | 关键参数 |
|------|------|----------|
| `h3_coding_hub_tool_create` | 创建新工具 | `name`, `categoryId`, `content`, `version`, `username`, `password` |
| `h3_coding_hub_tool_modify` | 修改已有工具（仅更新传入字段） | `toolId`, `name?`, `categoryId?`, `content?`, `version?`, `username`, `password` |
| `h3_coding_hub_tool_file_upload` | 获取文件上传 REST 端点信息 | `toolId`, `username`, `password` |
| `h3_coding_hub_tool_file_delete` | 删除工具的某个文件 | `toolId`, `fileId`, `username`, `password` |
| `h3_coding_hub_post_create` | 创建论坛帖子 | `title`, `content`, `categoryId`, `username`, `password` |

## 核心工作流

### 1. 搜索与安装工具

**触发词**: "查询工具列表"、"安装工具"、"有没有 XX 工具"

步骤:
1. 调用 `h3_coding_hub_tool_search` 按关键词搜索工具
2. 调用 `h3_coding_hub_tool_get` 获取完整文档（含安装说明、使用方法）
3. 调用 `h3_coding_hub_tool_files` 获取文件列表
4. 对每个需要的文件，调用 `h3_coding_hub_tool_download` 获取下载链接
5. 下载链接返回的是相对路径（如 `/api/v1/tools/{toolId}/files/{fileId}/download`），需拼接服务器基址 `http://<host>:8082` 构成完整 URL
6. 用 curl 下载文件到本地项目目录
7. 把工具版本号写到skill文件夹的tools.version文件中, 以便后续升级时对比

**版本检查**: 如果本地已有 skill，先读取其 `tools.version` 文件中的版本号，与远程工具版本对比，仅在版本不同时才下载安装。

### 2. 发布新工具

**触发词**: "发布 skill"、"上传工具到 CodingHub"

步骤:
1. 获取 CodingHub 账号凭据（按「凭据获取策略」执行：记忆优先，缺失再问用户，首次获取后保存记忆）
2. 确认 categoryId（工具分类 ID），可通过 `h3_coding_hub_tool_search` 查看现有工具的分类来推断
3. 准备工具描述 content（markdown 格式），应包含：工具介绍、安装方法、使用示例
4. 调用 `h3_coding_hub_tool_create` 创建工具，记录返回的 `toolId`
5. **文件上传不走 MCP**——调用 `h3_coding_hub_tool_file_upload` 获取 REST 上传端点信息
6. 用 curl 执行 HTTP multipart POST 上传文件：
   ```bash
   curl -X POST http://<host>:8082/api/v1/tools/{toolId}/files \
     -F "files=@/path/to/file1.zip" \
     -F "files=@/path/to/file2.md" \
     -F "readme=工具简介（可选）"
   ```
7. 上传限制：单文件 50MB，总计 200MB
8. 如果 skill 目录只有 SKILL.md 一个文件，直接上传 SKILL.md 即可，无需压缩；如果包含多个文件，先压缩为 zip 包再上传，保留目录结构

**关键**: 文件上传端点 `/api/v1/tools/{toolId}/files` 无需 JWT 认证（SecurityConfig 已放行）。

### 3. 更新已有工具

**触发词**: "更新工具"、"升级 skill 版本"

步骤:
1. 调用 `h3_coding_hub_tool_search` 找到目标工具，获取 `toolId`
2. 调用 `h3_coding_hub_tool_files` 获取当前文件列表
3. 如需替换文件：对每个要删除的文件调用 `h3_coding_hub_tool_file_delete`（readme 文件可保留）
4. 调用 `h3_coding_hub_tool_modify` 更新工具信息
   - 如果不传 `version` 参数，系统会自动递增最后一段版本号（如 `1.0.0` → `1.0.1`）
   - 也可手动指定版本号（如 `2.0.0`）
   - 未传入的字段保持不变
5. 用 curl 上传新版本文件（同发布流程的第 6 步）

**自动版本号规则**: `1.0.0` → `1.0.1`；`1.0.0-beta` → `1.0.1-beta`；`1.2.3` → `1.2.4`。

### 4. 论坛发帖

**触发词**: "发帖到论坛"、"把这个文档发布到论坛"

步骤:
1. 准备帖子标题和内容（markdown 格式）
2. 确认 categoryId（论坛分类 ID）
3. 调用 `h3_coding_hub_post_create`，传入 title、content、categoryId、username、password
4. 如需发布本地 markdown 文件，先读取文件内容作为 content 参数

## 认证机制

写入工具（create / modify / upload / delete / post_create）采用**参数级认证**：
- 每次调用写入工具时，必须传入 `username` 和 `password` 参数
- 这是因为 MCP over SSE 不携带 HTTP session / JWT，无法使用 Bearer token
- 默认密码为 `123456`（仅限开发/测试环境）

### 凭据获取策略（重要）

需要凭据时，按以下优先级获取：
1. **长期记忆优先**: 先用 `memory_search` 搜索 "CodingHub" 或 "账号密码"，如果记忆中有 username 和 password，直接使用，不要打扰用户
2. **询问用户**: 如果记忆中没有，向用户询问 CodingHub 的 username 和 password
3. **保存到记忆**: 用户首次提供凭据后，立即用 `memory` 工具（target="user"）将 username 和 password 保存到长期记忆，格式示例：`CodingHub account: username=xxx, password=xxx`。后续调用不再重复询问

## 常见陷阱

1. **文件上传走 REST，不走 MCP**: `h3_coding_hub_tool_file_upload` 只返回上传端点信息，实际上传需用 curl 执行 HTTP POST multipart/form-data
2. **下载链接是相对路径**: `h3_coding_hub_tool_download` 返回的 URL 需要拼接 `http://<host>:8082` 前缀
3. **MCP 端点无需 JWT**: `/sse` 和 `/mcp/**` 在 SecurityConfig 中已设为 permitAll，连接时不要传 Authorization header
4. **写操作每次都要凭据**: 没有 session 概念，每个写入调用都是独立的认证
5. **modify 的 partial update**: 只更新传入的字段，没传的保持不变。但 version 如果不传会自动递增
6. **skill 多文件才需要压缩**: 只有 SKILL.md 一个文件时直接上传原文；多文件时才 zip 压缩保留目录结构

## 验证

- 发布/更新后，调用 `h3_coding_hub_tool_get` 确认内容已生效
- 上传文件后，调用 `h3_coding_hub_tool_files` 确认文件列表正确
- 删除文件后，再次调用 `h3_coding_hub_tool_files` 确认文件已移除
- 发帖后，调用 `h3_coding_hub_post_get` 确认内容完整
