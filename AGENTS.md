# CodingHub - Agent 导航地图

> 本文档为 AI 代理提供项目结构、入口点、约束规则的快速参考。

## 1. 项目概述

- **项目名称**: CodingHub (ai-tool-square)
- **技术栈**: Java 17 / Spring Boot 3.2.5 (后端) + Vue 3.4 / TypeScript 5.4 / Vite 5.2 (前端)
- **构建工具**: Gradle 8.5 (后端) + npm (前端)
- **数据库**: MySQL 8.x (root/root, 库名: ai_tool_square)
- **端口**: 后端 8082, 前端 5173, MySQL 3306
- **部署模式**: 本地裸机部署，无 Docker/CI

## 2. 项目结构

```
CodingHub/
├── backend/src/main/java/com/iaihub/toolbox/
│   ├── controller/          # REST API (11 核心 + 5 论坛 + 2 微课)
│   ├── service/             # 业务逻辑 (8 核心 + 5 论坛 + 2 微课)
│   ├── repository/          # 数据访问 (7 核心 + 6 论坛 + 4 微课)
│   ├── model/               # 实体 (9 核心 + 7 论坛 + 5 微课)
│   ├── dto/                 # DTO (29 核心 + 7 论坛 + 7 微课)
│   ├── config/              # 配置 (6): Security, JWT, MCP, Upload
│   ├── exception/           # 异常 (9)
│   ├── util/                # 工具 (3): Jwt, Xss, Avatar
│   └── mcp/                 # MCP (4): SDK, 工具处理, SSE
├── frontend/src/
│   ├── pages/               # 页面 (12+2 admin+5 forum+4 video)
│   ├── components/          # 组件 (9+1 common+8 forum+3 video)
│   ├── services(5), stores(3), types(5), composables(1)
│   └── router/              # Vue Router
├── docs/  harness/  specs/  scripts/  Makefile
```

## 3. 模块与层级

### 后端分层 (com.iaihub.toolbox)

| 层级 | 包路径 | 文件数 | 依赖规则 |
|------|--------|--------|----------|
| L0 - 配置/工具 | config(6), util(3), exception(9) | 18 | 可依赖 L1, L2 |
| L1 - 模型 | model(9+7+5), dto(29+7+7) | 64 | 仅 L0 |
| L2 - 数据访问 | repository(7+6+4) | 17 | 仅 L1 |
| L3 - 业务逻辑 | service(8+5+2) | 15 | L0, L1, L2 |
| L4 - API/MCP | controller(11+5+2), mcp(4) | 22 | L1, L3 |

> config/ 注入 repository 是 Spring 标准用法，不算违规。

### 前端分层

| 层级 | 目录 | 文件数 | 依赖规则 |
|------|------|--------|----------|
| L0 - 类型/工具 | types(5), composables(1) | 6 | 无内部依赖 |
| L1 - 服务层 | services(5) | 5 | 仅 L0 |
| L2 - 状态管理 | stores(3) | 3 | L0, L1 |
| L3 - 组件 | components(9+1+8+3) | 21 | L0, L1, L2 |
| L4 - 页面 | pages(12+2+5+4) | 23 | L3 |

### 领域模块

| 模块 | 后端 | 前端 | 说明 |
|------|------|------|------|
| 核心 | controller, service, model | pages, components | 认证、工具 CRUD、分类、文件、点赞、评论 |
| 论坛 | controller/forum, service/forum, model/forum | pages/forum, components/forum | 帖子、评论、分类、标签、点赞、收藏 |
| 微课 | controller/video, service/video, model/video | pages/video, components/video | 视频上传/播放/互动 |
| 管理 | AdminController | pages/admin | 用户审批/管理 |
| MCP | mcp/ | - | Model Context Protocol (11 tools via SSE) |
| 概览 | OverviewController | pages/OverviewPage | 统计/排行 |

## 4. API 入口点 (http://localhost:8082)

| 前缀 | 说明 | 前缀 | 说明 |
|------|------|------|------|
| `/api/v1/auth` | 认证 (login/register/refresh) | `/api/forum/posts` | 论坛帖子 |
| `/api/v1/tools` | 工具 CRUD + 点赞 | `/api/forum/categories` | 论坛分类 |
| `/api/v1/tools/{id}/files` | 文件上传/下载 | `/api/forum/tags` | 论坛标签 |
| `/api/v1/categories` | 工具分类 | `/api/forum/likes` | 论坛点赞 |
| `/api/v1/users` | 用户 (profile/avatar) | `/api/v1/post-favorites` | 帖子收藏 |
| `/api/v1/admin` | 管理 (审批/用户) | `/api/overview` | 统计/排行 |
| `/api/v1/videos` | 微课 CRUD+互动 | `/mcp/sse` | MCP (11 tools, SSE) |

## 5. 数据库表 (ai_tool_square)

- **核心**: user, category, tool, tool_file, tool_like, tool_comment
- **论坛**: forum_category, forum_tag, forum_post, forum_post_tag, forum_comment, forum_like
- **微课**: video, video_comment, video_like, video_favorite
- **其他**: post_favorite

> 完整字段定义见 [ARCHITECTURE.md](docs/ARCHITECTURE.md) 和 Makefile `db` target。

## 6. 约束规则

### 代码约束

- **禁止循环依赖**: 单向依赖 controller -> service -> repository -> model
- **XSS 防护**: 用户输入经 `XssSanitizer.sanitize()`
- **JWT 认证**: `Authorization: Bearer <token>`，15min 过期，refresh 7 天
- **权限**: USER / ADMIN / SUPER_ADMIN，内容操作 `isOwner || isAdmin`
- **禁止 null 返回**: 抛异常或返回 Optional
- **软删除**: `status = DELETED` (Tool / ForumPost / Video)

### Git 约束
- **禁止私自提交**，必须经人工确认；Conventional Commits；单次 <= 1000 行；禁止循环内请求数据库

## 7. 快速命令
```bash
make db              # 创建数据库并初始化表结构
make install         # 安装前端依赖
make backend         # 启动后端 (8082)
make frontend        # 启动前端 (5173)
make run             # 同时启动后端+前端
make stop            # 停止所有服务
make lint            # lint-arch + lint-quality + lint-deps
```

## 8. 相关文档
- [架构详情](docs/ARCHITECTURE.md) - 分层设计、实体关系、请求流程序列图
- [开发指南](docs/DEVELOPMENT.md) - 开发环境搭建与工作流程
- [环境配置](harness/config/environment.json) - 运行时环境变量
