# Research: 多文件上传功能

**Date**: 2026-05-29
**Feature**: 工具上传功能优化 - 多文件支持

## 技术选型

### 后端文件上传方案

**Decision**: 使用本地文件系统存储

**Rationale**:
- 工具文件为中小型文件（代码、文档等），本地存储简单高效
- Spring Boot 内置 `MultipartFile` 支持，无需额外依赖
- 可扩展性强，后续可迁移至 OSS/S3

**Alternatives considered**:
- 云存储 (OSS/S3): 复杂度高，当前需求不需要
- 数据库 BLOB: 不适合大文件，影响数据库性能

### 前端多文件上传组件

**Decision**: 使用 Element Plus 的 el-upload 组件

**Rationale**:
- 项目已使用 Element Plus UI 库，保持一致性
- el-upload 支持多文件上传、文件列表展示、进度显示
- 与现有表单集成方便

### 文件夹命名策略

**Decision**: 使用工具ID作为文件夹名称

**Rationale**:
- 工具ID唯一，保证文件夹名称不冲突
- 便于后续通过工具ID快速定位文件
- 符合 RESTful 资源管理理念

### README 存储格式

**Decision**: 存储为 `readme.md` 文件

**Rationale**:
- Markdown 是项目已使用的格式
- 文件名统一便于程序自动识别
- 符合 GitHub 生态惯例

## 实现方案

### API 设计

1. **文件上传接口**:
   - `POST /api/v1/tools/{toolId}/files`
   - 支持 multipart/form-data
   - 支持多文件上传

2. **获取工具文件列表**:
   - `GET /api/v1/tools/{toolId}/files`
   - 返回文件元数据列表

3. **删除工具文件**:
   - `DELETE /api/v1/tools/{toolId}/files/{fileId}`

### 存储结构

```
uploads/
└── tools/
    └── {toolId}/
        ├── readme.md
        ├── file1.zip
        └── file2.py
```

### 前端实现

1. 在 UploadPage.vue 添加文件上传区域
2. 使用 el-upload 组件的 multiple 属性支持多文件
3. 显示已选择文件列表，支持移除文件
4. 提交时先创建工具记录，再上传文件

### 关键约束

- 单文件大小限制: 50MB
- 总上传大小限制: 200MB
- 允许文件类型: .zip, .tar, .gz, .py, .js, .ts, .md, .txt, .json, .yaml, .yml, .toml, .xml, .html, .css
