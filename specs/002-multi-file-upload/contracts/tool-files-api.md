# Tool Files API Contract

**Date**: 2026-05-29
**Feature**: 工具上传功能优化 - 多文件支持

## Base URL

`/api/v1/tools/{toolId}/files`

## Endpoints

### 1. Upload Files

上传一个或多个文件到指定工具。

**Endpoint**: `POST /api/v1/tools/{toolId}/files`
**Content-Type**: `multipart/form-data`
**Authentication**: Required (Bearer Token)

#### Path Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| toolId | Long | Yes | 工具ID |

#### Form Data

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| files | MultipartFile[] | Yes | 要上传的文件列表 (multiple) |
| readme | String | No | README 内容 (将保存为 readme.md) |

#### Request Example

```
POST /api/v1/tools/123/files
Authorization: Bearer <token>
Content-Type: multipart/form-data

files: [file1.zip, script.py]
readme: # My Tool\n\nThis is a tool for...
```

#### Response

**Success (200)**:
```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": {
    "toolId": 123,
    "files": [
      {
        "id": 1,
        "originalName": "file1.zip",
        "storedPath": "tools/123/file1.zip",
        "fileSize": 1024000,
        "contentType": "application/zip"
      },
      {
        "id": 2,
        "originalName": "script.py",
        "storedPath": "tools/123/script.py",
        "fileSize": 2048,
        "contentType": "text/x-python"
      }
    ],
    "readmeSaved": true
  }
}
```

**Error Responses**:

| Status | Code | Message |
|--------|------|---------|
| 400 | INVALID_FILE_TYPE | 不支持的文件类型 |
| 400 | FILE_SIZE_EXCEEDED | 文件大小超过限制 (50MB) |
| 400 | TOTAL_SIZE_EXCEEDED | 总上传大小超过限制 (200MB) |
| 401 | UNAUTHORIZED | 未授权 |
| 403 | FORBIDDEN | 无权操作此工具 |
| 404 | TOOL_NOT_FOUND | 工具不存在 |

---

### 2. List Tool Files

获取指定工具的所有文件列表。

**Endpoint**: `GET /api/v1/tools/{toolId}/files`
**Authentication**: Required

#### Path Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| toolId | Long | Yes | 工具ID |

#### Response

**Success (200)**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "toolId": 123,
    "folderPath": "tools/123",
    "files": [
      {
        "id": 1,
        "originalName": "file1.zip",
        "storedPath": "tools/123/file1.zip",
        "fileSize": 1024000,
        "contentType": "application/zip",
        "createdAt": "2026-05-29T12:00:00Z"
      }
    ],
    "readmeExists": true
  }
}
```

---

### 3. Download File

下载指定工具的单个文件。

**Endpoint**: `GET /api/v1/tools/{toolId}/files/{fileId}`
**Authentication**: Required (for non-public tools)

#### Path Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| toolId | Long | Yes | 工具ID |
| fileId | Long | Yes | 文件ID |

#### Response

**Success (200)**:
- Content-Type: `application/octet-stream` (或文件的实际类型)
- Content-Disposition: `attachment; filename="originalName.ext"`
- Body: 文件二进制内容

---

### 4. Delete File

删除指定工具的单个文件。

**Endpoint**: `DELETE /api/v1/tools/{toolId}/files/{fileId}`
**Authentication**: Required

#### Path Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| toolId | Long | Yes | 工具ID |
| fileId | Long | Yes | 文件ID |

#### Response

**Success (200)**:
```json
{
  "code": 200,
  "message": "文件删除成功",
  "data": null
}
```

**Error Responses**:

| Status | Code | Message |
|--------|------|---------|
| 401 | UNAUTHORIZED | 未授权 |
| 403 | FORBIDDEN | 无权删除此文件 |
| 404 | FILE_NOT_FOUND | 文件不存在 |

---

### 5. Delete All Tool Files

删除指定工具的所有文件（但不删除工具本身）。

**Endpoint**: `DELETE /api/v1/tools/{toolId}/files`
**Authentication**: Required

#### Path Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| toolId | Long | Yes | 工具ID |

#### Response

**Success (200)**:
```json
{
  "code": 200,
  "message": "所有文件删除成功",
  "data": {
    "deletedCount": 5
  }
}
```

## File Type Allowlist

| Extension | MIME Type |
|-----------|-----------|
| .zip | application/zip |
| .tar | application/x-tar |
| .gz | application/gzip |
| .py | text/x-python |
| .js | text/javascript |
| .ts | text/typescript |
| .md | text/markdown |
| .txt | text/plain |
| .json | application/json |
| .yaml / .yml | text/yaml |
| .toml | application/toml |
| .xml | text/xml |
| .html | text/html |
| .css | text/css |

## Error Response Format

所有错误响应遵循统一格式:

```json
{
  "code": <HTTP_STATUS>,
  "message": "<人类可读的错误描述>",
  "data": null,
  "timestamp": "2026-05-29T12:00:00Z",
  "path": "/api/v1/tools/123/files"
}
```
