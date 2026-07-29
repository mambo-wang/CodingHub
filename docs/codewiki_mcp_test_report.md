# CodeWiki MCP 功能验证测试报告

> 验证维度：以 Prompts 定义的 **7 个工作流** 为纲，覆盖 CodeWiki MCP Server 暴露的全部 **22 个工具**（含 2 个 legacy）。
> 验证日期：2026-07-29 · 目标仓库：`/Users/kirito/repos/CodingHub`（Java 17 + Vue3 全栈，约 1213 个代码组件）
> 验证方式：逐个工具实际发起 MCP 调用，捕获输入/输出/异常，不依赖文档声明。

---

## 1. 总体结论

| 指标 | 结果 |
|------|------|
| 工具总数 | 22（代码分析 7 · 跨服务 1 · 文档生成 6 · 知识库 5 · 质量保障 2 · 会话管理 1；另含 legacy 工具 2 个独立计数） |
| 完全正常 | 21（原 19 + 本次修复 2 个 legacy） |
| 调用成功但存在行为偏差 | 0（原 3 项偏差均已修复） |
| 故障（服务端 bug） | 0（原 2 个 legacy 已修复） |
| 工作流端到端可用 | 7 / 7（含跨服务追踪工作流，现已闭环） |

**核心结论（2026-07-29 二次回归验证）**：初版报告的 **5 个问题项已全部修复**——
- 2 个 legacy 工具（`get_module_tree`、`generate_docs`）的服务端签名 bug 已消除，前者正常返回模块树，后者优雅返回订阅错误（CodingPlan 未订阅属预期）；
- 3 个行为偏差（`query_cross_service` 取不回数据、`save_module_tree` 忽略 `output_dir`、`query_wiki` 强制 `output_dir`）已修复：跨服务索引已落盘且可闭环、`save_module_tree` 尊重 `output_dir`、`query_wiki` 现接受 `repo_path` 推导。

知识库、质量审计、Wiki 生成、跨服务追踪四大主流程功能现已完整可用。

---

## 2. 全量工具验证汇总

| # | 工具 | 分类 | 状态 | 关键发现 |
|---|------|------|------|----------|
| 1 | `analyze_repo` | 代码分析 | ✅ 通过 | 增量模式下不清除已有 wiki；1213 组件 / 280 叶节点；SQLite 缓存 |
| 2 | `list_components` | 代码分析 | ✅ 通过 | 返回聚合统计 + 指针文件，内联不返回明细，`name_pattern` 未体现在计数 |
| 3 | `list_dependencies` | 代码分析 | ✅ 通过 | `direction` 支持 `both`；返回聚合统计 + `dependencies.json` |
| 4 | `analyze_impact` | 代码分析 | ✅ 通过 | `direction` 枚举实为 `depended_by/depends_on/both`（非文档示例的 `callers`） |
| 5 | `read_code_components` | 代码分析 | ✅ 通过 | 落盘源码到 `.codewiki/workspace/sources/` |
| 6 | `view_repo_file` | 代码分析 | ✅ 通过 | 支持 `max_lines`/`line_offset` |
| 7 | `query_cross_service` | 跨服务 | ✅ 通过（已修复） | 二次回归：跨服务索引已落盘，传 `output_dir` 可正确返回 1 条链接（repo-a→repo-b）；默认路径亦可开箱闭环 |
| 8 | `write_doc_file` | 文档生成 | ✅ 通过 | 含 Mermaid 语法校验 |
| 9 | `edit_doc_file` | 文档生成 | ✅ 通过 | `str_replace` 正常，含 Mermaid 校验 |
| 10 | `save_module_tree` | 文档生成 | ✅ 通过（已修复） | 现**尊重 `output_dir`**，写入 `/tmp/codewiki_save_test/.meta/` 而非仓库 repowiki |
| 11 | `get_processing_order` | 文档生成 | ✅ 通过 | 基于 module_tree 计算叶优先顺序 |
| 12 | `get_prompt` | 文档生成 | ✅ 通过 | 全部模板类型（code_analysis/system_leaf/wiki_query/extraction_scan/taxonomy_plan/wiki_lint_report/cluster/impact_review…）均返回内容 |
| 13 | `generate_docs` | 文档生成(legacy) | ✅ 通过（已修复） | 签名 bug 已消除；无 CodingPlan 订阅时优雅返回 `InvalidSubscription`（预期行为） |
| 14 | `query_wiki` | 知识库 | ✅ 通过（已修复） | 现接受 `repo_path` 推导 `output_dir`，无需显式 `output_dir` 亦可检索 |
| 15 | `ingest_note` | 知识库 | ✅ 通过 | 笔记写入 `notes/` |
| 16 | `ingest_source` | 知识库 | ✅ 通过 | 源文件存入 `raw/sources/`，含抽取判定 |
| 17 | `retract_source` | 知识库 | ✅ 通过 | `flag_stale`(dry_run) 与 `remove_refs` 两模式均验证 |
| 18 | `batch_ingest` | 知识库 | ✅ 通过 | 批量写入多条笔记 |
| 19 | `lint_wiki` | 质量保障 | ✅ 通过 | 实际执行 13 项检查（超出 schema 枚举，含 overview_stale/unsupported_claims 等） |
| 20 | `flag_issue` | 质量保障 | ✅ 通过 | `severity` 枚举为 `error/warning/info`（非 low/medium/high） |
| 21 | `analyze_workspace` | 工作区分析 | ✅ 通过（已修复） | 检测到跨服务链接（2 路由→1 匹配），**结果已持久化**（workspace_routes.json / cross_service_links.json） |
| 22 | `close_session` | 会话管理 | ✅ 通过 | 清理工作区并重建索引 |
| 23 | `get_module_tree` | 会话管理(legacy) | ✅ 通过（已修复） | 签名 bug 已消除，正常返回 12 模块树 |

---

## 3. 按工作流（Prompts）维度验证

### 3.1 `generate-wiki` — 生成代码 Wiki 流水线
> 对应 Prompt：`generate-wiki`（阶段模板 cluster / system_leaf / system_complex / user / overview_module / overview_repo / code_analysis）

| 阶段 | 工具 | 验证 |
|------|------|------|
| ① 仓库分析 | `analyze_repo` ✅ | 1213 组件，增量模式保留 wiki |
| ② 聚类模板 | `get_prompt`(cluster/system_leaf) ✅ | 返回聚类 SOP 与模块树格式（dict of name→{path,components}） |
| ③ 保存模块树 | `save_module_tree` ✅⚠️ | 功能正常，但 **output_dir 被忽略**，写入仓库 repowiki |
| ④ 处理顺序 | `get_processing_order` ✅ | 返回叶优先顺序 |
| ⑤ 读取组件 | `read_code_components` / `list_components` ✅ | 签名与依赖可读取 |
| ⑥ 撰写文档 | `write_doc_file` ✅ | 12 篇模块文档 + overview 已生成（既有成果） |
| ⑦ 质量体检 | `lint_wiki` ✅ | health_score=100 |
| ⑧ 收尾索引 | `close_session` ✅ | 重建 BM25 索引 |

**结论**：主流程可用；需留意 `save_module_tree` 会越过传入的 `output_dir` 直写仓库，调用方须确保不破坏既有 module_tree。

### 3.2 `extract-knowledge` — 外部文档知识抽取
> 对应 Prompt：`extract-knowledge`（模板 extraction_scan / entity_page / concept_page / source_summary）

| 工具 | 验证 |
|------|------|
| `ingest_source` ✅ | 成功导入 markdown 源文件并判定可抽取 |
| `ingest_note` ✅ / `batch_ingest` ✅ | 决策/教训/陷阱类笔记写入 |
| `query_wiki` ✅（需 output_dir） | 检索验证抽取结果 |

**结论**：完整可用。

### 3.3 `search-wiki` — 知识库搜索策略
> 对应 Prompt：`search-wiki`（模板 wiki_query）

| 工具 | 验证 |
|------|------|
| `query_wiki` ✅⚠️ | BM25 检索正常（返回文档 + 关联组件），但**必须传 `output_dir`** |

**结论**：检索能力可靠；`output_dir` 为事实必填，建议修正 schema 或文档。

### 3.4 `quality-check` — 文档质量审计
> 对应 Prompt：`quality-check`（模板 wiki_lint_report）

| 工具 | 验证 |
|------|------|
| `lint_wiki` ✅ | 13 项检查，0 error，health_score=100 |
| `flag_issue` ✅ | 成功标注 issue（`severity=info/warning/error`） |
| `edit_doc_file` ✅ | 用于按 issue 修复文档 |

**结论**：完整可用。

### 3.5 `incremental-update` — 增量更新 Wiki
> 复用：analyze_repo(增量) → list_components/analyze_impact → edit_doc_file/write_doc_file → lint_wiki → close_session

| 工具 | 验证 |
|------|------|
| `analyze_repo`(增量) ✅ | 本次验证即增量模式，未清文档 |
| `analyze_impact` ✅ | 影响面分析（17 受影响 / 7 high-risk） |
| `edit_doc_file`/`write_doc_file`/`lint_wiki`/`close_session` ✅ | 见 3.1 / 3.4 |

**结论**：可用。

### 3.6 `workspace-analysis` — 多仓库工作区分析
> 对应 Prompt：`workspace-analysis`（依赖 analyze_workspace + get_module_tree + query_cross_service）

| 工具 | 验证 |
|------|------|
| `analyze_workspace` ✅ | 在隔离的 2 仓库工作区中正确识别 1 条跨服务链接，并**持久化**索引 |
| `query_cross_service` ✅（已修复） | 传入一致 `output_dir` 可正确返回 1 条链接；默认路径开箱闭环 |
| `get_module_tree`(legacy) ✅（已修复） | 正常返回 12 模块树 |

**结论**：工作流现已端到端可用。`analyze_workspace` 会持久化 `workspace_routes.json` / `cross_service_links.json`，`query_cross_service` 可据此取回跨服务关系。

### 3.7 `cross-service-trace` — 跨服务调用链追踪
> 对应 Prompt：`cross-service-trace`（依赖 analyze_workspace + query_cross_service，可选 CBM `trace_path`）

| 工具 | 验证 |
|------|------|
| `analyze_workspace` ✅ | 检测到跨服务链接并落盘 |
| `query_cross_service` ✅（已修复） | 返回 1 条链接（repo-a→repo-b，`GET /api/users/{}`，confidence 1.0） |

**结论**：**工作流已端到端闭环**（二次回归验证确认）。注意：`analyze_workspace` 与 `query_cross_service` 的 `output_dir` 必须一致（或均用默认 `<workspace>/workspace-wiki`），否则后者会读不到索引而返回空。

---

## 4. 关键异常与修复建议

### 🟢 已在二次回归中修复（2026-07-29）
1. **`get_module_tree` (legacy)** ✅ 已修复 — 服务端签名 bug（`takes 1 positional argument but 2 were given`）已消除，现正常返回模块树。
2. **`generate_docs` (legacy)** ✅ 已修复 — 签名 bug 已消除；无 CodingPlan 订阅时优雅返回 `InvalidSubscription`（预期行为，IDE 流程不依赖）。
3. **`query_cross_service` 取不回数据** ✅ 已修复 — 根因 `analyze_workspace` 未持久化索引已解决：现落盘 `workspace_routes.json` / `cross_service_links.json`，`query_cross_service` 可据此返回跨服务链接。**注意**：二者 `output_dir` 须一致（或均用默认 `<workspace>/workspace-wiki`），否则仍返回空。
4. **`save_module_tree` 忽略 `output_dir`** ✅ 已修复 — 现尊重 `output_dir`，不再越权写入仓库 repowiki（实测写入 `/tmp/codewiki_save_test/.meta/`）。
5. **`query_wiki` 强制要求 `output_dir`** ✅ 已修复 — 现可传 `repo_path` 推导 `output_dir`，无需显式 `output_dir` 亦可检索。

### 🟡 文档/枚举与实测的差异（功能正常，调用方按 schema 传值即可，无需修代码）
6. **`analyze_impact` 的 `direction` 枚举** 实际为 `depended_by/depends_on/both`，与 Prompt 示例 `callers/callees` 不一致。
7. **`flag_issue` 的 `severity` 枚举** 为 `error/warning/info`，与直觉 low/medium/high 不同。
8. **`lint_wiki` 的 `checks` 枚举** 实际执行 13 项（含 `overview_stale`、`unsupported_claims`、`isolated_components` 等超出文档枚举），`all` 为安全取值。
9. **`list_components` / `list_dependencies`** 内联返回聚合统计 + 指针文件路径，明细需读文件；`name_pattern` 不直接缩小内联计数。

### 🟢 验证通过（无需处理）
`analyze_repo`、`list_components`、`list_dependencies`、`analyze_impact`、`read_code_components`、`view_repo_file`、`write_doc_file`、`edit_doc_file`、`get_processing_order`、`get_prompt`、`ingest_note`、`ingest_source`、`retract_source`、`batch_ingest`、`lint_wiki`、`flag_issue`、`analyze_workspace`、`close_session`、`query_wiki`、`save_module_tree`、`query_cross_service`、`generate_docs`、`get_module_tree` 全部通过。

---

## 4.5 二次回归验证明细（2026-07-29）

针对初版报告的 5 个问题项逐一复测，复测环境与首次一致的隔离仓库 + 真实 repowiki 备份。

| # | 问题项 | 初版结论 | 复测方法 | 复测结果 |
|---|--------|----------|----------|----------|
| 1 | `get_module_tree` 签名 bug | ❌ `takes 1 positional argument but 2 were given` | 直接调用 `get_module_tree(repo_path=...)` | ✅ 返回 12 模块树，无报错 |
| 2 | `generate_docs` 签名 bug | ❌ 同上 | 调用 `generate_docs(doc_type=architecture)` | ✅ 不再崩溃；返回 `InvalidSubscription`（无 CodingPlan，预期） |
| 3 | `query_cross_service` 恒返回空 | ⚠️ 跨服务索引未落盘 | 隔离双仓库 `analyze_workspace` → 检查 `.meta/workspace_routes.json` 落盘 → `query_cross_service` | ✅ 索引已落盘；`query_cross_service`（传一致 `output_dir` 及默认路径两种）均返回 1 条链接 |
| 4 | `save_module_tree` 忽略 `output_dir` | ⚠️ 越权写入仓库 repowiki | 传 `output_dir=/tmp/codewiki_save_test` 后检查写入位置 | ✅ 写入 `/tmp/codewiki_save_test/.meta/`，仓库 repowiki 未被改写 |
| 5 | `query_wiki` 强制 `output_dir` | ⚠️ schema 可选但服务端强校验 | 仅传 `query`+`repo_path`（不传 `output_dir`） | ✅ 返回 9 条相关文档，证明 `repo_path` 可推导 `output_dir` |

**遗留提示（非缺陷，调用方须知）**：
- 跨服务流程中 `analyze_workspace` 与 `query_cross_service` 的 `output_dir` 必须一致，或均使用默认 `<workspace>/workspace-wiki`，否则 `query_cross_service` 会因读不到索引而返回空。

---

## 5. 验证安全说明

- **备份**：验证前已 `cp -R repowiki /tmp/repowiki-backup-20260729`，所有写入型调用（ingest/flag/save_module_tree/write/edit）均落在可恢复范围。
- **隔离**：分析类破坏性测试（analyze_workspace）在 `/tmp/cw_test_ws` 隔离双仓库中进行，未触及生产仓库。
- **恢复**：验证后已用备份完整覆盖 `repowiki`（12 模块、原始 module_tree、原始 issues 均还原），并清理全部 `/tmp` 临时产物与备份副本。
- **回归确认**：恢复后 `query_wiki` 检索「统一互动 TargetType」仍可返回 3 篇相关文档及关联代码组件，wiki 功能无损。
