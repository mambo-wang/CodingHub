# AI 工具广场 - 架构文档

## 1. 系统概述

AI 工具广场是一个全栈 Web 应用，提供 AI 工具/资源的管理和展示平台。

### 1.1 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.5 |
| 编程语言 | Java | 17 |
| 前端框架 | Vue 3 | 3.x |
| 构建工具 | Vite | 5.x |
| 数据库 | MySQL | 8.x |
| 认证 | JWT | 0.12.5 |

## 2. 后端架构

### 2.1 分层结构

```
┌─────────────────────────────────────────────────────────┐
│                    Controller Layer                     │
│           (AuthController, ToolController, etc.)        │
├─────────────────────────────────────────────────────────┤
│                     Service Layer                       │
│        (UserService, ToolService, CategoryService)     │
├─────────────────────────────────────────────────────────┤
│                   Repository Layer                      │
│         (JpaRepository<Tool, Long>, etc.)              │
├─────────────────────────────────────────────────────────┤
│                      Model Layer                        │
│            (User, Tool, Category, ToolFile)            │
└─────────────────────────────────────────────────────────┘
```

### 2.2 包结构

```
com.iaihub.toolbox/
├── controller/          # REST API endpoints
│   ├── AuthController.java      # /api/auth/*
│   ├── ToolController.java       # /api/tools/*
│   ├── CategoryController.java   # /api/categories/*
│   ├── UserController.java       # /api/users/*
│   └── ToolFileController.java   # /api/files/*
│
├── service/            # Business logic
│   ├── AuthService.java
│   ├── ToolService.java
│   ├── CategoryService.java
│   ├── ToolFileService.java
│   └── UserService.java
│
├── repository/         # Data access (Spring Data JPA)
│   ├── UserRepository.java
│   ├── ToolRepository.java
│   ├── CategoryRepository.java
│   └── ToolFileRepository.java
│
├── model/               # JPA Entities
│   ├── User.java
│   ├── Tool.java
│   ├── Category.java
│   └── ToolFile.java
│
├── dto/                 # Data Transfer Objects
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── RegisterRequest.java
│   ├── ToolDTO.java
│   └── ...
│
├── config/              # Configuration classes
│   ├── SecurityConfig.java      # Spring Security 配置
│   ├── JwtAuthenticationFilter.java
│   ├── DataInitializer.java      # 数据初始化
│   └── UploadConfig.java
│
├── exception/           # Exception handling
│   ├── GlobalExceptionHandler.java
│   ├── BusinessException.java
│   ├── ResourceNotFoundException.java
│   ├── UnauthorizedException.java
│   ├── ForbiddenException.java
│   └── DuplicateResourceException.java
│
└── util/                # Utilities
    ├── JwtUtil.java            # JWT token 生成/验证
    └── XssSanitizer.java      # XSS 过滤
```

### 2.3 依赖规则 (层级隔离)

| 层级 | 包 | 可依赖 |
|------|-----|--------|
| L0 | config/, util/ | 外部库 |
| L1 | model/, dto/ | L0 |
| L2 | repository/ | L1 |
| L3 | service/ | L0, L1, L2 |
| L4 | controller/ | L1, L3 |

### 2.4 核心实体关系

```
User (1) ──────────< Tool (N)
  │                     │
  │                     │
  └──── (上传者)         │
                         │
Category (1) ───────< Tool (N)
                         │
                         │
                    ToolFile (N)
```

## 3. 前端架构

### 3.1 目录结构

```
frontend/src/
├── components/          # 可复用 Vue 组件
│   ├── Header.vue
│   ├── ToolCard.vue
│   └── ...
├── pages/               # 页面组件
│   ├── HomePage.vue
│   ├── LoginPage.vue
│   ├── RegisterPage.vue
│   └── ToolDetailPage.vue
├── services/           # API 调用层
│   ├── auth.ts
│   ├── tools.ts
│   └── categories.ts
├── stores/              # Pinia 状态管理
│   ├── auth.ts
│   └── tools.ts
├── router/              # Vue Router 配置
│   └── index.ts
├── types/               # TypeScript 类型定义
│   └── index.ts
├── utils/               # 工具函数
│   └── index.ts
├── assets/              # 静态资源
├── App.vue              # 根组件
└── main.ts              # 入口文件
```

### 3.2 前端分层

```
┌─────────────────────────────────────────┐
│              Pages (L4)                 │
│         HomePage, LoginPage, etc.       │
├─────────────────────────────────────────┤
│           Components (L3)               │
│        ToolCard, Header, etc.           │
├─────────────────────────────────────────┤
│             Stores (L2)                 │
│           auth store, tools store      │
├─────────────────────────────────────────┤
│            Services (L1)                │
│         auth.ts, tools.ts, etc.        │
├─────────────────────────────────────────┤
│    Types / Utils / Assets (L0)          │
└─────────────────────────────────────────┘
```

## 4. API 设计

### 4.1 认证相关

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/auth/register | 用户注册 | 否 |
| POST | /api/auth/login | 用户登录 | 否 |
| GET | /api/auth/me | 获取当前用户 | 是 |

### 4.2 工具相关

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/tools | 工具列表 (分页) | 否 |
| GET | /api/tools/{id} | 工具详情 | 否 |
| POST | /api/tools | 创建工具 | 是 |
| PUT | /api/tools/{id} | 更新工具 | 是 |
| DELETE | /api/tools/{id} | 删除工具 | 是 |

### 4.3 分类相关

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/categories | 分类列表 | 否 |

### 4.4 文件上传

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/files/upload | 上传文件 | 是 |
| GET | /api/files/{id} | 下载文件 | 是 |

## 5. 安全机制

### 5.1 JWT 认证流程

1. 用户登录 → 服务端验证 → 生成 JWT token
2. 客户端存储 token (localStorage)
3. 请求时在 Header 携带: `Authorization: Bearer <token>`
4. JwtAuthenticationFilter 验证 token → 设置 Security Context

### 5.2 XSS 防护

所有用户输入内容通过 `XssSanitizer.sanitize()` 过滤，防止 XSS 攻击。

### 5.3 密码存储

使用 BCrypt 哈希存储密码，不可逆。

## 6. 数据库设计

### 6.1 表结构

```sql
-- 用户表
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at DATETIME,
    INDEX idx_user_email (email)
);

-- 分类表
CREATE TABLE category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    icon VARCHAR(255),
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 工具表
CREATE TABLE tool (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    uploader_id BIGINT NOT NULL,
    status ENUM('NORMAL', 'DELETED') NOT NULL DEFAULT 'NORMAL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES category(id),
    FOREIGN KEY (uploader_id) REFERENCES user(id),
    INDEX idx_tool_category (category_id, status),
    INDEX idx_tool_uploader (uploader_id, status),
    UNIQUE INDEX uk_tool_uploader_name (uploader_id, name, status)
);

-- 工具文件表
CREATE TABLE tool_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tool_id BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tool_id) REFERENCES tool(id)
);
```

## 7. 部署架构

### 7.1 开发环境

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Browser   │────▶│   Vite      │     │   MySQL     │
│             │     │   :5173     │     │   :3306     │
└─────────────┘     └──────┬──────┘     └─────────────┘
                          │
                    ┌─────▼─────┐
                    │  Spring   │
                    │  Boot     │
                    │  :8080    │
                    └───────────┘
```

### 7.2 端口配置

| 服务 | 端口 | 说明 |
|------|------|------|
| 后端 | 8080 | Spring Boot 应用 |
| 前端 | 5173 | Vite 开发服务器 |
| MySQL | 3306 | 数据库服务 |

---

**最后更新**: 2026-05-29