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

<!-- CodeWiki LLM Wiki -->

## CodeWiki LLM Wiki

本项目已使用 [CodeWiki](https://github.com/mambo-wang/CodeWiki-CN) 生成 LLM Wiki 文档，位于 `repowiki/` 目录。

**入口文件：**

- [`repowiki/wiki/overview.md`](repowiki/wiki/overview.md) — 仓库级架构总览（含 Mermaid 架构图）
- [`repowiki/wiki/index.md`](repowiki/wiki/index.md) — 文档目录与知识笔记索引
- [`repowiki/wiki/schema.yaml`](repowiki/wiki/schema.yaml) — 项目文档约定（命名规范、必填章节等）

**模块列表：**

- [工具广场](repowiki/wiki/modules/工具广场.md)
- [用户与认证](repowiki/wiki/modules/用户与认证.md)
- [统一互动](repowiki/wiki/modules/统一互动.md)
- [论坛社区](repowiki/wiki/modules/论坛社区.md)
- [知识库与RAG](repowiki/wiki/modules/知识库与RAG.md)
- [MCP服务](repowiki/wiki/modules/MCP服务.md)
- [前端应用](repowiki/wiki/modules/前端应用.md)

### MCP 工具用法

如果当前 IDE 已配置 CodeWiki MCP 服务器，可直接使用以下工具：

**查询文档和笔记（query_wiki）：**

```json
{
  "query": "如何处理依赖分析",
  "scope": "模块名（可选，限定搜索范围）",
  "include_notes": true,
  "include_code_refs": true,
  "max_results": 10,
  "expand_terms": ["依赖图", "依赖追踪"]
}
```

返回排序后的匹配结果（含上下文片段）和相关组件 ID。在编码、调试或做设计决策时，先查询 wiki 获取相关上下文。

**归档决策/经验教训（ingest_note）：**

```json
{
  "note_type": "decision",
  "title": "选择 SQLite 作为缓存后端",
  "content": "选择原因：...",
  "related_modules": ["模块名"]
}
```

`note_type` 可选值：`decision`（设计决策）、`lesson`（经验教训）、`architecture`（架构说明）、`bug_fix`（Bug 修复记录）、`general`（通用笔记）。笔记存储在 `repowiki/notes/` 目录，可被 `query_wiki` 检索。

**文档一致性检查（lint_wiki）：**

```json
{}
```

检查文档与代码是否一致，包括：过时引用、断链、未文档化组件、循环依赖、覆盖率。

### 使用建议

1. **编码前**：先用 `query_wiki` 搜索相关模块文档，了解架构约定和依赖关系
2. **做决策时**：用 `query_wiki` 搜索已有的 `decision` 类型笔记，避免重复讨论
3. **完成重要决策后**：用 `ingest_note` 归档，让未来的 Agent 和团队成员都能查到
4. **定期维护**：用 `lint_wiki` 检查文档是否过时，保持文档与代码同步

### 纠正识别与经验沉淀

当你被用户纠正、吐槽或补充了未知上下文时，这可能是值得沉淀的经验。按以下规则处理：

**识别纠正信号（满足任一即触发）：**

- 用户明确否定你的输出："不对""你搞错了""不是这样的""应该是…"
- 用户表达重复犯错的不满："又…""上次就…""为什么又…"
- 你修改了自己的输出后用户仍不满意，说明理解有根本偏差
- 用户补充了你不知道的关键上下文："你不知道吗…""这个项目一直都是…""我们约定过…"
- 用户指出方法名/Javadoc 与实际行为不一致，或指出代码中的历史遗留问题

**执行三步流程：**

1. **反思**：明确说出自己错在哪里、正确做法是什么、根因是什么（是缺少项目上下文？还是对代码理解有误？）
2. **起草笔记**：将教训整理为结构化内容，包含：背景（什么场景下犯了错）、正确做法、根因分析
3. **征求确认**：向用户展示笔记草稿，询问"要把这条经验记录到 Wiki 吗？"——**必须得到用户确认后才执行 `ingest_note`**，不要默默保存

**归档示例：**

```json
{
  "note_type": "lesson",
  "title": "OrderService.process() 只做参数校验不做业务处理",
  "content": "## 背景\n\nAgent 误以为 OrderService.process() 包含完整业务逻辑，基于方法名做了错误的设计假设。\n\n## 正确做法\n\nprocess() 仅做入参校验和格式化，实际业务处理在 OrderService.execute() 中。老项目方法名与实际行为不一致是常见情况，应优先阅读实现而非信任方法名。\n\n## 根因\n\n十几年老项目，方法经过多次重构但名称未更新。",
  "related_modules": ["order"]
}
```

**注意**：不是每次纠正都需要沉淀。只记录有复用价值的经验——特定于本次任务的临时调整、用户个人偏好等不需要记录。判断标准：如果未来的 Agent 或新同事遇到同样场景时这条经验有用，就值得记录。

<!-- /CodeWiki LLM Wiki -->
