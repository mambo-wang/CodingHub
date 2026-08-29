# 插件市场增强：收藏数展示 + 标签 + 置顶/热门角标 — 设计文档

日期：2026-08-29 ｜ 状态：已批准 ｜ 范围：CodingHub 插件市场

## 背景

插件市场列表页卡片 stats 行目前只有点赞/评论/浏览，缺收藏数；插件模块未接入统一标签系统；管理员无法置顶插件，卡片也无置顶/热门角标。用户决策：收藏数计入热度分 score；标签在上传+编辑页维护、卡片与详情页都展示；热门角标沿用 hot-top5 模式。

## 方案选择

- 收藏计数（采纳）：对齐工具广场 Tool 既有模式——冗余 `favoriteCount` 列 + repository 原子 UPDATE 同步维护计数与 score。否决"实时 COUNT"：score 是存储字段且 hot 排序依赖它，收藏变化无法联动。
- 标签（采纳）：统一标签系统扩展 `TagType.PLUGIN` + `plugin_tag` 关联表，复用 `TagService`/`TagSelector`/`TagBadge`。
- 置顶/热门（采纳）：完全对齐 Tool：`pinned` 字段 + `POST/DELETE /plugins/{id}/pin`（ADMIN/SUPER_ADMIN）+ hot 排序 `pinned DESC, score DESC`（new 排序不置顶优先，与工具一致）+ `hot-top5` 接口驱动前端 Flame 角标。

## 后端设计

1. **Plugin 实体**（`model/Plugin.java`）
   - 新增 `favorite_count`（Integer 默认 0）、`pinned`（Boolean 默认 false）。Schema 走 Hibernate `ddl-auto: update`（项目权威机制，双库兼容，不另写 Flyway）。
   - `updateScore()`：`view×1 + like×3 + favorite×4 + comment×5`（收藏权重对齐 Tool；插件无下载量），同步注释。
   - 新增 `increment/decrementFavoriteCount()` 实体方法（内部调 `updateScore()`）。
2. **PluginRepository**
   - `@Modifying` 原子 JPQL `increment/decrementFavoriteCount`：计数与 `score ±4` 同步，`status='NORMAL'` 守卫 + `CASE WHEN` 下限保护（对齐 ToolRepository 写法；教训：计数更新必须 repository 层 @Modifying，禁止 save 读改写）。
   - `pinById/unpinById` 原子更新（对齐 ToolRepository）。
   - `findByFiltersOrderByHot` 改为 `ORDER BY p.pinned DESC, p.score DESC`；`findByFilters`（最新）保持 `createdAt DESC`。
   - `findTop5ByStatusOrderByScoreDesc`（hot-top5 用）。
3. **UnifiedFavoriteService.toggleFavorite**：计数块从 `TOOL` 扩展 `PLUGIN` 分支。
4. **标签接入**
   - `TagType` 枚举加 `PLUGIN`；新建 `PluginTag`（表 `plugin_tag`：plugin_id + tag_id）+ `PluginTagRepository`，对齐 `ToolTag`。
   - 插件创建/更新请求 DTO 加 `tags: List<String>`；`PluginService` 走 `TagService.resolveOrCreateTags(names, PLUGIN)` 重建关联并维护 usageCount（对齐 ToolService 模式）。
   - 查询详情/列表时装配 tags（批量查关联，避免 N+1 失控）。
5. **接口**（`PluginController` 或 `PluginMarketController`）
   - `POST/DELETE /api/v1/plugins/{id}/pin`，`@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")`。
   - `GET /api/v1/plugins/hot-top5`，匿名可访问，返回 5 个 ID（对齐 `/tools/hot-top5`）。
6. **DTO**：`PluginSummaryDTO` 加 `favoriteCount`、`pinned`、`tags`；`toSummaryDTO()`/`copySummary()` 补映射，`PluginDetailDTO` 继承。
7. **存量回填（幂等）**：`DataInitializer` 启动步骤——按 favorite 表 `target_type='PLUGIN'` 分组计数修正 `Plugin.favoriteCount` 不一致项并 `updateScore()` 重算；tags 无存量无需回填。

## 前端设计

8. `types/plugin.ts`：`PluginSummary` 加 `favoriteCount: number`、`pinned: boolean`、`tags: Tag[]`。
9. `PluginMarketPage.vue`
   - stats 行 Heart 之后加 `<Bookmark :size="14" /> {{ fmtCount(p.favoriteCount) }}`（图标与工具广场一致）。
   - 名称行加角标：`pinned` → `badge-pill badge-pinned`（ArrowUp + 置顶）；`hotTop5Ids.has(p.id)` → `badge-pill badge-hot`（Flame + 热门）；`hotTop5Ids` 页面加载时经新接口获取。样式对齐 HomePage 对应 class。
   - 卡片描述下方展示 `TagBadge`（最多 3 个）。
   - `authStore.isAdmin` 时卡片显示 Pin/PinOff 切换按钮（`@click.stop`，调 pin/unpin API 后本地翻转，对齐 HomePage handlePinTool）。
10. `PluginUploadPage.vue` / `PluginEditPage.vue`：接入 `TagSelector`，tags 随创建/更新请求提交。
11. `PluginDetailPage.vue`：展示 tags（TagBadge）。

## 测试与验证

- 后端 JUnit，失败测试先行（TDD）：
  - 收藏 toggle：favoriteCount ±1、score ±4、取消防护负数；summary 含 favoriteCount。
  - pin/unpin：权限（USER 403）、hot 排序置顶优先、new 排序不受影响；hot-top5 返回 ≤5 个 ID。
  - tags：创建带 tags 建关联；更新重建关联且 usageCount 正确；列表/详情返回 tags。
  - 回填：预置 favorite 行 + favoriteCount=0，initializer 后计数与 score 修正。
- 前端无测试框架：`vue-tsc` + 运行中服务（JBR21 + postgresql profile）浏览器实测卡片角标、标签、收藏数、置顶按钮。
- `make lint` 通过；提交按文末拆分方案执行，单 commit ≤1000 行。

## 提交拆分

- commit 1：后端（实体/仓库/服务/控制器/测试）
- commit 2：前端（类型/市场页/上传编辑页/详情页）
