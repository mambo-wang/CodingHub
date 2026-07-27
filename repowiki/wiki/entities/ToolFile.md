---
title: ToolFile 工具附件
type: entity
---

# ToolFile 工具附件

## 定义

ToolFile 是工具广场的附件实体，支持任意格式文件上传，与 [Tool](Tool.md) 形成一对多关系。

## 代码位置

- 实体: `backend/src/main/java/com/iaihub/toolbox/model/ToolFile.java`
- 服务: `backend/src/main/java/com/iaihub/toolbox/service/FileService.java`
- 存储: 本地文件系统 `uploads/tools/`

## 关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| tool | Tool | 所属工具 |
| fileName | String | 原始文件名 |
| filePath | String | 存储路径 |
| fileSize | Long | 文件大小（字节） |
| contentType | String | MIME 类型 |
| downloadCount | Integer | 下载次数 |

## 核心行为

- **格式开放**: 不限制文件类型（设计决策：tool-file-format-open）
- **大小限制**: 单文件 50MB
- **下载计数**: 每次下载 +1
- **MCP 暴露**: [McpServer](McpServer.md) 的 tool_files / tool_download 工具
- **批量上传**: [[async-batch-upload]] 支持并发上传

## 关联实体

[Tool](Tool.md) · [McpServer](McpServer.md)

## 设计决策来源

- tool-file-format-open (2026-06-07)
- async-batch-upload (2026-07-01)
