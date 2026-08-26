# CodingHub 使用指导

> AI 工具发现与体验分享平台 — 技术架构、功能详解与快速上手指南

---

## 目录

- [一、项目概述](#一项目概述)
- [二、技术栈总览](#二技术栈总览)
- [三、系统架构](#三系统架构)
- [四、功能模块详解](#四功能模块详解)
- [五、MCP 协议集成](#五mcp-协议集成)
- [六、安全与权限](#六安全与权限)
- [七、快速开始](#七快速开始)
- [八、部署架构](#八部署架构)
- [九、API 端点一览](#九api-端点一览)

---

## 一、项目概述

### 1.1 什么是 CodingHub

CodingHub 是一个面向企业内网的 **AI 工具发现与体验分享平台**，可以理解为 "AI 工具的 App Store"。

**核心价值：**

- 开发者发布 Skills、MCP Server、Prompt 模板等 AI 工具
- 其他用户的 AI 助手通过 MCP 协议自动发现、搜索并一键安装
- 社区论坛分享使用经验，微课视频教学，知识库沉淀技术文档

### 1.2 解决的痛点

| 痛点 | 现状 | CodingHub 方案 |
|------|------|---------------|
| 发现难 | AI 工具散落在 GitHub、博客、群聊中 | 统一目录 + 分类标签 + 搜索 |
| 安装繁 | 手动下载、解压、配置 | MCP 一键发现安装 |
| 经验缺 | 工具用法靠口口相传 | 论坛 + 微课 + 知识库 |
| 内网限 | 企业网络无法访问外网服务 | 仅需 MySQL + JDK，无外部依赖 |

### 1.3 关键数据

| 指标 | 数量 |
|------|------|
| 数据库表 | 25+ |
| MCP 工具 | 18 |
| 前端页面 | 29 |
| 功能模块 | 10+ |

---

## 二、技术栈总览

### 2.1 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 (LTS) | 编程语言，支持 Records + Sealed Classes |
| Spring Boot | 3.2.5 | 应用框架，自动配置 + Actuator 监控 |
| Spring Security | - | JWT 认证 + 三级角色权限 (USER/ADMIN/SUPER_ADMIN) |
| Spring Data JPA | - | ORM 数据访问，仓库模式 |
| MCP SDK | 2.0.0 | Model Context Protocol，Streamable HTTP 传输 |
| Flyway | 9 | 数据库版本迁移 (V1~V9) |
| MySQL | 8.x | InnoDB 存储引擎 + 全文索引 |
| Gradle | 8.5 | 构建自动化 |
| JJWT | 0.12.5 | JWT Token 生成与验证 |

### 2.2 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.21 | 组合式 API + `<script setup>` |
| TypeScript | 5.4.5 | 全量类型安全 |
| Vite | 5.2.8 | 毫秒级 HMR 热更新 |
| Pinia | 2.1.7 | 轻量状态管理 (auth/forum/theme) |
| Element Plus | 2.7 | 企业级 UI 组件库 |
| markdown-it | 14.1 | Markdown 渲染 + 代码高亮 (highlight.js) |
| Mermaid | 11.15 | 流程图 / 架构图渲染 |
| Lucide Vue | - | 一致的图标体系 |

### 2.3 设计系统

**双主题规范：**

| 主题 | 背景色 | 主色调 | 特点 |
|------|--------|--------|------|
| Cyberpunk Dark (默认) | `#09090b` 深黑 | 紫 `#8b5cf6` + 青 `#06b6d4` + 粉 `#ec4899` | 玻璃拟态 + 渐变光晕 + 扫描线动画 |
| Glassmorphism Light | `#f8fafc` 浅灰 | 紫 `#7c3aed` + 青 `#0891b2` + 玫红 `#db2777` | 半透明卡片 + 柔和阴影 |

**字体：** Sora (标题/正文) + Space Mono (代码/统计)

详细设计规范请参阅 [design-system/CodingHub/MASTER.md](../design-system/CodingHub/MASTER.md)。

---

## 三、系统架构

### 3.1 端到端拓扑

```
  Browser (Vue 3 SPA)
       |
       | HTTP :5173 (dev) / :80 (prod)
       v
    Nginx (反向代理)
       |
       | /api/ /mcp/ -> :8082
       | /rag/ -> :8000
       | 静态资源 -> dist/
       v
  Spring Boot :8082
  +-- REST API (22 Controllers)
  +-- MCP Server (18 Tools)
  +-- Security (JWT + RBAC)
  +-- Service Layer (22 模块)
       |
       | JDBC          HTTP
       v               v
   MySQL :3306    RAG :8000
  (25+ tables)   (向量检索)
       |
       | I/O
       v
  ~/.aifiles (文件存储)
```

### 3.2 后端分层架构 (严格单向依赖)

```
L4 -- Controller / MCP  <-- 22 Controllers + 4 MCP 模块
      REST 端点 + MCP 18 工具注册 + Streamable HTTP
       ^
L3    Service            <-- 22 业务服务
      ToolService / ForumPostService / VideoService
      KnowledgeBaseService / RagApiClient ...
       ^
L2 -- Repository         <-- 26 数据仓库
      Spring Data JPA + 自定义查询 + 分页排序
       ^
L1 -- Model / DTO        <-- 35 Entities + 61 DTOs
      JPA 实体映射 + 请求/响应 DTO + 参数校验
       ^
L0 -- Config / Util      <-- 18 基础设施
      SecurityConfig / JwtAuthFilter / XssSanitizer
      UploadConfig / RagApiClient / DataInitializer
      9 Exception classes (全局异常处理)
```

**约束规则：**

- 禁止循环依赖：Controller -> Service -> Repository -> Model 单向
- XSS 防护：用户输入经 `XssSanitizer.sanitize()`
- 软删除：`status = DELETED` (Tool / ForumPost / Video)
- 禁止 null 返回：抛异常或返回 Optional

### 3.3 前端分层架构

```
L4 -- Pages (29 页面)
      HomePage / PostListPage / VideoListPage
      KnowledgeDetailPage / OverviewPage ...
       ^
L3 -- Components (34 组件)
      AppHeader / PostContent / VideoPlayer
      DanmakuPlayer / KnowledgeSearch / TagBadge ...
       ^
L2 -- Stores (3 Pinia)
      auth (JWT/角色) / forum (分类/帖子) / theme (暗/亮)
       ^
L1 -- Services (9 Axios)
      api.ts / tool.ts / forum.ts / video.ts
      knowledge.ts / interaction.ts ...
       ^
L0 -- Types & Composables (9)
      7 TypeScript 接口 + 2 Composables
```

### 3.4 数据库设计

**25+ 表，分 7 个领域组：**

| 领域 | 表数量 | 核心表 |
|------|--------|--------|
| 核心 | 6 | user, category, tool, tool_file, tool_like, tool_comment |
| 标签 | 3 | tag, tool_tag, video_tag (统一标签系统) |
| 论坛 | 7 | forum_post, forum_comment, forum_like, post_favorite ... |
| 微课 | 5 | video, video_comment, video_like, video_favorite, danmaku |
| 知识库 | 2 | knowledge_base, kb_document |
| 通知 | 1 | notification (LIKE/COMMENT/SYSTEM) |
| 反馈 | 1 | feedback_message |

**迁移管理：** Flyway 9 个迁移文件 (V1~V9)，启动时自动执行

完整字段定义请参阅 [docs/ARCHITECTURE.md](ARCHITECTURE.md)。

---

## 四、功能模块详解

### 4.1 工具广场 (核心功能)

工具广场是 CodingHub 的核心，类似于 App Store 的工具发现与下载中心。

**功能清单：**

| 功能 | 说明 |
|------|------|
| 工具发布 | 名称、描述、分类、版本、标签、附件上传 (单文件 50MB) |
| 搜索筛选 | 关键词搜索 + 分类过滤 (Skill/MCP/插件/Prompt/其他) + 排序 (热度/最新/评分) |
| 互动系统 | 点赞、评论、收藏、浏览量统计、评分排行 |
| 文件管理 | 多文件上传、在线下载、文件类型检测 |
| 版本管理 | 自动版本号递增、修改历史记录 |
| MCP 桥接 | AI 助手通过 `tool_search` / `tool_download` 自动发现并获取工具 |
| 标签系统 | 统一标签 (TOOL 类型)，支持 "必装"、"热门"、"置顶" 等特殊标记 |

**页面路由：**

| 路径 | 说明 |
|------|------|
| `/` | 首页 (工具列表 + 搜索 + 分类) |
| `/tools/:id` | 工具详情 (Markdown 描述 + 文件下载 + 互动) |
| `/tools/upload` | 上传新工具 |
| `/me/tools/:id/edit` | 编辑工具 |

### 4.2 社区论坛

技术经验分享与讨论的社区平台。

**功能清单：**

| 功能 | 说明 |
|------|------|
| Markdown 发帖 | 完整 Markdown 语法、代码高亮、Mermaid 图表 |
| 分类体系 | 技术交流 / 工具分享 / 问题求助 / 心得体会 |
| 统一标签 | TOOL / FORUM / VIDEO 三类型标签跨模块复用 |
| 嵌套评论 | 多级回复 (parentId/rootId)、匿名点赞 (IP hash) |
| 收藏管理 | "我的收藏" 页面、一键收藏/取消 |
| 个人中心 | "我的帖子" 页面、编辑/删除/软删除 |
| 通知联动 | 评论/点赞触发 Notification，铃铛组件实时未读计数 |

**页面路由：**

| 路径 | 说明 |
|------|------|
| `/forum` | 帖子列表 (分类筛选 + 搜索 + 排序) |
| `/forum/posts/:id` | 帖子详情 (Markdown 渲染 + 嵌套评论) |
| `/forum/editor` | 新建/编辑帖子 |
| `/forum/my-posts` | 我的帖子 |
| `/forum/my-favorites` | 我的收藏 |

### 4.3 微课视频

技术视频分享与弹幕互动平台。

**功能清单：**

| 功能 | 说明 |
|------|------|
| 视频上传 | 最大 1GB、自动转码、封面图自动截取/手动选择 |
| HTML5 播放器 | 进度控制、全屏播放、自适应码率 |
| 弹幕系统 | 实时弹幕 (DanmakuPlayer)、颜色/时间/类型可配 |
| 互动功能 | 点赞、收藏、评论、浏览量统计 |
| 个人管理 | "我的视频" / "视频收藏" 页面 |
| 统一标签 | VIDEO 类型标签、标签筛选与热门排行 |

**页面路由：**

| 路径 | 说明 |
|------|------|
| `/videos` | 视频列表 (卡片布局 + 封面预览) |
| `/videos/:id` | 视频详情 (播放器 + 弹幕 + 评论) |
| `/videos/upload` | 上传视频 |
| `/videos/my-videos` | 我的视频 |
| `/videos/my-favorites` | 视频收藏 |

### 4.4 知识库 (RAG 驱动)

基于向量检索的智能知识管理系统，是 CodingHub 最具技术含量的模块。

**核心架构：**

```
文档上传 -> 格式转换 -> 语义分块 -> 向量嵌入 -> 存储检索
   |            |           |           |          |
   |         markitdown   动态阈值   Qwen3-Embedding  zvec
   |                      (mean-1sigma) (1024维)    向量库
   |
   +-- 支持: MD/TXT/PDF/DOCX/PPTX/XLSX/PY/JS/TS/JAVA/GO
```

**功能清单：**

| 功能 | 说明 |
|------|------|
| 知识库 CRUD | 创建/编辑/删除、权限控制 (仅 Owner 可修改) |
| 文档上传 | 支持 10+ 种文件格式，二进制文件保存原文件 |
| 异步处理流水线 | UPLOADING -> CONVERTING -> CHUNKING -> EMBEDDING -> READY |
| 语义分块 | 动态阈值 (mean-1sigma) 检测余弦断裂点，可配 chunk_size/overlap |
| 向量检索 | Qwen3-Embedding-0.6B (1024维) + zvec + Rerank |
| 搜索结果渲染 | Markdown 片段高亮显示，支持代码块和列表 |
| MCP 集成 | `kb_list` / `kb_search` / `kb_create` 等 7 个知识库 MCP 工具 |

**RAG 服务独立部署：**

| 配置项 | 值 |
|--------|-----|
| 端口 | :8000 |
| 传输协议 | stdio / SSE / Streamable HTTP / REST API |
| 向量库 | zvec (轻量级向量数据库) |
| 分块策略 | 语义分块动态阈值 + 超长回退递归字符 |
| 批次插入 | 1024 分块/批次，失败降级逐条插入 |

**页面路由：**

| 路径 | 说明 |
|------|------|
| `/knowledge` | 知识库列表 |
| `/knowledge/:id` | 知识库详情 (文档列表 + 语义搜索) |
| `/knowledge/create` | 创建知识库 |
| `/knowledge/:id/edit` | 编辑知识库配置 |

### 4.5 其他功能模块

| 模块 | 说明 |
|------|------|
| 概览仪表盘 | 全局统计 (用户/工具/帖子/视频) + 工具/帖子/视频排行榜 |
| 通知系统 | LIKE / COMMENT / SYSTEM 三类通知，铃铛组件实时未读计数 |
| 留言反馈 | 公开留言板 + 管理员回复 |
| 用户管理 | 注册审批 (PENDING->APPROVED/REJECTED) + 角色分配 |
| 统一互动 | 跨模块点赞/评论/收藏 + 匿名支持 (IP hash) |
| 快速开始 | MCP 配置引导页，帮助新用户快速接入 |

---

## 五、MCP 协议集成

MCP (Model Context Protocol) 是 CodingHub 的核心差异化特性，让 AI 助手可以直接操作平台。

### 5.1 传输协议

MCP Server 通过 Streamable HTTP 传输协议暴露，单端点、会话制 (MCP 2025-03-26)：

| 协议 | 端点 | 说明 |
|------|------|------|
| Streamable HTTP | `/mcp` (POST) | 单端点、会话制 (MCP 2025-03-26) |

### 5.2 18 个 MCP 工具

| 分类 | 数量 | 工具列表 |
|------|------|---------|
| 只读查询 | 6 | `tool_search`, `tool_get`, `tool_files`, `tool_download`, `post_search`, `post_get` |
| 写操作+认证 | 5 | `tool_create`, `tool_modify`, `tool_file_delete`, `post_create`, `kb_create/update/delete` |
| 信息桥接 | 3 | `tool_file_upload`, `kb_upload_document`, `kb_document_status` |
| 知识库检索 | 4 | `kb_list`, `kb_search`, `kb_create`, `kb_update` |

### 5.3 Server 信息

| 项目 | 值 |
|------|-----|
| 名称 | H3CodingHub-MCP-Server v2.0.0 |
| SDK | io.modelcontextprotocol.sdk:mcp-bom:2.0.0 |
| 注册位置 | `McpSdkServerConfig.java` |

### 5.4 智能体对接配置

在智能体的 MCP 配置中添加：

```json
{
  "mcpServers": {
    "codinghub": {
      "type": "streamableHttp",
      "url": "http://your-codinghub-host/mcp"
    }
  }
}
```

> 将 `your-codinghub-host` 替换为实际服务器地址。内网部署无需公网访问。

---

## 六、安全与权限

### 6.1 认证机制

| 项目 | 说明 |
|------|------|
| JWT Token | Access Token (15min) + Refresh Token (7天) |
| 密码加密 | BCrypt |
| 请求头 | `Authorization: Bearer <token>` |

### 6.2 角色权限

| 角色 | 权限范围 |
|------|---------|
| USER | 管理自己的内容 (工具/帖子/视频/知识库) |
| ADMIN | 管理所有用户 + 删除任何内容 |
| SUPER_ADMIN | 审批用户注册 + 角色分配 |

**权限模型：** 内容操作遵循 `isOwner || isAdmin` 原则

### 6.3 安全防护

| 防护措施 | 实现方式 |
|---------|---------|
| XSS 防护 | `XssSanitizer.sanitize()` 净化用户输入 |
| SQL 注入 | JPA 参数化查询 |
| 文件上传限制 | 单文件 50MB，总请求 200MB，类型检测 |
| 软删除 | `status = DELETED` 标记，数据可恢复 |

---

## 七、快速开始

### 7.1 环境要求

| 组件 | 要求 | 说明 |
|------|------|------|
| JDK | 17+ | Spring Boot 运行环境 |
| MySQL | 8.0+ | 数据库，库名 `ai_tool_square` |
| Node.js | 18+ | 前端构建 (仅开发需要) |
| Nginx | 1.26+ | 生产环境反向代理 (可选) |

**无需：** Docker、Redis、Elasticsearch、外部 CDN

### 7.2 Windows 一键启动

```powershell
# 初始化数据库 + 安装依赖 + 启动服务
.\setup-windows.ps1

# 启动全部服务
.\run-windows.ps1

# 停止全部服务
.\stop-windows.ps1
```

### 7.3 Linux / macOS

```bash
# 初始化数据库
make db

# 安装前端依赖
make install

# 启动后端 (:8082)
make backend

# 启动前端 (:5173)
make frontend

# 同时启动后端+前端
make run

# 停止所有服务
make stop

# 代码质量检查
make lint
```

### 7.4 端口配置

| 服务 | 端口 | 说明 |
|------|------|------|
| 后端 | 8082 | Spring Boot 应用 |
| 前端 | 5173 | Vite 开发服务器 |
| MySQL | 3306 | 数据库服务 |
| RAG | 8000 | 向量检索服务 (可选) |
| Nginx | 80 | 生产环境反向代理 (可选) |

### 7.5 使用 MCP 发现工具

配置 MCP 连接后，AI 助手可以：

1. **搜索工具** — 调用 `h3_coding_hub_tool_search` 检索工具
2. **查看详情** — 调用 `h3_coding_hub_tool_get` 获取完整说明
3. **一键下载** — 调用 `h3_coding_hub_tool_download` 获取下载链接

**示例对话：**

> "帮我找 CodingHub 上标记为'必装'的工具"
>
> AI 自动搜索 -> 展示结果 -> 一键下载安装

---

## 八、部署架构

### 8.1 部署模式

| 模式 | 配置 | 说明 |
|------|------|------|
| 开发模式 | Vite :5173 + Spring Boot :8082 + MySQL :3306 | 前后端分离，支持 HMR |
| 生产模式 | Nginx :80 反向代理 | 前端静态资源由 Nginx 或 Spring Boot 托管 |

### 8.2 文件存储

- 默认路径：`~/.aifiles`
- 环境变量：`AIHUB_FILE_BASE_DIR`
- 存储内容：工具附件、视频文件、封面图片

### 8.3 离线部署要点

1. **打包依赖**: 后端使用 Gradle 将所有依赖打包为 Fat JAR
2. **前端构建**: 前端构建为静态资源，由后端直接托管
3. **数据库迁移**: Flyway 自动执行 SQL 迁移，无需手动建表
4. **无需外网**: 所有资源内嵌，启动后即可通过局域网访问

---

## 九、API 端点一览

### 9.1 核心 API

| 前缀 | 说明 |
|------|------|
| `/api/v1/auth` | 认证 (login/register/refresh) |
| `/api/v1/tools` | 工具 CRUD + 点赞 |
| `/api/v1/tools/{id}/files` | 文件上传/下载 |
| `/api/v1/categories` | 工具分类 |
| `/api/v1/users` | 用户 (profile/avatar) |
| `/api/v1/admin` | 管理 (审批/用户) |
| `/api/v1/interactions` | 统一互动 (点赞/评论/收藏) |
| `/api/overview` | 统计/排行 |

### 9.2 论坛 API

| 前缀 | 说明 |
|------|------|
| `/api/forum/posts` | 论坛帖子 |
| `/api/forum/categories` | 论坛分类 |
| `/api/forum/tags` | 论坛标签 |
| `/api/forum/likes` | 论坛点赞 |
| `/api/v1/post-favorites` | 帖子收藏 |

### 9.3 其他 API

| 前缀 | 说明 |
|------|------|
| `/api/v1/videos` | 微课 CRUD+互动 |
| `/api/v1/knowledge` | 知识库 CRUD+搜索 |
| `/api/v1/feedback` | 留言反馈 |
| `/api/v1/notifications` | 通知 (未读/已读) |
| `/api/v1/tags` | 统一标签 (列表/热门) |
| `/mcp` | MCP (18 tools, Streamable HTTP) |

---

## 相关文档

| 文档 | 说明 |
|------|------|
| [README.md](../README.md) | 项目简介与快速开始 |
| [ARCHITECTURE.md](ARCHITECTURE.md) | 详细架构说明、ER 图、序列图 |
| [DEVELOPMENT.md](DEVELOPMENT.md) | 开发环境搭建与工作流程 |
| [AGENTS.md](../AGENTS.md) | AI 代理快速参考 |
| [MASTER.md](../design-system/CodingHub/MASTER.md) | 设计系统规范 |

---

*最后更新: 2026-06-30*
