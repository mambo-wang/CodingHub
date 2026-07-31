# CodingHub - Agent 导航地图

> AI 代理快速参考：项目结构、入口点、约束规则。详情见链接文档。

## 1. 项目概述

- **项目名称**: CodingHub (ai-tool-square)
- **技术栈**: Java 17 / Spring Boot 3.2.5 + Vue 3.4 / TypeScript 5.4 / Vite 5.2
- **构建工具**: Gradle 8.5 + npm · **数据库**: MySQL 8.x / PostgreSQL 双库共存（配置切换，默认 MySQL）。`ai_tool_square` 库；Schema 由 Hibernate(`ddl-auto: update`) 按激活 Profile(mysql/postgresql) 生成，方言自动探测。初始化：`make db`(MySQL) / `make db-pg`(建库)+`make db-pg-seed`(种子)
- **端口**: 后端 8082, 前端 5173, MySQL 3306 · **部署**: 本地裸机，无 Docker/CI
- **设计系统**: 双主题 (Cyberpunk Dark / Glassmorphism Light)，见 `design-system/`

## 2. 项目结构

```
CodingHub/
├── backend/src/main/java/com/iaihub/toolbox/
│   ├── controller/    # REST API (22: 11核心 + forum,video,feedback,kb,notification,tag)
│   ├── service/       # 业务逻辑 (22)     ├── repository/   # 数据访问 (26)
│   ├── model/         # 实体 (35)          ├── dto/          # DTO (61)
│   ├── config/        # 配置 (7): Security,JWT,MCP,Upload,RAG
│   ├── exception/     # 异常 (9)           ├── util/         # 工具 (2): Jwt,Xss
│   └── mcp/           # MCP SDK + 工具处理 + Streamable HTTP/SSE (18 tools)
├── frontend/src/
│   ├── pages/ (28)    ├── components/ (36: 7通用+9common+7forum+4video+7knowledge+2feedback)
│   ├── services/ (9)  ├── stores/ (3)     ├── types/ (7)     └── composables/ (2)
├── rag/               # RAG 知识库 Python 服务 (MCP + REST API)
├── design-system/  docs/  harness/  specs/  scripts/  openspec/  Makefile
```

## 3. 模块与层级

### 后端分层

| 层级 | 包路径 | 文件数 | 依赖规则 |
|------|--------|--------|----------|
| L0 | config(7), util(2), exception(9) | 18 | 可依赖 L1, L2 |
| L1 | model(35), dto(61) | 96 | 仅 L0 |
| L2 | repository(26) | 26 | 仅 L1 |
| L3 | service(22) | 22 | L0, L1, L2 |
| L4 | controller(22), mcp(4) | 26 | L1, L3 |

> config/ 注入 repository 是 Spring Security 标准用法，不算违规。

### 前端分层

| 层级 | 目录 | 文件数 | 依赖规则 |
|------|------|--------|----------|
| L0 | types(7), composables(2) | 9 | 无内部依赖 |
| L1 | services(9) | 9 | 仅 L0 |
| L2 | stores(3) | 3 | L0, L1 |
| L3 | components(36) | 36 | L0, L1, L2 |
| L4 | pages(28) | 28 | L3 |

### 领域模块

| 模块 | 后端子包 | 前端子目录 | 说明 |
|------|---------|-----------|------|
| 核心 | controller,service,model | pages,components | 认证、工具CRUD、分类、文件、互动 |
| 论坛 | controller/forum,service/forum | pages/forum,components/forum | 帖子、评论、标签、点赞、收藏 |
| 微课 | controller/video,service/video | pages/video,components/video | 视频上传/播放/互动/弹幕 |
| 知识库 | controller/kb,service/kb | pages/knowledge,components/knowledge | RAG知识库、文档管理、语义搜索 |
| 留言反馈 | controller/feedback | pages/feedback,components/feedback | 留言板、管理员回复 |
| 通知 | controller/notification | NotificationBell | 推送、未读计数 |
| 标签 | controller/tag,service/tag | TagBadge,TagSelector | 统一标签(TOOL/FORUM/VIDEO) |
| 管理 | AdminController | pages/admin | 用户审批/管理 |
| MCP | mcp/ | - | 18 tools via Streamable HTTP/SSE |
| 概览 | OverviewController | OverviewPage | 统计/排行 |
| RAG | - | - | rag/ Python服务(MCP+REST) |

## 4. API 入口点 (http://localhost:8082)

| 前缀 | 说明 | 前缀 | 说明 |
|------|------|------|------|
| `/api/v1/auth` | 认证 | `/api/forum/posts` | 论坛帖子 |
| `/api/v1/tools` | 工具CRUD+点赞 | `/api/forum/categories` | 论坛分类 |
| `/api/v1/categories` | 工具分类 | `/api/v1/post-favorites` | 帖子收藏 |
| `/api/v1/users` | 用户(profile/avatar) | `/api/overview` | 统计/排行 |
| `/api/v1/admin` | 管理(审批/用户) | `/mcp` | MCP(18 tools, Streamable HTTP/SSE) |
| `/api/v1/videos` | 微课 | `/api/v1/feedback` | 留言反馈 |
| `/api/v1/interactions` | 统一互动 | `/api/v1/notifications` | 通知 |
| `/api/v1/knowledge` | 知识库 | `/api/v1/tags` | 统一标签 |

## 5. 数据库表 (ai_tool_square)

- **核心**: user, category, tool, tool_file, tool_like, tool_comment
- **论坛**: forum_category, forum_tag, forum_post, forum_post_tag, forum_comment, forum_like · **微课**: video, video_comment, video_like, video_favorite, danmaku
- **知识库**: knowledge_base, kb_document · **标签**: tag, tool_tag, video_tag · **通知**: notification · **留言**: feedback_message · **其他**: post_favorite

> 完整字段定义见 [ARCHITECTURE.md](docs/ARCHITECTURE.md)。迁移: Flyway V1~V9 (`backend/src/main/resources/db/migration/`)

## 6. 约束规则

- **禁止循环依赖**: 单向依赖 controller → service → repository → model
- **XSS 防护**: `XssSanitizer.sanitize()`
- **JWT 认证**: `Authorization: Bearer <token>`，15min 过期，refresh 7 天
- **权限**: USER / ADMIN / SUPER_ADMIN，内容操作 `isOwner || isAdmin`
- **禁止 null 返回**: 抛异常或返回 Optional
- **软删除**: `status = DELETED` (Tool / ForumPost / Video)
- **Git**: 禁止私自提交，须人工确认；Conventional Commits；单次 ≤ 1000 行

## 7. 快速命令

```bash
make db          # 创建数据库并初始化
make install     # 安装前端依赖
make backend     # 启动后端 (8082)
make frontend    # 启动前端 (5173)
make run         # 同时启动后端+前端
make stop        # 停止所有服务
make lint        # lint-arch + lint-quality + lint-deps
```

## 8. 相关文档
- [架构详情](docs/ARCHITECTURE.md) — 分层设计、实体关系、API设计、安全机制
- [开发指南](docs/DEVELOPMENT.md) — 环境搭建、开发命令、问题排查
- [环境配置](harness/config/environment.json) — 运行时环境变量
- [设计系统](design-system/CodingHub/MASTER.md) — 双主题 UI 规范
- [RAG 服务](rag/README_CN.md) — RAG 知识库 Python MCP 服务

## Agent skills

### Issue tracker

Issues live in GitHub Issues for this repo. Use the `gh` CLI for all operations. See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical triage roles mapped to default label strings (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout: one `CONTEXT.md` plus `docs/adr/` at the repo root. See `docs/agents/domain.md`.
