# Data Model: AI 工具广场

**Feature**: 001-ai-tool-square
**Date**: 2026-05-29

## Entity Relationship Diagram (Conceptual)

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│    User      │       │     Tool      │       │   Category   │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id (PK)      │──┐    │ id (PK)      │       │ id (PK)      │
│ email        │  │    │ name         │       │ name         │
│ password     │  └───►│ categoryId(FK)│       │ icon         │
│ username     │       │ content      │       │ sortOrder    │
│ createdAt    │       │ uploaderId(FK)│      │ createdAt    │
│ updatedAt    │       │ status       │       │ updatedAt    │
└──────────────┘       │ createdAt    │       └──────────────┘
                       │ updatedAt    │
                       └──────────────┘
```

## Entity Definitions

### 1. User（用户）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | Long | PK, AUTO_INCREMENT | 用户唯一标识 |
| email | String(255) | UNIQUE, NOT NULL | 登录邮箱，唯一索引 |
| password | String(255) | NOT NULL | BCrypt 加密后的密码 hash |
| username | String(100) | NOT NULL | 显示名称，1-100字符 |
| createdAt | LocalDateTime | NOT NULL | 注册时间 |
| updatedAt | LocalDateTime | NOT NULL | 最后更新时间 |
| lastLoginAt | LocalDateTime | NULLABLE | 最后登录时间 |

**Validation**:
- email: 符合 RFC 5322 邮箱格式
- password: 最短 8 字符，含大小写字母和数字
- username: 1-100 字符，字母/数字/中文/常用符号

**State**: 无状态机，删除通过软删（设 status=DELETED）

---

### 2. Tool（工具）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | Long | PK, AUTO_INCREMENT | 工具唯一标识 |
| name | String(100) | NOT NULL, INDEX | 工具名称，1-100字符 |
| categoryId | Long | FK → Category.id, NOT NULL | 所属分类 |
| content | TEXT | NOT NULL | Markdown 格式的介绍内容，最大 5000 字符 |
| uploaderId | Long | FK → User.id, NOT NULL | 上传者用户 ID |
| status | Enum | NOT NULL, DEFAULT NORMAL | 状态：NORMAL / DELETED |
| createdAt | LocalDateTime | NOT NULL | 上传时间 |
| updatedAt | LocalDateTime | NOT NULL | 最后更新时间 |

**Validation**:
- name: 1-100 字符，字母/数字/中文/常用符号，与 uploaderId 联合唯一（同一用户不能上传同名工具）
- content: 最大 5000 字符，存储原始 Markdown 文本

**Indexes**:
- `idx_tool_category` on (categoryId, status)
- `idx_tool_uploader` on (uploaderId, status)
- `idx_tool_name_status` on (name, status) — 用于搜索和重复检查
- `uk_tool_uploader_name` on (uploaderId, name, status) — 联合唯一

**State Transitions**:
- NORMAL → DELETED (软删除，更新 status 和 updatedAt)

---

### 3. Category（分类）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | Long | PK, AUTO_INCREMENT | 分类唯一标识 |
| name | String(50) | UNIQUE, NOT NULL | 分类名称：Skill / MCP / API / Prompt / 其他 |
| icon | String(255) | NULLABLE | 图标 URL 或 emoji |
| sortOrder | Integer | NOT NULL, DEFAULT 0 | 排序权重，数字越小越靠前 |
| createdAt | LocalDateTime | NOT NULL | 创建时间 |

**Pre-seeded Data** (启动时初始化):

| name | icon | sortOrder |
|------|------|-----------|
| Skill | 🛠️ | 1 |
| MCP | 🔌 | 2 |
| API | 🌐 | 3 |
| Prompt | 💬 | 4 |
| 其他 | 📦 | 5 |

---

## API Request/Response DTOs

### Auth DTOs

```
RegisterRequest  { email, password, username }
LoginRequest     { email, password }
LoginResponse    { accessToken, refreshToken, user: { id, email, username } }
RefreshResponse  { accessToken }
```

### Tool DTOs

```
ToolSummaryDTO   { id, name, categoryName, categoryIcon, uploaderUsername, createdAt }
ToolDetailDTO    { id, name, categoryName, categoryIcon, content, uploaderUsername, uploaderId, createdAt, updatedAt }
CreateToolRequest { name, categoryId, content }
UpdateToolRequest { name, categoryId, content }
PagedToolQuery   { categoryId?, keyword?, sortBy?, page, size }
                  sortBy: "latest" (default) | "name"
PageResponse<T>  { content: T[], totalElements, totalPages, page, size }
```

### Error DTOs

```
ErrorResponse    { code: int, message: string, timestamp: string }
```

---

## Database Schema (MySQL DDL)

```sql
CREATE DATABASE IF NOT EXISTS ai_tool_square CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_tool_square;

CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at DATETIME NULL,
    INDEX idx_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    icon VARCHAR(255) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    uploader_id BIGINT NOT NULL,
    status ENUM('NORMAL', 'DELETED') NOT NULL DEFAULT 'NORMAL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tool_category (category_id, status),
    INDEX idx_tool_uploader (uploader_id, status),
    INDEX idx_tool_name_status (name, status),
    UNIQUE INDEX uk_tool_uploader_name (uploader_id, name, status),
    CONSTRAINT fk_tool_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT fk_tool_uploader FOREIGN KEY (uploader_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Pre-seed categories
INSERT INTO category (name, icon, sort_order) VALUES
    ('Skill', '🛠️', 1),
    ('MCP', '🔌', 2),
    ('API', '🌐', 3),
    ('Prompt', '💬', 4),
    ('其他', '📦', 5);
```