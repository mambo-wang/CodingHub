# 工具 API 契约

**功能**：004-tool-version-management
**版本**：1.0.0
**日期**：2026-06-04
**最后更新**：2026-06-05

## 概述

本文档定义工具管理相关 API 的详细契约，包括版本号字段支持和增强的文件管理功能。

---

## 1. 创建工具

### 请求

```
POST /api/v1/tools
Content-Type: application/json
Authorization: Bearer {token}
```

### 请求体

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 工具名称，1-100字符 |
| categoryId | long | 是 | 分类 ID |
| content | string | 是 | 工具描述，最大5000字符 |
| version | string | 是 | 版本号，格式如 1.0.0 |

### 请求示例

```json
{
  "name": "图像识别工具",
  "categoryId": 1,
  "content": "这是一个强大的图像识别AI工具",
  "version": "1.0.0"
}
```

### 成功响应

```
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
  "code": 201,
  "message": "上传成功",
  "data": {
    "id": 1,
    "name": "图像识别工具",
    "categoryName": "计算机视觉",
    "version": "1.0.0",
    "content": "这是一个强大的图像识别AI工具",
    "uploaderUsername": "admin",
    "viewCount": 0,
    "likeCount": 0,
    "score": 0,
    "createdAt": "2026-06-04T10:00:00"
  }
}
```

### 错误响应

**版本号格式错误（400）**

```json
{
  "code": 400,
  "message": "Bad Request",
  "data": {
    "version": "版本号格式不正确，请使用标准格式（如 1.0.0）"
  }
}
```

**版本号重复（409）**

```json
{
  "code": 409,
  "message": "该分类下已存在同名工具",
  "data": {
    "existingToolId": 123,
    "existingVersion": "1.0.0"
  }
}
```

---

## 2. 更新工具

### 请求

```
PUT /api/v1/tools/{id}
Content-Type: application/json
Authorization: Bearer {token}
```

### 请求体

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 否 | 工具名称，1-100字符 |
| categoryId | long | 否 | 分类 ID |
| content | string | 否 | 工具描述，最大5000字符 |
| version | string | 否 | 版本号，格式如 2.0.0 |

### 请求示例

```json
{
  "name": "图像识别工具",
  "categoryId": 1,
  "content": "更新：支持更多图像格式",
  "version": "2.0.0"
}
```

### 成功响应

```
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": 1,
    "name": "图像识别工具",
    "categoryName": "计算机视觉",
    "version": "2.0.0",
    "content": "更新：支持更多图像格式",
    "uploaderId": 1,
    "uploaderUsername": "admin",
    "viewCount": 100,
    "likeCount": 50,
    "commentCount": 10,
    "score": 450,
    "createdAt": "2026-06-04T10:00:00",
    "updatedAt": "2026-06-04T12:00:00"
  }
}
```

---

## 3. 获取工具详情

### 请求

```
GET /api/v1/tools/{id}
```

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "图像识别工具",
    "categoryName": "计算机视觉",
    "categoryIcon": "👁️",
    "version": "2.0.0",
    "content": "更新：支持更多图像格式",
    "uploaderId": 1,
    "uploaderUsername": "admin",
    "viewCount": 100,
    "likeCount": 50,
    "commentCount": 10,
    "score": 450,
    "createdAt": "2026-06-04T10:00:00",
    "updatedAt": "2026-06-04T12:00:00"
  }
}
```

---

## 4. 获取工具列表

### 请求

```
GET /api/v1/tools?categoryId=1&keyword=图像&sortBy=latest&page=0&size=12
```

### 查询参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| categoryId | long | - | 分类 ID 筛选 |
| keyword | string | - | 关键词搜索（名称/描述） |
| sortBy | string | latest | 排序：latest, popular |
| page | int | 0 | 页码 |
| size | int | 12 | 每页数量 |

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "图像识别工具",
        "categoryName": "计算机视觉",
        "categoryIcon": "👁️",
        "version": "2.0.0",
        "content": "更新：支持更多图像格式",
        "uploaderUsername": "admin",
        "viewCount": 100,
        "likeCount": 50,
        "score": 450,
        "createdAt": "2026-06-04T10:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "currentPage": 0,
    "pageSize": 12
  }
}
```

---

## 5. 上传工具文件

### 请求

```
POST /api/v1/tools/{id}/files
Content-Type: multipart/form-data
Authorization: Bearer {token}
```

### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| files | file[] | 是 | 文件列表，最多10个 |
| readme | string | 否 | README 内容 |

### 同名文件处理

当上传的文件名与已存在文件同名时：
1. 系统自动删除旧文件记录（软删除）
2. 系统自动删除旧物理文件
3. 保存新文件

### 成功响应

```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "toolId": 1,
    "files": [
      {
        "id": 10,
        "toolId": 1,
        "originalName": "manual.pdf",
        "fileSize": 102400,
        "contentType": "application/pdf",
        "createdAt": "2026-06-04T12:00:00"
      }
    ],
    "readmeSaved": true
  }
}
```

---

## 6. 删除工具文件

### 请求

```
DELETE /api/v1/tools/{toolId}/files/{fileId}
Authorization: Bearer {token}
```

### 权限控制

- 仅工具创建者可删除文件
- 管理员可删除任意文件

### 成功响应

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

### 错误响应

**无权限（403）**

```json
{
  "code": 403,
  "message": "无权限删除此文件",
  "data": null
}
```

---

## 7. 错误码定义

| HTTP 状态码 | code | 说明 |
|-------------|------|------|
| 400 | 400 | 请求参数错误 |
| 401 | 401 | 未认证 |
| 403 | 403 | 无权限 |
| 404 | 404 | 资源不存在 |
| 409 | 409 | 版本号冲突 |
| 413 | 413 | 文件大小超限 |
| 415 | 415 | 不支持的文件类型 |
| 500 | 500 | 服务器内部错误 |

---

## 8. MCP Server 工具搜索结果

### 请求（MCP Internal）

MCP Server 调用 `McpSearchService.searchTools()` 获取工具列表

### 响应字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 工具 ID |
| name | string | 工具名称 |
| description | string | 工具描述（截断至100字符） |
| category | string | 分类名称 |
| version | string | **版本号**（新增） |
| createdAt | string | 创建时间 |

### 响应示例

```json
[
  {
    "id": 1,
    "name": "图像识别工具",
    "description": "这是一个强大的图像识别AI工具，支持...",
    "category": "计算机视觉",
    "version": "2.0.0",
    "createdAt": "2026-06-04T10:00:00"
  },
  {
    "id": 2,
    "name": "语音合成工具",
    "description": "支持多种语言的语音合成功能...",
    "category": "语音处理",
    "version": "1.0.0",
    "createdAt": "2026-06-03T15:30:00"
  }
]
```

### 相关代码

- **DTO**: `dto/ToolSearchResult.java`
- **Service**: `service/McpSearchService.java`
