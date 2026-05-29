# Data Model: 工具文件管理

**Date**: 2026-05-29
**Feature**: 工具上传功能优化 - 多文件支持

## Entity Relationship

```
┌─────────────┐       ┌─────────────────┐
│    Tool     │       │   ToolFile      │
├─────────────┤       ├─────────────────┤
│ id (PK)    │──┐    │ id (PK)         │
│ name       │  │    │ toolId (FK)     │
│ categoryId │  └───►│ originalName    │
│ content    │       │ storedPath      │
│ uploaderId │       │ fileSize        │
│ status     │       │ contentType     │
│ createdAt  │       │ createdAt       │
│ updatedAt  │       └─────────────────┘
└─────────────┘
```

## Entities

### ToolFile

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | PK, Auto-increment | 文件记录ID |
| toolId | Long | FK, NOT NULL | 关联的工具ID |
| originalName | String(255) | NOT NULL | 用户上传时的原始文件名 |
| storedPath | String(512) | NOT NULL, UNIQUE | 服务器存储路径 (相对于 uploads 根目录) |
| fileSize | Long | NOT NULL, >= 0 | 文件大小 (字节) |
| contentType | String(100) | | 文件 MIME 类型 |
| createdAt | Timestamp | NOT NULL | 上传时间 |

### Tool (修改)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| folderPath | String(512) | | 工具文件夹路径 (可选，预留字段) |

## Validation Rules

### 文件上传

- 单文件大小: <= 50MB
- 总上传大小: <= 200MB
- 允许类型: `.zip, .tar, .gz, .py, .js, .ts, .md, .txt, .json, .yaml, .yml, .toml, .xml, .html, .css`

### ToolFile 业务规则

- 同一工具下文件 originalName 可以重复（用户可能上传同名不同内容文件）
- 文件删除时物理文件一并删除
- 工具删除时，该工具下所有文件一并删除

## State Transitions

### ToolFile States

- **NORMAL**: 文件正常存在
- **DELETED**: 文件被删除（软删除或物理删除）

### Upload Flow

```
[用户选择文件]
      │
      ▼
[前端: 显示文件列表]
      │
      ▼
[用户提交]
      │
      ▼
[创建工具记录] ──► [上传文件到临时目录]
      │                    │
      │                    ▼
      │            [移动到工具文件夹]
      │                    │
      │                    ▼
      │            [创建ToolFile记录]
      │                    │
      ▼                    ▼
[返回成功响应]
```

## Storage Structure

```
uploads/                          # 文件存储根目录 (可配置)
└── tools/                         # 工具文件目录
    └── {toolId}/                  # 每个工具的独立文件夹
        ├── readme.md              # 工具介绍文档 (由 content 字段生成)
        ├── file1.zip              # 用户上传的文件
        └── script.py              # 用户上传的文件
```
