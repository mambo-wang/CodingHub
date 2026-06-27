# CodingHub - Agent 导航地图

> 本文档为 AI 代理提供项目结构、入口点、约束规则的快速参考。

## 1. 项目概述

- **项目名称**: CodingHub (ai-tool-square)
- **技术栈**: Java 17 / Spring Boot 3.2.5 (后端) + Vue 3.4 / TypeScript 5.4 / Vite 5.2 (前端)
- **构建工具**: Gradle 8.5 (后端) + npm (前端)
- **数据库**: MySQL 8.x (root/root, 库名: ai_tool_square) + Flyway 迁移 (9 migrations)
- **端口**: 后端 8082, 前端 5173, MySQL 3306
- **部署模式**: 本地裸机部署，无 Docker/CI
- **设计系统**: 双主题规范 (Cyberpunk Dark / Glassmorphism Light)，见 `design-system/`

## 2. 项目结构

```
CodingHub/
├── backend/src/main/java/com/iaihub/toolbox/
│   ├── controller/          # REST API (11 核心 + 11 子模块)
│   │   ├── forum/           # 论坛模块
│   │   ├── video/           # 微课模块
│   │   ├── feedback/        # 留言反馈
│   │   ├── kb/              # 知识库
│   │   ├── notification/    # 通知
│   │   └── tag/             # 统一标签
│   ├── service/             # 业务逻辑 (11 核心 + 11 子模块)
│   ├── repository/          # 数据访问 (9 核心 + 17 子模块)
│   ├── model/               # 实体 (12 核心 + 23 子模块)
│   ├── dto/                 # DTO (34 核心 + 27 子模块)
│   ├── config/              # 配置 (7): Security, JWT, MCP, Upload, RAG
│   ├── exception/           # 异常 (9)
│   ├── util/                # 工具 (2): Jwt, Xss (Avatar 合并)
│   └── mcp/                 # MCP (4): SDK, 工具处理, SSE (17 tools)
├── backend/src/main/resources/db/migration/  # Flyway 迁移 (V1-V9)
├── frontend/src/
│   ├── pages/               # 页面 (11 核心 + 2 admin + 6 forum + 6 video + 3 knowledge + 1 feedback)
│   ├── components/          # 组件 (7 通用 + 9 common + 7 forum + 4 video + 2 feedback + 5 knowledge)
│   ├── services(9), stores(3), types(7), composables(2)
│   └── router/              # Vue Router
├── design-system/           # 设计系统规范 (双主题)
├── docs/  harness/  specs/  scripts/  Makefile
```

## 3. 模块与层级

### 后端分层 (com.iaihub.toolbox)

| 层级 | 包路径 | 文件数 | 依赖规则 |
|------|--------|--------|----------|
| L0 - 配置/工具 | config(7), util(2), exception(9) | 18 | 可依赖 L1, L2 |
| L1 - 模型 | model(35), dto(61) | 96 | 仅 L0 |
| L2 - 数据访问 | repository(26) | 26 | 仅 L1 |
| L3 - 业务逻辑 | service(22) | 22 | L0, L1, L2 |
| L4 - API/MCP | controller(22), mcp(4) | 26 | L1, L3 |

> config/ 注入 repository 是 Spring 标准用法，不算违规。

### 前端分层

| 层级 | 目录 | 文件数 | 依赖规则 |
|------|------|--------|----------|
| L0 - 类型/工具 | types(7), composables(2) | 9 | 无内部依赖 |
| L1 - 服务层 | services(9) | 9 | 仅 L0 |
| L2 - 状态管理 | stores(3) | 3 | L0, L1 |
| L3 - 组件 | components(7 通用 + 9 common + 7 forum + 4 video + 2 feedback + 5 knowledge) | 34 | L0, L1, L2 |
| L4 - 页面 | pages(11 核心 + 2 admin + 6 forum + 6 video + 3 knowledge + 1 feedback) | 29 | L3 |

### 领域模块

| 模块 | 后端 | 前端 | 说明 |
|------|------|------|------|
| 核心 | controller, service, model | pages, components | 认证、工具 CRUD、分类、文件、互动 |
| 论坛 | controller/forum, service/forum, model/forum | pages/forum, components/forum | 帖子、评论、分类、标签、点赞、收藏 |
| 微课 | controller/video, service/video, model/video | pages/video, components/video | 视频上传/播放/互动/弹幕 |
| 知识库 | controller/kb, service/kb, model/kb | pages/knowledge, components/knowledge | RAG 知识库、文档管理、语义搜索 |
| 留言反馈 | controller/feedback, service/feedback, model/feedback | pages/feedback, components/feedback | 留言板、管理员回复 |
| 通知 | controller/notification, service/notification, model/notification | components/common/NotificationBell | 通知推送、未读计数 |
| 标签 | controller/tag, service/tag, model/tag | components/common/TagBadge/Selector | 统一标签系统 (TOOL/FORUM/VIDEO) |
| 管理 | AdminController | pages/admin | 用户审批/管理 |
| MCP | mcp/ | - | Model Context Protocol (17 tools via SSE) |
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
| `/api/v1/videos` | 微课 CRUD+互动 | `/mcp/sse` | MCP (17 tools, SSE) |
| `/api/v1/interactions` | 统一互动 (点赞/评论/收藏) | `/api/v1/feedback` | 留言反馈 |
| `/api/v1/knowledge` | 知识库 CRUD+搜索 | `/api/v1/notifications` | 通知 (未读/已读) |
| `/api/v1/tags` | 统一标签 (列表/热门) | | |

## 5. 数据库表 (ai_tool_square)

- **核心**: user, category, tool, tool_file, tool_like, tool_comment
- **论坛**: forum_category, forum_tag, forum_post, forum_post_tag, forum_comment, forum_like
- **微课**: video, video_comment, video_like, video_favorite, danmaku
- **知识库**: knowledge_base, kb_document
- **标签**: tag, tool_tag, video_tag
- **通知**: notification
- **留言**: feedback_message
- **其他**: post_favorite

> 完整字段定义见 [ARCHITECTURE.md](docs/ARCHITECTURE.md) 和 Makefile `db` target。

**迁移**: Flyway (9 个迁移文件: V1~V8 + 独立迁移)，位于 `backend/src/main/resources/db/migration/`

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
- [设计系统](design-system/CodingHub/MASTER.md) - 双主题 UI 规范 (Cyberpunk / Glassmorphism)
