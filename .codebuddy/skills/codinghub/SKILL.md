---
name: codinghub
description: CodingHub 工具广场操作指南。当用户要求搜索/安装/发布/更新 CodingHub 工具，发帖到论坛，管理知识库，或通过 MCP 与 CodingHub 平台交互时使用。前提是已配置 CodingHub MCP 连接（SSE 模式）。
version: 3.0.0
---

# CodingHub 操作指南

通过 MCP（Model Context Protocol）与 CodingHub 工具广场交互，支持工具发现、安装、发布、更新、论坛交流，以及知识库管理。

## 网站访问

当用户说"打开codinghub网站"、"访问codinghub"时，打开浏览器访问 CodingHub 网站，地址为 CodingHub MCP Server 的 IP 地址。

## MCP 连接与工具概览

- 协议: SSE（Server-Sent Events），入口: `http://<host>:8082/sse`（host 即 MCP Server 地址）
- 工具总数: 17 个（9 只读 + 8 写入）
- MCP 端点无需 JWT（SecurityConfig 已 permitAll），连接时不要传 Authorization header

> **按需加载**: 工具参数速查详见 `references/tool-reference.md`，在需要调用具体工具前读取该文件获取完整参数列表。

## 凭据获取策略

写入操作需要 `username` + `password`（MCP over SSE 无 session/JWT，采用参数级认证）。凭据存储在 skill 目录下的 `config.json` 中：

```json
{
  "username": "your_username",
  "password": "your_password"
}
```

获取凭据时按以下优先级执行：

1. **读 config.json**: 读取 skill 目录下的 `config.json`，如果 `username` 和 `password` 非空则直接使用，不打扰用户
2. **记忆兜底**: 如果 config.json 为空或不存在，用 `memory_search` 搜索 "CodingHub" 凭据作为备选
3. **询问用户**: 以上均无凭据时向用户询问 username 和 password
4. **回写 config.json**: 获取凭据后（无论来自记忆还是用户输入），立即写入 `config.json`，确保下次直接读取。同时用 `memory` 工具（target="user"）保存一份到长期记忆作为跨项目备份

## 核心工作流

### 1. 搜索与安装工具

**触发词**: "查询工具列表"、"安装工具"、"有没有 XX 工具"

步骤:
1. 调用 `h3_coding_hub_tool_search` 按关键词搜索工具
2. 调用 `h3_coding_hub_tool_get` 获取完整文档（含安装说明、使用方法）
3. 调用 `h3_coding_hub_tool_files` 获取文件列表
4. 对每个需要的文件，调用 `h3_coding_hub_tool_download` 获取下载链接
5. 下载链接返回相对路径（如 `/api/v1/tools/{toolId}/files/{fileId}/download`），需拼接服务器基址 `http://<host>:8082` 构成完整 URL
6. 用 curl 下载文件到本地项目目录
7. 把工具版本号写到 skill 文件夹的 `tools.version` 文件中，以便后续升级时对比

**版本检查**: 如果本地已有 skill，先读取其 `tools.version` 中的版本号，与远程对比，仅在版本不同时才下载安装。

### 2. 发布新工具

**触发词**: "发布 skill"、"上传工具到 CodingHub"

步骤:
1. 获取 CodingHub 账号凭据（按「凭据获取策略」执行）
2. 确认 `categoryId`（工具分类 ID），可通过 `h3_coding_hub_tool_search` 查看现有工具的分类来推断
3. 准备工具描述 `content`（markdown 格式），应包含：工具介绍、安装方法、使用示例
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
8. skill 目录只有 SKILL.md 一个文件时直接上传原文；多文件时先 zip 压缩再上传，保留目录结构

**关键**: 文件上传端点 `/api/v1/tools/{toolId}/files` 无需 JWT 认证（SecurityConfig 已放行）。

### 3. 更新已有工具

**触发词**: "更新工具"、"升级 skill 版本"

步骤:
1. 调用 `h3_coding_hub_tool_search` 找到目标工具，获取 `toolId`
2. 调用 `h3_coding_hub_tool_files` 获取当前文件列表
3. 如需替换文件：对每个要删除的文件调用 `h3_coding_hub_tool_file_delete`（readme 可保留）
4. 调用 `h3_coding_hub_tool_modify` 更新工具信息
   - 不传 `version` 时系统自动递增最后一段（`1.0.0` → `1.0.1`）
   - 可手动指定版本号（如 `2.0.0`）
   - 未传入的字段保持不变
5. 用 curl 上传新版本文件（同发布流程第 6 步）

**自动版本号规则**: `1.0.0` → `1.0.1`；`1.0.0-beta` → `1.0.1-beta`；`1.2.3` → `1.2.4`。

### 4. 论坛发帖

**触发词**: "发帖到论坛"、"把这个文档发布到论坛"

步骤:
1. 准备帖子标题和内容（markdown 格式）
2. 确认 `categoryId`（论坛分类 ID）
3. 调用 `h3_coding_hub_post_create`，传入 title、content、categoryId、username、password
4. 如需发布本地 markdown 文件，先读取文件内容作为 content 参数

### 5. 知识库管理

**触发词**: "创建知识库"、"上传文档"、"检索知识库"、"搜索知识库"

> **按需加载**: 知识库的完整操作指南（创建、上传预处理流程含 mermaid 图、语义搜索参数、更新、删除）详见 `references/kb-management.md`，执行知识库操作前先读取该文件。

## 常见陷阱

1. **文件上传走 REST，不走 MCP**: `h3_coding_hub_tool_file_upload` 只返回上传端点信息，实际上传需用 curl 执行 HTTP POST multipart/form-data
2. **下载链接是相对路径**: `h3_coding_hub_tool_download` 返回的 URL 需拼接 `http://<host>:8082` 前缀
3. **MCP 端点无需 JWT**: `/sse` 和 `/mcp/**` 已设为 permitAll，连接时不要传 Authorization header
4. **写操作每次都要凭据**: 没有 session 概念，每个写入调用都是独立认证
5. **modify 的 partial update**: 只更新传入的字段，没传的保持不变；但 version 不传会自动递增
6. **skill 多文件才需要压缩**: 只有 SKILL.md 一个文件时直接上传原文；多文件时才 zip 压缩保留目录结构
7. **知识库文档上传也走 REST**: `h3_coding_hub_kb_upload_document` 只返回端点信息，实际上传用 curl HTTP Multipart POST
8. **上传后异步处理，不要立即检索**: 文档经历 UPLOADING → CONVERTING → CHUNKING → EMBEDDING → READY，必须等全部 READY 后再检索
9. **带图片文档必须预处理**: 含截图/图表的 PDF/Word/PPT 直接上传会丢失图片内容，须先用 markitdown-mcp 预处理（详见 `references/kb-management.md`）
10. **知识库配置修改不影响已有文档**: 修改分块参数后需重新上传文档才能生效
11. **kb_search 参数默认值**: `rerank=true`, `expandContext=1`，调用时一般无需修改

## 验证

- 发布/更新后，调用 `h3_coding_hub_tool_get` 确认内容已生效
- 上传文件后，调用 `h3_coding_hub_tool_files` 确认文件列表正确
- 删除文件后，再次调用 `h3_coding_hub_tool_files` 确认文件已移除
- 发帖后，调用 `h3_coding_hub_post_get` 确认内容完整
- 创建知识库后，上传文档 → 查询状态（`h3_coding_hub_kb_document_status`）→ 全部 READY 后 → `h3_coding_hub_kb_search` 验证检索
