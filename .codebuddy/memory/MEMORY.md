# 内存 - AI 工具广场项目

## 项目核心规则

1. 禁止私自提交代码：需求开发须人工确认
2. 禁止在循环中请求数据库或调接口
3. 遍历集合优先用 for，避免 while/foreach/stream/iterator
4. 方法不返回 null（抛异常或 Optional）

## 数据库（双库共存，Profile 切换）

- MySQL 8（默认 profile=mysql）：localhost:3306 / root / root / ai_tool_square
- PostgreSQL：localhost:5432 / codinghub / codinghub / ai_tool_square
- 业务代码零改动，Hibernate 6 按连接自动探测方言；`User` 保留 `@Table(name="`user`")`，`globally_quoted_identifiers=true` 仅放 postgresql profile

## 设计系统（前端）

- Cyberpunk Glassmorphism 暗色：底 `#0D0D0D`，强调 `#00FFFF`/`#FF00FF`/`#00FF00`
- 图标 `@lucide/vue-next`；字体 `Fira Code`+`Fira Sans`

## CodeWiki MCP Wiki 生成/重建要点（高价值，可复用）

> 仓库 LLM Wiki 用 CodeWiki MCP 生成，落在 `repowiki/wiki/`（模块在 `modules/`，总览 `overview.md`）。

- 流程：`analyze_repo`（重建会话；若缓存丢失会报 "Session not found"，重跑即可）→ classifier 脚本建 `module_tree.json` → `save_module_tree` → `get_processing_order`（叶优先）→ 逐模块 `write_doc_file` → 写 `overview.md` → `lint_wiki` → `close_session`。
- **`close_session` 可能清空 `repowiki/` + `.codewiki/workspace/`**（会话开始时曾观测到 repowiki 消失，需重生成）；本次末次调用却保留了。行为不稳定 → **调用前先 `cp -R repowiki /tmp/repowiki-backup` 备份**。
- `write_doc_file` **不会覆盖**已存在的 module 文件 → 必须 `rm` 后重写；`edit_doc_file` 当前**损坏**（`UnboundLocalError: cannot access local variable 'repo_path'`）→ 用「删+重写」代替。
- **Mermaid 坑（本校验器严格）**：
  - 禁止一行多节点（`A[x] B[y]` 解析报错）→ 每节点独占一行；
  - 节点/边标签里**禁花括号**（`{id}` 会破坏解析）→ 改用 `(id)`；
  - 中文子图标题需加引号 `subgraph "中文"`；
  - 校验器返回 503 = 服务宕机（非语法错），稍后重试。
- `overview.md` **不是合法 page_type** → 直接 `write_to_file` 到 `repowiki/wiki/overview.md`（不走 `write_doc_file`）。其内链接须写 `modules/X.md`（从 `wiki/` 解析）；模块文档互链用裸 `X.md`（在 `modules/` 内解析）。
- `lint_wiki` 的 checks 合法值：`all`/`stale_refs`/`undocumented`/`broken_links`/`cycles`/`coverage`/`orphan_pages`/`no_outlinks`/`missing_aliases`/`stale_sources`/`superseded_pages`。`missing_aliases` 仅 info（不阻断）。目标 `health_score: 100`、0 error。
- 论坛接口路径是 `/api/forum/...`（**不含 /v1**），与其余 `/api/v1/...` 不同——文档与前端 `forum.ts` 都别多写 `/v1`。
