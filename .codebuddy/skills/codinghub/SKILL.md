---
name: codinghub
description: CodingHub 工具广场操作指南。当用户要求搜索/安装/发布/更新 CodingHub 工具，发帖到论坛，管理知识库，或通过 MCP 与 CodingHub 平台交互时使用。前提是已配置 CodingHub MCP 连接（SSE 模式）。
version: 2.1.0
---

# CodingHub 操作指南

通过 MCP（Model Context Protocol）与 CodingHub 工具广场交互，支持工具发现、安装、发布、更新、论坛交流，以及知识库管理（创建、文档上传、语义检索）。

## 网站访问
。
当用户说"打开codinghub网站"、"访问codinghub"时，打开浏览器访问 CodingHub 网站，地址为CodingHub MCP Server的IP地址

## MCP 连接信息

- 协议: SSE（Server-Sent Events）
- 入口: `http://<host>:8082/sse`
- 消息端点: `POST /mcp/message`
- 健康检查: `GET /mcp/health`
- 工具总数: 17 个（9 只读 + 8 写入）

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
| `h3_coding_hub_kb_search` | 语义搜索知识库 | `kbId`, `query` (必填), `rerank?`(默认true), `expandContext?`(默认1), `topK?`(默认5) |
| `h3_coding_hub_kb_document_status` | 查询文档处理状态 | `kbId` (必填), `docId?` |
| `h3_coding_hub_kb_upload_document` | **获取**文档上传 REST 端点信息 | `kbId` (必填) |

### 写入工具（需 username + password 认证）

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

### 5. 知识库管理

**触发词**: "创建知识库"、"上传文档"、"检索知识库"、"搜索知识库"

知识库（Knowledge Base）模块支持文档管理、语义检索和 RAG（检索增强生成）。文档上传后经过分块（Chunking）→ 向量化（Embedding）→ 可搜索。

#### 5.1 创建知识库

步骤:
1. 获取 CodingHub 账号凭据（同「凭据获取策略」）
2. 调用 `h3_coding_hub_kb_create`，传入以下参数：
   - `name`（必填）：知识库名称
   - `description`：知识库用途描述（推荐填写，便于后续管理）
   - `chunkMode`：分块模式，默认 `structural`（按文档结构分块），可选 `fixed`（固定大小分块）
   - `chunkSize`：分块大小（字符数），默认 800
   - `chunkOverlap`：分块重叠（字符数），默认 50
3. 记录返回的 `kbId`，后续上传文档、检索均需使用

> **rerank 策略**：创建知识库时无需传 `rerank` 参数，系统默认启用重排序。如需关闭可在后续更新时调整。

#### 5.2 上传文档到知识库

**触发词**: "上传文档"、"添加到知识库"、"把文件传到知识库"

> **重要**：上传前优先使用 **markitdown-mcp** 做文档预处理，确保图片可见、格式正确。

步骤:
1. 确认目标知识库的 `kbId`
2. **文档预处理**（关键步骤）：
   - **纯文字文档**（无嵌入图片的 md/txt/pdf/docx/pptx 等）：可直接通过 REST 上传
   - **带图片的文档**（含截图的 PDF/Word/PPT，含流程图/架构图/截图的文件）：**必须先用 markitdown-mcp 预处理**，流程如下：

     ```mermaid
     flowchart TD
         A[原始文档] --> B{是否含图片/截图?}
         B -->|是| C[调用 markitdown convert_to_markdown\n参数: uri=文件路径, extract_images=true]
         B -->|否| F[直接上传]
         C --> D[获取转换后的 markdown 文本\n及提取到磁盘的图片文件]
         D --> E[将图片文件一并上传到知识库\n确保 markdown 中图片引用路径正确]
         E --> G[上传 markdown 文件]
         F --> G
         G --> H[调用 h3_coding_hub_kb_document_status\n查询处理进度]
     ```

     **markitdown 预处理工具选择**：
     - `convert_to_markdown(uri, extract_images=true)`：完整转换文档为 markdown，提取图片到磁盘。适用于需要保留完整原格式的文档。
     - `analyze_document(path)`：提取文档骨架 + 图片列表，便于 AI 用视觉能力逐张读取图片内容并理解上下文。适用于需 AI 分析图片内容的场景（如含图表、UI 截图的文档）。

   - **多文档批量预处理**：如果有多个文档文件，逐一调用 markitdown 转换后再批量上传。

3. **执行上传**：
   - 调用 `h3_coding_hub_kb_upload_document` 获取上传端点信息（返回批量上传 URL、支持类型、curl 示例）
   - 上传端点无需认证，直接通过 HTTP Multipart POST 上传：
     ```bash
     curl -X POST http://<host>:8082/api/v1/knowledge/{kbId}/documents/upload \
       -F "files=@/path/to/doc1.md" \
       -F "files=@/path/to/doc2.md"
     ```
   - 支持批量上传，单次最多 20 个文件
   - 上传后服务器异步处理，不会立即返回搜索结果

4. **查询处理进度**：
   - 调用 `h3_coding_hub_kb_document_status(kbId)` 查询集合内所有文档状态
   - 状态流转：`UPLOADING` → `CONVERTING` → `CHUNKING` → `EMBEDDING` → `READY`
   - 也可传 `docId` 查询单个文档状态
   - **等全部文档变为 `READY` 后再进行检索操作**

**支持的文件类型**: md, txt, pdf, docx, pptx, xlsx, py, js, ts, java, go 等常见格式

#### 5.3 检索知识库（语义搜索）

**触发词**: "搜索知识库"、"检索文档"、"查一下知识库中关于 XX 的内容"

默认参数（**rerank 默认开启，expandContext 默认传 1**）：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `kbId` | 必填 | 知识库 ID |
| `query` | 必填 | 搜索关键词（语义搜索，支持自然语言） |
| `topK` | 5 | 返回的相似片段数 |
| `rerank` | **`true`** | 是否启用重排序（**默认开启**，提升结果相关性） |
| `expandContext` | **`1`** | 上下文扩展块数（**默认 1**，返回匹配片段前后的上下文） |

步骤:
1. 确认目标知识库的 `kbId`
2. 调用 `h3_coding_hub_kb_search(kbId, query, topK=5, rerank=true, expandContext=1)`
3. 检查返回结果中的片段内容，确认相关度

> **tip**：如果对检索结果不满意，可以尝试：
> - 调整 `topK` 增加候选范围
> - 调整 `expandContext` 获取更多上下文（设为 0 则只返回精确匹配块）
> - 关闭 `rerank` 可查看原始向量相似度排序（不推荐，通常开启 rerank 效果更好）

#### 5.4 更新知识库配置

**触发词**: "修改知识库"、"更新知识库配置"

步骤:
1. 调用 `h3_coding_hub_kb_update(kbId, ...)`，传入要修改的字段
2. 支持修改：`name`, `description`, `chunkMode`, `chunkSize`, `chunkOverlap`, `rerank`
3. 未传入的字段保持不变（partial update）

> 注意：修改 `chunkMode`/`chunkSize`/`chunkOverlap` 等配置后，已有文档**不会自动重新分块**。如需生效，需重新上传文档。

#### 5.5 删除知识库

**触发词**: "删除知识库"、"移除知识库"

步骤:
1. 确认目标知识库的 `kbId`
2. 调用 `h3_coding_hub_kb_delete(kbId, username, password)`
3. 删除后将移除知识库及其包含的所有文档数据，不可恢复

> 知识库删除的认证机制与工具写入操作相同，需要 `username` + `password`。

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
7. **知识库文档上传也是走 REST，不走 MCP**: `h3_coding_hub_kb_upload_document` 只返回上传端点信息，实际上传文件需用 curl 执行 HTTP Multipart POST
8. **上传后异步处理，不要立即检索**: 文档上传后依次经历 UPLOADING → CONVERTING → CHUNKING → EMBEDDING → READY，必须等全部文档变为 READY 后语义搜索才能返回有效结果
9. **带图片的文档必须预处理**: 含截图/图表/架构图的 PDF、Word、PPT 等文件，直接上传后图片内容会丢失。必须先用 markitdown-mcp 的 `convert_to_markdown(uri, extract_images=true)` 转换为 markdown 再上传
10. **知识库配置修改不影响已有文档**: 修改 `chunkMode`/`chunkSize`/`chunkOverlap` 后，已有文档不会自动重新分块，需重新上传文档
11. **kb_search 参数默认值**: `rerank=true`, `expandContext=1`, 调用时一般无需修改，特殊情况再调整

## 验证

- 发布/更新后，调用 `h3_coding_hub_tool_get` 确认内容已生效
- 上传文件后，调用 `h3_coding_hub_tool_files` 确认文件列表正确
- 删除文件后，再次调用 `h3_coding_hub_tool_files` 确认文件已移除
- 发帖后，调用 `h3_coding_hub_post_get` 确认内容完整
- 创建知识库后，调用 `h3_coding_hub_kb_search` 确认能正常检索
- 上传文档后，调用 `h3_coding_hub_kb_document_status` 确认处理进度和最终状态
- 文档全部 READY 后，调用 `h3_coding_hub_kb_search` 验证搜索结果
