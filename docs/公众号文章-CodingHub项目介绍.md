# SDD+TDD 双驱动实战：从零构建 AI 工具共享平台 CodingHub

> 本文介绍 CodingHub 项目的完整研发过程，展示规格驱动开发（SDD）与测试驱动开发（TDD）在实际项目中的落地实践。

---

## 一、这是什么？

**CodingHub** 是一个面向 AI 智能体的工具发现与经验分享平台。你可以把它理解为一个"AI 工具的 App Store"——开发者把自己的 Skill、MCP Server、Prompt 模板发布到广场上，其他用户的 AI 助手通过 MCP 协议接入后，就能直接搜索、下载、安装这些工具。

### 一句话概括

> 让 AI 助手能逛"应用商店"，一键发现和安装新能力。

### 为什么需要它？

现在 AI 编码助手（CodeBuddy、Claude Code、Copilot CLI 等）的能力扩展依赖 Skill/MCP 生态，但这些工具的**发现和分发**还停留在口口相传或手动克隆 GitHub 仓库的阶段。CodingHub 解决了两个核心痛点：

1. **发现难**：不知道有哪些工具可用，缺少统一的分类和搜索入口
2. **安装烦**：手动下载、解压、配置，没有一键安装体验

---

## 二、核心功能一览

### 🛠️ 工具广场

工具发布、搜索、分类浏览、详情查看。支持版本管理、文件附件下载、评论和点赞。

> 示例：在 AI 助手中说 *"使用 CodingHub MCP 查询工具列表，排查有无需要升级的工具"*，助手自动对比本地版本与在线版本，发现过期即可一键升级。

### 🔌 内置 MCP Server

CodingHub 不仅是一个平台，自身也是一个 **MCP Server**，提供以下工具函数供 AI 智能体直接调用：

| 函数 | 功能 |
|------|------|
| `h3_coding_hub_tool_search` | 搜索工具列表 |
| `h3_coding_hub_tool_get` | 获取工具详情 |
| `h3_coding_hub_tool_files` | 获取工具附件列表 |
| `h3_coding_hub_tool_download` | 下载工具文件 |
| `h3_coding_hub_post_search` | 搜索社区帖子 |
| `h3_coding_hub_post_get` | 获取帖子详情 |

AI 助手接入后，用户只需用自然语言描述需求，助手自动通过 MCP 完成工具发现、下载、安装全流程。

### 💬 社区论坛

技术经验分享、工具使用心得交流。支持 Markdown 写作、分类检索、评论互动、收藏管理。

### 👤 用户中心

个人资料、头像上传、我的工具管理（含版本更新）、我的帖子和收藏。

### 🎨 双主题设计

Cyberpunk Glassmorphism 风格，暗色/亮色主题一键切换，数据留存本地不丢失。

---

## 三、技术架构

### 整体架构

```
┌─────────────────────────────────────────────────┐
│                    前端 (Vue 3)                    │
│          port 5173 · Vite · TypeScript            │
│    Element Plus · Pinia · Vue Router · Axios     │
└────────────────────┬────────────────────────────┘
                     │ REST API (/api/v1)
┌────────────────────▼────────────────────────────┐
│                后端 (Spring Boot 3.2.5)            │
│          port 8082 · Java 17 · Gradle             │
│  JWT Auth · BCrypt · Flyway · XSS Filter         │
│                    ┌──────────┐                   │
│                    │ MCP SSE  │ ← AI 智能体接入    │
│                    └──────────┘                   │
└────────────────────┬────────────────────────────┘
                     │ JDBC
┌────────────────────▼────────────────────────────┐
│                   MySQL 8.x                      │
│          flyway 数据库版本管理                     │
└─────────────────────────────────────────────────┘
```

### 前端分层架构

| 层级 | 目录 | 职责 |
|------|------|------|
| L0 类型/工具 | `types/` `utils/` | TypeScript 类型定义、工具函数 |
| L1 服务层 | `services/` | Axios HTTP 封装，调用后端 API |
| L2 状态管理 | `stores/` | Pinia Store，全局状态 |
| L3 组件 | `components/` | 可复用 Vue 组件 |
| L4 页面 | `pages/` | 路由页面，组合 L3 组件 |

**单向依赖**：页面 → 组件 → 状态 → 服务 → 类型，禁止反向引用。

### 后端分层架构

| 层级 | 包路径 | 职责 |
|------|--------|------|
| L0 配置/工具 | `config/` `util/` | Security、JWT、文件上传配置 |
| L1 模型 | `model/` `dto/` | JPA 实体、数据传输对象 |
| L2 数据访问 | `repository/` | Spring Data JPA |
| L3 业务逻辑 | `service/` | 核心业务逻辑 |
| L4 API 控制器 | `controller/` | REST 端点、MCP 端点 |

### 数据库设计

5 张核心表，简洁且完备：

```
user        —— 用户（邮箱、密码BCrypt、头像）
category    —— 工具分类（名称、图标、排序）
tool        —— 工具（名称、内容、版本、上传者、状态）
tool_file   —— 工具附件（文件路径、名称、大小）
forum_post  —— 论坛帖子（标题、内容、分类、标签、收藏）
```

### 安全机制

- **JWT 认证**：无状态令牌，支持过期刷新
- **BCrypt 密码**：单向加密，不可逆
- **XSS 防护**：所有用户输入经 `XssSanitizer` 过滤
- **分层权限**：公开接口无需登录，发布/管理需认证

---

## 四、开发方法论：SDD + TDD 双驱动

本项目全程采用 **规格驱动开发（Spec-Driven Development, SDD）** 和 **测试驱动开发（Test-Driven Development, TDD）** 相结合的方式。

### SDD 流程

```
需求分析 → 编写规格文档 → 设计评审 → 生成实现计划 → 代码实现 → 验证交付
```

#### 规格文档体系（`specs/` 目录）

每个功能模块有独立的规格目录，包含：

```
specs/001-ai-tool-square/
├── spec.md          # 功能规格（GIVEN/WHEN/THEN 场景）
├── design.md         # 架构设计（组件树、数据流、接口契约）
├── tasks.md          # 任务拆解（原子化步骤清单）
├── quickstart.md     # 快速入门文档
└── checklists/       # 验收清单
    ├── requirements.md
    └── design.md
```

#### OpenSpec 工作流（`openspec/` 目录）

规格驱动的标准化研发流水线：

```
brainstorming → writing-plans → TDD实现 → code-review → browser-test → archive
```

### TDD 实践

每个功能模块严格遵循 **RED → GREEN → REFACTOR** 循环：

1. **RED**：先写失败的测试用例
2. **GREEN**：用最少代码让测试通过
3. **REFACTOR**：重构优化，保持测试绿色
4. **Commit**：每个循环提交一次

### 开发数据

| 指标 | 数量 |
|------|------|
| 规格文档 | 33 个功能规格 |
| OpenSpec 变更记录 | 105+ 份工作流文档 |
| 前端页面 | 19 个页面 |
| 前端组件 | 19 个可复用组件 |
| 后端 Java 文件 | 117 个 |
| 后端 API 控制器 | 20+ |
| 数据库迁移脚本 | 7 个 Flyway 脚本 |

---

## 五、企业级特性

### 离线优先

项目设计之初就考虑了企业内网管控环境：**零外部 CDN 依赖**，所有资源本地化，启动仅需 JDK 17 + MySQL 8。

### 一键部署

提供跨平台启动脚本：

```bash
# Windows
./setup-windows.ps1   # 初始化环境
./run-windows.ps1      # 启动前后端

# Linux/Mac
make install && make run
```

### Agent 基础设施

项目内置 `harness/` 目录，提供 AI 代理运行时的环境配置和启动脚本，确保开发环境和生产环境的一致性。

---

## 六、写在最后

CodingHub 是 SDD + TDD 方法论的一次完整实践。从需求分析到规格文档，从任务拆解到逐条实现，从单元测试到浏览器端到端验证，全程由规格驱动、测试保障。

**关键收获**：

- **SDD 让需求不跑偏**：每个功能上线前都有明确的 GIVEN/WHEN/THEN 验收标准
- **TDD 让代码可信赖**：每次提交都经过测试验证，重构时有安全网
- **OpenSpec 工作流让过程可追溯**：从 brainstorming 到 archive，每个环节都有文档记录

> 项目地址：`CodingHub`（内部仓库）

---

*发布于 CodingHub 社区 · 2026-06-13*
