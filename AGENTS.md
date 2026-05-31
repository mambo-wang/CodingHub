# AI 工具广场 - Agent 导航地图

> 本文档为 AI 代理提供项目结构、入口点、约束规则的快速参考。

## 1. 项目概述

- **项目名称**: AI 工具广场 (AI Tool Square)
- **技术栈**: Java 17 / Spring Boot 3.2.5 (后端) + Vue 3 / TypeScript / Vite (前端)
- **构建工具**: Gradle (后端) + npm (前端)
- **数据库**: MySQL 8.x (用户: root, 密码: root)
- **端口**: 后端 8080, 前端 5173

## 2. 项目结构

```
iaihub/
├── backend/                    # Java Spring Boot 后端
│   └── src/main/java/com/iaihub/toolbox/
│       ├── controller/        # REST API 控制器
│       ├── service/           # 业务逻辑层
│       ├── repository/        # 数据访问层 (JPA)
│       ├── model/             # 实体类
│       ├── dto/               # 数据传输对象
│       ├── config/            # 配置类 (Security, JWT, Upload)
│       ├── exception/         # 异常处理
│       └── util/              # 工具类
├── frontend/                  # Vue 3 + TypeScript 前端
│   └── src/
│       ├── components/        # Vue 组件
│       ├── pages/             # 页面
│       ├── services/          # API 调用
│       ├── stores/            # 状态管理
│       ├── router/            # 路由配置
│       └── types/             # TypeScript 类型定义
├── docs/                      # 详细文档
│   └── ARCHITECTURE.md       # 架构详情
├── harness/                   # Agent 基础设施
│   └── config/                # 环境配置
├── specs/                     # 功能规格说明
└── Makefile                   # 快速命令
```

## 3. 模块依赖关系

```
controller → service → repository → model
              ↓
           config (Security, JWT)
              ↓
           util (JwtUtil, XssSanitizer)
```

### 后端分层

| 层级 | 包路径 | 依赖规则 |
|------|--------|----------|
| L0 - 配置/工具 | config/, util/ | 可依赖 L1, L2；禁止依赖 L3, L4 |
| L1 - 模型 | model/, dto/ | 仅 L0 |
| L2 - 数据访问 | repository/ | 仅 L1 |
| L3 - 业务逻辑 | service/ | L0, L1, L2 |
| L4 - API 控制器 | controller/ | L1, L3 |

> **注意**: config/ 包可以注入 repository，这是 Spring 标准用法，不算违规。

### 前端分层

| 层级 | 目录 | 依赖规则 |
|------|------|----------|
| L0 - 类型/工具 | types/, utils/ | 无内部依赖 |
| L1 - 服务层 | services/ | 仅 L0 |
| L2 - 状态管理 | stores/ | L0, L1 |
| L3 - 组件 | components/ | L0, L1, L2 |
| L4 - 页面 | pages/ | L3 |

## 4. API 入口点

### 后端 API (http://localhost:8080)

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/auth/register` | POST | 用户注册 |
| `/api/auth/login` | POST | 用户登录 |
| `/api/tools` | GET/POST | 工具列表/创建 |
| `/api/tools/{id}` | GET/PUT/DELETE | 工具 CRUD |
| `/api/categories` | GET | 分类列表 |
| `/api/files/upload` | POST | 文件上传 |

### 前端页面 (http://localhost:5173)

| 路径 | 说明 |
|------|------|
| `/` | 首页/工具列表 |
| `/login` | 登录页 |
| `/register` | 注册页 |
| `/tools/{id}` | 工具详情 |
| `/profile` | 用户中心 |

## 5. 数据库表结构

- **user**: id, email, password, username, created_at, updated_at, last_login_at
- **category**: id, name, icon, sort_order, created_at
- **tool**: id, name, category_id, content, uploader_id, status, created_at, updated_at
- **tool_file**: id, tool_id, file_path, file_name, file_size, created_at

## 6. 约束规则

### 代码约束

1. **禁止循环依赖**: controller → service → repository → model 是单向依赖，禁止反向
2. **XSS 防护**: 所有用户输入通过 `XssSanitizer.sanitize()` 过滤
3. **JWT 认证**: 需要认证的接口在 Header 携带 `Authorization: Bearer <token>`
4. **禁止 null 返回**: 方法不返回 null，抛异常或返回 Optional

### Git 约束

1. **禁止私自提交**: 需求开发过程中不得私自提交代码，提交代码必须经过人工确认
2. 提交信息遵循 Conventional Commits 格式
3. 单次提交不超过 1000 行更改（超出则提示分批）
4. 禁止在循环中请求数据库或调用接口

## 7. 快速命令

```bash
make db        # 初始化数据库
make install   # 安装前端依赖
make backend   # 启动后端 (8080)
make frontend  # 启动前端 (5173)
make run       # 同时启动后端+前端
make stop      # 停止所有服务
```

## 8. 相关文档

- [架构详情](docs/ARCHITECTURE.md) - 详细架构说明
- [环境配置](harness/config/environment.json) - 运行时环境变量

---

**最后更新**: 2026-05-29