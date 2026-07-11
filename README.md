<p align="center">
  <a href="https://github.com/mambo-wang/CodingHub" target="_blank">
    <img src="https://img.shields.io/badge/GitHub-CodingHub-181717?style=flat-square&logo=github" alt="GitHub" />
  </a>
</p>

# CodingHub

> AI 工具及使用经验分享平台 —— 离线部署，仅需 MySQL 8.0+ 与 JDK 17，简洁安全，专为企业内部网络管控环境打造。

## 项目简介

CodingHub 是一个支持**离线部署**的 AI 工具发现与经验分享网站。项目仅依赖 **MySQL 8.0+** 和 **JDK 17**，无需 Redis、Elasticsearch 等额外中间件，部署极简。特别适用于**网络管控严格的企事业单位**——所有服务在局域网内运行，数据不外泄，安全可控。

### 核心特性

| 特性 | 说明 |
|------|------|
| 🔌 **极简依赖** | 仅需 MySQL 8.0+ 和 JDK 17，无其他中间件 |
| 🏢 **企业友好** | 支持完全离线部署，数据留存本地，满足内网安全要求 |
| 🔗 **MCP Server** | 内置 18 个 MCP 工具 + 6 套提示词模板，任意智能体可对接，一次配置自动拉取 |
| 🧠 **RAG 知识库** | 内置向量检索知识库，支持语义搜索、多种分块策略与重排序 |
| 🎨 **双主题设计** | Cyberpunk Glassmorphism 暗色/亮色主题切换 |
| 🔐 **安全第一** | JWT 认证 + XSS 防护 + 参数校验 + 角色权限控制 |
| 📦 **开箱即用** | 一键启动脚本，Windows / Linux / macOS 均支持 |

## 功能概览

### 工具广场

AI 工具的发现、分享与管理中心。

- 工具浏览：分页列表、分类筛选、关键词搜索、热度/最新排序
- 工具详情：Markdown 文档渲染、版本信息、标签展示、附件下载
- 工具发布：名称、分类、版本、描述、Markdown 内容编辑器、标签选择
- 文件管理：多文件上传（单文件最大 50MB，总量 200MB）、下载、删除
- 版本管理：支持自动递增版本号
- 管理员置顶：管理员可将工具置顶展示
- 热度排行：Hot Top 5 工具榜单
- 我的工具：查看个人上传的工具列表
- 我的收藏：查看收藏的工具列表

### 论坛社区

技术经验分享与讨论的社区板块。

- 帖子浏览：分页列表、分类筛选、标签筛选、关键词搜索、热度/最新排序
- 帖子详情：Markdown 内容渲染、浏览/点赞/评论计数
- 发帖/编辑：标题、Markdown 内容、分类选择、标签关联
- 帖子可见性：支持公开/受限可见性控制
- 管理员置顶：管理员可将帖子置顶
- 热度排行：Hot Top 5 帖子榜单
- 我的帖子：查看个人发布的帖子
- 我的收藏：查看收藏的帖子
- 标签管理：创建系统/用户标签，热门标签排行

### 微课视频

视频微课分享与互动平台。

- 视频浏览：卡片式列表、缩略图展示、热度/最新排序
- 视频播放：内嵌播放器，支持 HTTP Range 分段加载，流畅大文件播放
- 弹幕系统：实时弹幕覆盖播放，支持自定义颜色、位置（滚动/顶部/底部）和时间戳
- 视频上传：文件选择 + 标题、描述、标签等元数据
- 自定义封面：支持上传/选择视频封面图
- 管理员置顶：管理员可将视频置顶
- 热度排行：Hot Top 5 视频榜单
- 我的视频：查看个人上传的视频
- 我的收藏：查看收藏的视频

### RAG 知识库

基于向量检索的智能知识库系统，配套 Python RAG 服务。

- 知识库管理：创建、编辑、删除知识库，配置分块模式/大小/重叠/重排序
- 文档上传：单文件或批量上传（最多 20 个文件），异步后台处理
- 支持格式：md、txt、pdf、docx、pptx、xlsx、py、js、ts、java、go 等 30+ 种文本/代码格式
- 分块策略：递归字符分割、语义分块（基于 Embedding 主题边界检测）、结构化分块（Markdown 标题/代码/表格感知）
- 向量存储：zvec 嵌入式向量数据库，FP32 精度
- Embedding 模型：sentence-transformers（默认 all-MiniLM-L6-v2，384 维），支持 HuggingFace 国内镜像
- 重排序：Cross-encoder 重排序（BAAI/bge-reranker-v2-m3），提升检索相关性
- 语义搜索：可配置 top_k、重排序开关、glob 源文件过滤、上下文扩展（相邻分块检索）
- 文档状态追踪：上传 → 转换 → 分块 → 向量化 → 就绪/失败，全流程可视
- 文件变更检测：SHA-256 哈希校验，跳过未变更文件的重复导入

### MCP Server

内置 MCP (Model Context Protocol) Server，支持 Streamable HTTP 和 SSE 双协议，任意智能体可对接。

**18 个 MCP 工具：**

| 工具 | 说明 |
|------|------|
| `tool_search` | 按关键词和分类搜索工具 |
| `tool_get` | 获取工具详情及完整 Markdown 文档 |
| `tool_files` | 获取工具附件列表及下载链接 |
| `tool_download` | 获取指定附件的下载链接 |
| `tool_create` | 创建新工具（需认证） |
| `tool_modify` | 更新工具并自动递增版本号（需认证） |
| `tool_file_upload` | 获取文件上传接口信息 |
| `tool_file_delete` | 删除工具附件（需认证） |
| `post_search` | 按关键词搜索论坛帖子 |
| `post_get` | 获取帖子内容 |
| `post_create` | 发布新帖子（需认证） |
| `kb_list` | 列出知识库（分页） |
| `kb_search` | 知识库语义搜索 |
| `kb_create` | 创建新知识库（需认证） |
| `kb_update` | 更新知识库配置（需认证） |
| `kb_delete` | 删除知识库（需认证） |
| `kb_upload_document` | 获取 RAG 文档批量上传接口信息 |
| `kb_document_status` | 查询文档处理状态 |

**6 套 Prompt 模板：** 搜索工具、安装工具、检查版本更新、发布工具、更新工具、发布帖子到论坛。

**资源订阅：** 工具目录、最近更新工具、单个工具详情，支持资源变更自动通知。

### 统一互动系统

贯穿工具、帖子、视频三大内容模块的统一交互层。

- 点赞：支持对工具/帖子/视频点赞，已登录用户和匿名用户（IP 哈希）均可参与
- 评论：支持对任意内容类型发表主题评论，支持嵌套回复（楼中楼）
- 收藏：登录用户可收藏任意内容类型，按类型分页查看
- 互动统计：浏览量、点赞数、评论数、收藏数统一展示

### 用户与认证

- 用户注册：用户名 + 密码 + 昵称，普通用户自动审批，管理员注册需超级管理员审批
- 登录认证：JWT 令牌（Access Token + Refresh Token）
- 个人中心：头像上传（支持 JPG/PNG/WebP/GIF）、昵称编辑、个人简介、密码修改
- 角色权限：USER / ADMIN / SUPER_ADMIN 三级角色体系

### 管理后台

- 注册审批：审批/驳回待审核的管理员注册申请
- 用户管理：用户列表、搜索、角色/状态筛选、启用/禁用/删除账户
- 内容管理：置顶/取消置顶工具、帖子、视频

### 平台概览

- 全局统计：工具总数、帖子总数、视频总数、用户总数
- 排行榜：工具排行、帖子排行、视频排行

### 留言反馈

- 留言板：提交留言（支持匿名），分类筛选（建议、Bug 反馈、表扬、其他）
- 管理员回复：管理员可回复和删除留言

### 通知系统

- 通知列表：分页查看个人通知
- 未读计数：未读通知数量角标
- 标记已读：单条标记已读 / 全部标记已读

## 预览截图

> 将截图文件放入 `docs/screenshot.png` 即可显示。

![CodingHub 预览](docs/screenshot.png)

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.5 |
| 编程语言 | Java | 17 |
| 前端框架 | Vue 3 + TypeScript | 3.4 / 5.4 |
| 构建工具 | Vite | 5.2 |
| 数据库 | MySQL | 8.x |
| 数据库迁移 | Flyway | — |
| 认证 | JWT | 0.12.5 |
| RAG 服务 | Python + sentence-transformers | — |
| 向量数据库 | zvec (嵌入式) | — |

## 项目结构

```
CodingHub/
├── backend/                    # Java Spring Boot 后端
│   └── src/main/java/com/iaihub/toolbox/
│       ├── controller/        # REST API 控制器 (22)
│       ├── service/           # 业务逻辑层 (22)
│       ├── repository/        # 数据访问层 (26)
│       ├── model/             # 实体类 (35)
│       ├── dto/               # 数据传输对象 (61)
│       ├── config/            # 配置类 (Security, JWT, MCP, Upload, RAG)
│       ├── exception/         # 异常处理
│       ├── util/              # 工具类 (JWT, XSS)
│       └── mcp/               # MCP Server (18 tools + SSE)
├── frontend/                  # Vue 3 + TypeScript 前端
│   └── src/
│       ├── components/        # Vue 组件 (36)
│       ├── pages/             # 页面 (28)
│       ├── services/          # API 调用
│       ├── stores/            # 状态管理
│       ├── router/            # 路由配置
│       └── types/             # TypeScript 类型定义
├── rag/                       # RAG 知识库 Python 服务
├── design-system/             # 双主题设计系统
├── docs/                      # 详细文档
├── harness/                   # Agent 基础设施
├── scripts/                   # 脚本工具
├── specs/                     # 功能规格说明
├── openspec/                  # OpenSpec 变更管理
└── Makefile                   # 快速命令
```

## 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- MySQL 8.x
- npm

### Windows 一键启动

```powershell
# 初始化数据库 + 安装依赖 + 启动服务
.\setup-windows.ps1

# 启动全部服务
.\run-windows.ps1

# 停止全部服务
.\stop-windows.ps1
```

### Linux / macOS

```bash
# 初始化数据库
make db

# 安装前端依赖
make install

# 启动后端 (8082)
make backend

# 启动前端 (5173)
make frontend

# 同时启动后端+前端
make run

# 停止所有服务
make stop
```

### 端口配置

| 服务 | 端口 | 说明 |
|------|------|------|
| 后端 | 8082 | Spring Boot 应用 |
| 前端 | 5173 | Vite 开发服务器 |
| MySQL | 3306 | 数据库服务 |

## MCP Server 对接

在智能体的 MCP 配置中添加 CodingHub Server 即可：

```json
{
  "mcpServers": {
    "CodingHub-mcp": {
      "type": "sse",
      "url": "http://127.0.0.1:8082/sse",
      "description": "CodingHub MCP Server",
      "disabled": false
    }
  }
}
```

支持 Streamable HTTP (`/mcp`) 和 SSE (`/sse`) 双传输协议。配置完成后，智能体可自动拉取工具目录、搜索工具、发布内容等。

## API 入口点

### 后端 API (http://localhost:8082)

| 前缀 | 说明 | 前缀 | 说明 |
|------|------|------|------|
| `/api/v1/auth` | 认证（注册/登录/刷新令牌） | `/api/forum/posts` | 论坛帖子 CRUD |
| `/api/v1/tools` | 工具 CRUD + 点赞 | `/api/forum/categories` | 论坛分类 |
| `/api/v1/categories` | 工具分类 | `/api/v1/post-favorites` | 帖子收藏 |
| `/api/v1/users` | 用户资料 / 头像 | `/api/overview` | 平台统计 / 排行榜 |
| `/api/v1/admin` | 管理后台（审批/用户管理） | `/mcp` | MCP Streamable HTTP |
| `/api/v1/videos` | 微课视频 | `/sse` | MCP SSE 传输 |
| `/api/v1/interactions` | 统一互动（点赞/评论/收藏） | `/api/v1/feedback` | 留言反馈 |
| `/api/v1/knowledge` | 知识库管理 + 语义搜索 | `/api/v1/notifications` | 通知系统 |
| `/api/v1/tags` | 统一标签系统 | | |

### 前端页面 (http://localhost:5173)

| 路径 | 说明 | 路径 | 说明 |
|------|------|------|------|
| `/` | 首页 / 工具列表 | `/forum` | 论坛帖子列表 |
| `/login` | 登录 | `/forum/posts/:id` | 帖子详情 |
| `/register` | 注册 | `/forum/editor` | 发帖 / 编辑 |
| `/tools/:id` | 工具详情 | `/my-posts` | 我的帖子 |
| `/upload` | 上传工具 | `/my-favorites` | 我的收藏（帖子） |
| `/tools/:id/edit` | 编辑工具 | `/videos` | 视频列表 |
| `/profile` | 个人中心 | `/videos/:id` | 视频详情 |
| `/admin/approval` | 注册审批 | `/videos/upload` | 上传视频 |
| `/admin/users` | 用户管理 | `/my-videos` | 我的视频 |
| `/overview` | 平台概览 | `/knowledge` | 知识库列表 |
| `/feedback` | 留言反馈 | `/knowledge/:id` | 知识库详情 |
| `/quickstart` | 快速入门 | `/about` | 关于 |

## 数据库表结构

通过 Flyway 迁移脚本自动管理（V1 ~ V9）。

| 模块 | 表名 | 说明 |
|------|------|------|
| 核心 | `user` | 用户（角色、状态、头像、简介） |
| 核心 | `category` | 工具分类 |
| 核心 | `tool` | 工具（名称、Markdown 内容、版本、热度分数、置顶标记） |
| 核心 | `tool_file` | 工具附件 |
| 论坛 | `forum_post` | 帖子（标题、Markdown 内容、热度分数、可见性） |
| 论坛 | `forum_category` | 帖子分类 |
| 论坛 | `forum_tag` | 论坛标签（系统/用户定义） |
| 论坛 | `forum_post_tag` | 帖子-标签关联 |
| 微课 | `video` | 视频（标题、描述、封面、热度分数） |
| 微课 | `danmaku` | 弹幕（时间戳、颜色、类型） |
| 互动 | `unified_like` | 统一点赞（支持工具/帖子/视频） |
| 互动 | `unified_comment` | 统一评论（支持嵌套回复） |
| 互动 | `unified_favorite` | 统一收藏 |
| 标签 | `tag` | 统一标签（类型：TOOL/FORUM/VIDEO） |
| 标签 | `tool_tag` / `video_tag` | 工具/视频-标签关联 |
| 知识库 | `knowledge_base` | 知识库（名称、描述、RAG 配置） |
| 知识库 | `kb_document` | 知识库文档 |
| 其他 | `notification` | 通知（类型、目标、已读状态） |
| 其他 | `feedback_message` | 留言反馈（分类、管理员回复） |
| 其他 | `post_favorite` | 帖子收藏 |

## 设计风格

本项目采用 **Cyberpunk Glassmorphism** 双主题设计：

| 属性 | 暗色主题 | 亮色主题 |
|------|----------|----------|
| 背景色 | `#0D0D0D` 深黑 | `#F5F0FF` 淡紫白 |
| 主色调 | `#00FFFF` Cyan | `#7C3AED` 紫罗兰 |
| 辅助色 | `#FF00FF` Magenta | `#F97316` 活力橙 |
| 字体 | Fira Code + Fira Sans | Fira Code + Fira Sans |

详细设计规范请参阅 [design-system/CodingHub/MASTER.md](design-system/CodingHub/MASTER.md)。

## 离线部署

CodingHub 专为离线环境设计：

1. **打包依赖**: 后端使用 Gradle 将所有依赖打包为 Fat JAR
2. **前端构建**: 前端构建为静态资源，由后端直接托管
3. **数据库迁移**: Flyway 自动执行 SQL 迁移，无需手动建表
4. **无需外网**: 所有资源内嵌，启动后即可通过局域网访问

## 约束规则

### 代码约束

1. **禁止循环依赖**: controller → service → repository → model 单向依赖
2. **XSS 防护**: 所有用户输入通过 `XssSanitizer.sanitize()` 过滤
3. **JWT 认证**: 需要认证的接口在 Header 携带 `Authorization: Bearer <token>`
4. **禁止 null 返回**: 方法不返回 null，抛异常或返回 Optional
5. **禁止循环调用**: 禁止在循环中请求数据库或调用接口

### Git 约束

1. **禁止私自提交**: 需求开发的过程中不得私自提交代码
2. 提交信息遵循 Conventional Commits 格式
3. 单次提交不超过 1000 行更改

## 相关文档

- [**使用指导**](docs/GUIDE.md) - 技术架构、功能详解与快速上手（推荐新手阅读）
- [架构详情](docs/ARCHITECTURE.md) - 详细架构说明、ER 图、序列图
- [开发指南](docs/DEVELOPMENT.md) - 开发环境搭建与工作流程
- [Agent 导航地图](AGENTS.md) - AI 代理快速参考
- [设计系统](design-system/CodingHub/MASTER.md) - 双主题 UI 规范
- [RAG 服务](rag/README_CN.md) - RAG 知识库 Python MCP 服务
- [环境配置](harness/config/environment.json) - 运行时环境变量

---

**最后更新**: 2026-07-11
