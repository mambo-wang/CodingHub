# API Contracts: CodingHub

**Feature**: 001-ai-tool-square
**Date**: 2026-05-29

## Base URL

```
/api/v1
```

## Common Headers

| Header | Required | Description |
|--------|----------|-------------|
| Content-Type | Yes | `application/json` |
| Accept | Yes | `application/json` |
| Authorization | Conditional | `Bearer <accessToken>` — required for protected endpoints |

## Common Response Structure

All API responses follow this envelope:

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

**Error codes**:

| code | meaning |
|------|---------|
| 200 | Success |
| 400 | Bad Request — invalid input |
| 401 | Unauthorized — missing or invalid token |
| 403 | Forbidden — insufficient permissions |
| 404 | Not Found — resource doesn't exist |
| 409 | Conflict — duplicate resource (e.g., email already registered) |
| 500 | Internal Server Error |

---

## Authentication APIs

### POST /auth/register — User Registration

**Description**: Register a new user account.

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123",
  "username": "张三"
}
```

**Validation**:
- email: valid email format, unique in system
- password: min 8 chars, must contain uppercase, lowercase and digit
- username: 1-100 chars, pattern `^[a-zA-Z0-9\u4e00-\u9fa5_-]+$`

**Success Response** (201 Created):
```json
{
  "code": 201,
  "message": "注册成功",
  "data": {
    "accessToken": "<jwt>",
    "refreshToken": "<jwt>",
    "user": {
      "id": 1,
      "email": "user@example.com",
      "username": "张三"
    }
  }
}
```

**Error Responses**:
- 400: Invalid input (validation errors returned in `message` field)
- 409: Email already registered

---

### POST /auth/login — User Login

**Description**: Authenticate user and return JWT tokens.

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "<jwt>",
    "refreshToken": "<jwt>",
    "user": {
      "id": 1,
      "email": "user@example.com",
      "username": "张三"
    }
  }
}
```

**Error Responses**:
- 400: Missing fields
- 401: Invalid email or password

---

### POST /auth/refresh — Refresh Access Token

**Description**: Exchange a valid refresh token for a new access token.

**Request Header**: `Authorization: Bearer <refreshToken>`

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "<new-jwt>"
  }
}
```

**Error Responses**:
- 401: Refresh token missing, expired, or revoked

---

## Tool APIs (Public — No Auth Required)

### GET /tools — List Tools

**Description**: Get paginated list of tools with optional filtering and search.

**Query Parameters**:

| param | type | required | default | description |
|-------|------|----------|---------|-------------|
| categoryId | long | No | - | Filter by category |
| keyword | string | No | - | Search in tool name (partial match) |
| sortBy | string | No | latest | `latest` or `name` |
| page | int | No | 0 | Page number (0-indexed) |
| size | int | No | 12 | Page size (max 100) |

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "CodeBuddy Assistant",
        "categoryName": "Skill",
        "categoryIcon": "🛠️",
        "uploaderUsername": "张三",
        "createdAt": "2026-05-29T10:00:00"
      }
    ],
    "totalElements": 42,
    "totalPages": 4,
    "page": 0,
    "size": 12
  }
}
```

---

### GET /tools/{id} — Get Tool Detail

**Description**: Get full details of a specific tool including rendered Markdown content.

**Path Parameters**:
- `id` (long): Tool ID

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "CodeBuddy Assistant",
    "categoryName": "Skill",
    "categoryIcon": "🛠️",
    "content": "# CodeBuddy\n\nThis is a **Skill**...",
    "uploaderId": 1,
    "uploaderUsername": "张三",
    "createdAt": "2026-05-29T10:00:00",
    "updatedAt": "2026-05-29T10:00:00"
  }
}
```

**Error Responses**:
- 404: Tool not found or deleted

---

## Tool APIs (Protected — Auth Required)

### POST /tools — Create Tool

**Description**: Upload a new tool. Authenticated users only.

**Request Header**: `Authorization: Bearer <accessToken>`

**Request Body**:
```json
{
  "name": "My Awesome Tool",
  "categoryId": 1,
  "content": "# My Tool\n\nDescription in Markdown..."
}
```

**Validation**:
- name: 1-100 chars, unique per user
- categoryId: must exist in Category table
- content: max 5000 chars

**Success Response** (201 Created):
```json
{
  "code": 201,
  "message": "上传成功",
  "data": {
    "id": 5,
    "name": "My Awesome Tool",
    "categoryName": "Skill",
    "categoryIcon": "🛠️",
    "uploaderUsername": "张三",
    "createdAt": "2026-05-29T11:00:00"
  }
}
```

**Error Responses**:
- 400: Validation error
- 401: Unauthorized
- 409: Tool name already exists for this user

---

### PUT /tools/{id} — Update Tool

**Description**: Update an existing tool. Only the uploader can update.

**Request Header**: `Authorization: Bearer <accessToken>`

**Path Parameters**:
- `id` (long): Tool ID

**Request Body**:
```json
{
  "name": "Updated Tool Name",
  "categoryId": 2,
  "content": "# Updated Content..."
}
```

**Error Responses**:
- 400: Validation error
- 401: Unauthorized
- 403: Not the uploader
- 404: Tool not found

---

### DELETE /tools/{id} — Delete Tool (Soft Delete)

**Description**: Soft-delete a tool. Only the uploader can delete.

**Request Header**: `Authorization: Bearer <accessToken>`

**Path Parameters**:
- `id` (long): Tool ID

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

## User APIs (Protected — Auth Required)

### GET /users/me/tools — Get My Tools

**Description**: Get list of tools uploaded by the current authenticated user.

**Request Header**: `Authorization: Bearer <accessToken>`

**Query Parameters**: Same as GET /tools

**Success Response** (200 OK): Same structure as GET /tools

---

### GET /users/me — Get Current User Info

**Description**: Get profile of the currently authenticated user.

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "username": "张三",
    "createdAt": "2026-05-20T08:00:00",
    "lastLoginAt": "2026-05-29T10:00:00"
  }
}
```

---

## Category APIs (Public)

### GET /categories — List All Categories

**Description**: Get all available tool categories (for filter dropdown).

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "success",
  "data": [
    { "id": 1, "name": "Skill", "icon": "🛠️", "sortOrder": 1 },
    { "id": 2, "name": "MCP", "icon": "🔌", "sortOrder": 2 },
    { "id": 3, "name": "API", "icon": "🌐", "sortOrder": 3 },
    { "id": 4, "name": "Prompt", "icon": "💬", "sortOrder": 4 },
    { "id": 5, "name": "其他", "icon": "📦", "sortOrder": 5 }
  ]
}
```