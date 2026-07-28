# 内存 - AI 工具广场项目

## 项目核心规则

1. 禁止私自提交代码：需求开发须人工确认
2. 禁止在循环中请求数据库或调接口
3. 遍历集合优先用 for，避免 while/foreach/stream/iterator
4. 方法不返回 null（抛异常或 Optional）
5. **新增评论/点赞/收藏功能禁止重复造轮子**：复用已有统一互动代码（`UnifiedLikeService`/`UnifiedFavoriteService`/`UnifiedCommentService` + `UnifiedInteractionController` `/api/v1/interactions` + `TargetType` 枚举扩展），不要新建 `xxx_like`/`xxx_favorite` 表

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
- **`close_session` 和 `analyze_repo` 都可能清空 `repowiki/` 文档**（2026-07-28 回归测试确认：一次 `analyze_repo` 重新初始化清掉了全部 13 个模块文档）。任何 CodeWiki 会话操作前 → **先 `cp -R repowiki /tmp/repowiki-backup` 备份**。恢复 = 拷回 `wiki/modules/`、`wiki/overview.md`、`.meta/module_tree.json`，然后 `close_session` 重建索引。
- `query_wiki` 在 BM25 索引未建/丢失时**静默返回空结果**（不报错）→ 结果为空先怀疑索引，跑一次 `close_session` 重建再查。
- `write_doc_file` **不会覆盖**已存在的 module 文件 → 必须 `rm` 后重写；`edit_doc_file` **已修复**（2026-07-28 验证 str_replace/insert/undo 均正常，此前 UnboundLocalError 已消失），可正常用于小修改。小 bug：frontmatter 注入后 `---` 与正文首行 `#` 标题会粘连成 `---# 标题`。
- `generate_docs`(legacy) 依赖服务端内置 LLM（火山 CodingPlan 订阅），订阅过期即 400；IDE 流程用细粒度工具，不受影响。
- **Mermaid 坑（本校验器严格）**：
  - 禁止一行多节点（`A[x] B[y]` 解析报错）→ 每节点独占一行；
  - 节点/边标签里**禁花括号**（`{id}` 会破坏解析）→ 改用 `(id)`；
  - 中文子图标题需加引号 `subgraph "中文"`；
  - 校验器返回 503 = 服务宕机（非语法错），稍后重试。
- `overview.md` **不是合法 page_type** → 直接 `write_to_file` 到 `repowiki/wiki/overview.md`（不走 `write_doc_file`）。其内链接须写 `modules/X.md`（从 `wiki/` 解析）；模块文档互链用裸 `X.md`（在 `modules/` 内解析）。
- `lint_wiki` 的 checks 合法值：`all`/`stale_refs`/`undocumented`/`broken_links`/`cycles`/`coverage`/`orphan_pages`/`no_outlinks`/`missing_aliases`/`stale_sources`/`superseded_pages`。`missing_aliases` 仅 info（不阻断）。目标 `health_score: 100`、0 error。
- 论坛接口路径是 `/api/forum/...`（**不含 /v1**），与其余 `/api/v1/...` 不同——文档与前端 `forum.ts` 都别多写 `/v1`。

## CodingHub 项目自带 MCP Server（后端 /mcp，20 工具）

- 启动：后端 `./gradlew bootRun`（8082）→ MCP 随应用初始化，暴露 `/mcp`(streamable-http) 与 `/sse`，各 20 tools/3 resources/6 prompts。`/mcp/**`、`/sse/**` 在 SecurityConfig 为 **permitAll**（MCP 调用无需 token）。
- RAG 服务独立：`cd rag && python3 server.py --host 127.0.0.1 --port 8000`（前端 KB 类工具依赖它）。
- 写类工具（tool_create/post_create/kb_*）内部用调用方传入的 username/password 调 `userService.login` 鉴权；可用 admin/Cloud@1234。
- **【BUG】`h3_coding_hub_kb_document_status` 单文档分支（传 docId）会触发 `output validation failed: 未找到所需属性 kbId/documents/totalCount`**——工具输出 schema 只匹配"列表"分支结构，单文档分支返回裸文档对象不合规。位置 `backend/.../mcp/IaihubToolHandler.java` `handleKbDocumentStatus`（L612-630）。修复：单文档分支也包成 `KbDocumentStatusResponse` 形状，或放宽输出 schema。列表分支（不传 docId）正常。
- 验证客户端：Python `mcp` SDK `streamablehttp_client("http://127.0.0.1:8082/mcp")` + `ClientSession`。
- **后端空闲久了会僵死**：TCP 端口 LISTEN 但 HTTP 全部超时（curl/python HTTP 000，仅 nc 通），多为 DB 连接池陈旧。重启后端即可恢复；`/actuator/health` 在本项目未暴露（返回 500 是 404 被全局异常转译），不代表异常。
