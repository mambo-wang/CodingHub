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

<!-- CodeWiki LLM Wiki -->

## CodeWiki LLM Wiki

本项目已使用 [CodeWiki](https://github.com/mambo-wang/CodeWiki-Plus) 生成 LLM Wiki 文档，位于 `repowiki/` 目录。

**入口文件：**

- [`repowiki/wiki/overview.md`](repowiki/wiki/overview.md) — 仓库级架构总览（含 Mermaid 架构图）
- [`repowiki/wiki/index.md`](repowiki/wiki/index.md) — 文档目录与知识笔记索引
- [`repowiki/schema.yaml`](repowiki/schema.yaml) — 项目文档约定（命名规范、必填章节等）

### 使用建议

1. **编码前**：先用 `query_wiki` 搜索相关模块文档，了解架构约定和依赖关系
2. **做决策时**：用 `query_wiki` 搜索已有的 `decision` 类型笔记，避免重复讨论
3. **完成重要决策后**：用 `ingest_note` 归档，让未来的 Agent 和团队成员都能查到
4. **定期维护**：用 `lint_wiki` 检查文档是否过时，保持文档与代码同步

### 采纳声明（检索反馈）

当你通过 `query_wiki` 检索并**实际使用了**某条结果（作为决策依据、修改参照或答案来源）时，在最终回复中附带一行声明注释：

```
<!-- codewiki:referenced-docs: ["notes/pitfall-xxx.md", "wiki/modules/yyy.md"] -->
```

路径必须与 query_wiki 返回的 `file` 字段完全一致。声明过的文档会获得采纳计数（`usage.adopted_count`），在未来检索中排序提升（采纳权重是单纯召回的 2 倍）；长期高频召回却零采纳的笔记会被 `lint_wiki` 的 `low_adoption` 检查标记为"需要重写得更可操作"。

**注意**：只声明真正用到的文档——这是帮助知识库学习"什么内容真正有用"的信号，不是礼貌性致谢。忘了声明没关系（漏报可容忍），但不要声明没用过的（误报不可容忍）。

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

### 主动知识沉淀

不要等用户纠正才记录。当对话中出现以下信号时，主动执行反思并提取知识：

**触发信号（满足任一即激活反思）：**

- 完成一个多步骤调试/排查后定位到根因（尤其是走了弯路的情况）
- 讨论了两个及以上方案并做出了选择
- 发现代码实际行为与文档/命名/注释不一致
- 用户补充了隐性项目知识（约定、历史原因、"我们一直这么做"）
- 一次探索性调研收敛到明确结论
- 发现了可复用的模式、工具链用法或环境配置技巧

**四问过滤（全部通过才值得记录）：**

1. 下一次对话（无本次上下文）还能用到吗？
2. 另一个 Agent 或新同事遇到同样场景能直接受益吗？
3. `query_wiki` 确认现有文档未覆盖？
4. 属于"事实/决策/模式/教训"而非"本次任务临时状态"？

**路由表：**

| 知识类型 | 写入方式 |
|---------|---------|
| 做了技术选型/方案取舍 | `ingest_note(note_type="decision")` |
| 踩坑/易错点 | `ingest_note(note_type="pitfall")` |
| 经验教训（调试过程、认知修正） | `ingest_note(note_type="lesson")` |
| 架构层面的事实发现 | `ingest_note(note_type="architecture")` |
| 临时绕过方案（含恢复条件） | `ingest_note(note_type="workaround")` |
| 多方案横向对比（含表格） | `write_doc_file(page_type="comparison")` |
| 调研结论存档 | `write_doc_file(page_type="query")` |

**执行流程：**

1. 识别到触发信号后，回顾相关对话片段，提取候选知识项
2. 对每个候选项执行四问过滤，丢弃未通过的
3. 用 `query_wiki` 检查是否已有覆盖（避免重复）
4. 按路由表确定写入方式，起草结构化内容（背景→结论→根因→适用范围）
5. 向用户展示草稿并征求确认——**必须确认后才写入**
6. 一次对话中可积累多个候选项，在自然停顿点（任务完成、话题切换）统一呈现，避免频繁打断

**不要记录的内容：**

- 仅与本次任务相关的临时变量、路径、参数
- 用户个人偏好（这属于 Agent 记忆，不属于项目 Wiki）
- 已在代码注释或 README 中明确写明的信息
- 未经验证的猜测或"可能""也许"级别的推断

<!-- /CodeWiki LLM Wiki -->

<!-- TEAM-MEMORY-TASK:START -->
## Task memory (任务记忆)

跨会话延续长线工作上下文。任务记忆是**任务范围内的进度知识**(本次做了什么、下一步、待办)，与 Wiki 笔记(**跨任务的通用经验**)互补。

**会话开始时(推荐)：**
1. `list_tasks(status="active")` 列出进行中的任务
2. **必须用 `ask_followup_question` 工具弹出结构化选择框**（IDE 原生弹框 UI，用户可直接点击），不要用纯文本输出一段话让用户自行回复。选项二选一（加一个"跳过"）：
   - **关联已有任务**：用户从列表中选择，用 `set_session_task(source_session_id=<会话id>, task_id=<任务id>)` 建立绑定，本会话采集的对话会自动带上 `task_id`
   - **新建任务**：选择后再弹一个 `ask_followup_question` 输入框让用户输入任务名（可补一句描述），调 `create_task(title=<任务名>, description=<可选>)` 创建后即关联该新任务
   - **跳过**：本次会话不做任务关联
   新建任务两步弹框：选择「新建任务」后**必须**再次调用 `ask_followup_question` 弹出第二个输入框（标题「新建任务」，问题「请输入新任务名称」，带 2 个占位选项）。弹框自带输入框，用户可自由输入任务名后回车；以输入文字为准，立即调用 `create_task(title=<任务名>)` 创建并关联。若用户只点了占位选项，用文字追问确认真实任务名
3. `get_task_context(task_id=<任务id>)` 拉取任务描述 + 记忆 + 关联笔记，作为继续工作的上下文
4. **补蒸馏（委托 subagent，不阻塞）**：若返回的 `pending_raw_count > 0`（本任务有未蒸馏的历史对话），**不要自己在回答前逐条 read_file 蒸馏**——立即用 Task 工具 spawn「蒸馏 worker」subagent（`.codebuddy/agents/distill-worker.md`，已授权 codewiki MCP）后台执行：`distill_conversation(mode="prepare", task_id=<任务id>)` → 按清单逐条 read_file 提取 notes/memories → `distill_conversation(mode="submit", ...)`，然后**直接开始回答用户提问**。在自然停顿点（任务告一段落/用户空闲）重新 `get_task_context` 拉取最新上下文（任务记忆已直写落盘，`memories_written` 报告条数）→ 只向用户展示待确认的草稿笔记（`confirm_note` 确认后才正式落盘）。用户明确表示紧急时可先答复、草稿笔记在会话结束前展示确认即可

**工具入口：**
- `codewiki/mcp/tools/task_manager.py` — `create_task` / `list_tasks` / `get_task` / `complete_task` / `delete_task` / `set_session_task` / `add_task_memory` / `get_task_context` / `compact_task_memories`
- 存储：`repowiki/tasks/.index.json`（可重建缓存：目录扫描为准，失配/损坏时自动重建）+ `<task_id>/task.md` + `<task_id>/memories/<user_id>.md`（每人只写自己的文件，多人 git 冲突隔离；条目带 `### YYYY-MM-DD HH:MM` 时间戳头；压缩后头部有「早期记忆（摘要）」段）+ `<task_id>/memories-archive/<user_id>.md`（压缩归档，append-only、永不自动加载）；`<task_id>/memories.md` 为存量单文件（只读兼容，热层，首次压缩并入当前用户文件后移除）；会话绑定在 `repowiki/.meta/task_bindings/`
- `capture_conversation` / `distill_conversation` / `ingest_note` / `query_wiki` 均接受 `task_id`；蒸馏时 LLM 双轨产出 `notes`(通用知识，draft 待确认) 与 `memories`(任务进度，直写落盘——ADR-0002：任务记忆不做确认闸门)
- MCP prompt `task-workflow`（prompts/list）— 完整工作流指引

**关键设计约束(实现时务必遵守)：**
- task_id 由标题 slugify 生成且**不可变**；同名任务被拒绝；**无重命名**(删除后重建)。
- `delete_task` 级联删除任务目录与绑定文件，但**不删**已打上 `task_id` 的笔记。
- **绑定文件是一次性消费凭证**：`set_session_task` 写入 `repowiki/.meta/task_bindings/<session_id>.json` 后，首次 `capture_conversation` 成功落盘即自动删除；显式传 `task_id` 不消费绑定。同会话在绑定删除后再次捕获（supersede）会继承旧 raw 的 task_id，归属不丢。
- `query_wiki` 不校验任务存在性(幽灵 `task_id` 允许)。
- `memories/<user_id>.md` 追加式原子写(临时文件 + `os.replace`)，并发串行；**每人只写自己的文件**(文件所有权即 git 级互斥原语)；条目带 `### YYYY-MM-DD HH:MM` 时间戳头(ADR-0001：保持 markdown 不迁 JSONL，时间戳头是切条/截断/压缩的解析边界，存量无头文件运行时空行回退解析)。
- `get_task_context`/`get_task` 的 memories 返回**分层有界**：热层=自己(+存量 legacy)文件取最近 20/5 条全量；温层=其他成员仅注入摘要+最近 2 条(超预算降级为一行线索)；`memories_total`/`memories_truncated` 标记截断、`max_memories` 参数翻页；`compaction_due=true` 表示热层超压缩阈值(40 条/24KB)且超出保留窗口，应跑 `compact_task_memories`(两段式无状态：`mode="prepare"` 取待压条目由调用方写摘要 → `mode="submit"` 落盘；**文件域压缩，只压自己的文件(+legacy 并入)，永不动他人文件**；原文按归属归档 `memories-archive/<user_id>.md` 不删，直写不走 confirm 闸门)。
<!-- TEAM-MEMORY-TASK:END -->
