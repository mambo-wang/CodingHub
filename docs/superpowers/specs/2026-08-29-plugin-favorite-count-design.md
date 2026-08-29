# 插件市场列表页收藏数展示 — 设计文档

日期：2026-08-29 ｜ 状态：已批准 ｜ 范围：CodingHub 插件市场

## 背景

插件市场列表页卡片 stats 行目前展示点赞（Heart）、评论（MessageCircle）、浏览（Eye）三项，缺少收藏数。
插件收藏功能已存在（`UnifiedFavoriteService`，favorite 表 `target_type='PLUGIN'`），但 `Plugin` 实体没有冗余
`favoriteCount` 字段，toggle 收藏时也只维护 TOOL 的计数。用户已确认：**收藏数需计入热度分 score**。

## 方案选择

- 方案 A（采纳）：对齐工具广场 Tool 的既有模式——冗余计数列 + repository 层原子 UPDATE 同步维护计数与 score。
- 方案 B（否决）：列表查询实时 COUNT favorite 表。改动小但 score 是存储字段、"hot" 排序依赖它，收藏变化无法联动热度，与需求冲突。

## 设计

### 后端

1. **Plugin 实体**（`model/Plugin.java`）
   - 新增列 `favorite_count`（`favoriteCount`，Integer，默认 0），由 Hibernate `ddl-auto: update` 自动建列（MySQL/PG 双库兼容）。
   - `updateScore()` 公式变更为 `view×1 + like×3 + favorite×4 + comment×5`（收藏权重与 Tool 一致，插件无下载量），同步更新方法注释。
   - 新增实体方法 `incrementFavoriteCount()` / `decrementFavoriteCount()`（与其他计数方法同风格，内部调 `updateScore()`）。
2. **PluginRepository**（`repository/PluginRepository.java`）
   - 新增 `@Modifying` 原子 JPQL：`incrementFavoriteCount` / `decrementFavoriteCount`，同时调 `score ±4`，
     带 `status='NORMAL'` 守卫与 `CASE WHEN` 下限保护（完全对齐 `ToolRepository` 第 165~179 行写法）。
     规避已知教训：计数更新必须用 repository 层 @Modifying，禁止 save 读改写（并发丢更新且意外刷新 updatedAt）。
3. **UnifiedFavoriteService.toggleFavorite**
   - 现有 `if (targetType == TargetType.TOOL)` 计数块扩展为 `TOOL` / `PLUGIN` 分支，PLUGIN 走 `pluginRepository.increment/decrementFavoriteCount`。
4. **DTO 链路**
   - `PluginSummaryDTO` 加 `favoriteCount`；`PluginService.toSummaryDTO()` 与 `copySummary()` 同步补映射。
   - `PluginDetailDTO` 经 `copySummary` 继承该字段，前端详情页类型顺带可用。
5. **存量回填（幂等）**
   - `DataInitializer` 增加启动步骤：按 favorite 表 `target_type='PLUGIN'` 分组计数，与 `Plugin.favoriteCount`
     不一致的实体修正并用 `updateScore()` 重算（量少，逐条 JPA 即可，避免方言 SQL）。列刚新增时默认 0 而存量收藏>0 的插件必须回填，保证 "hot" 排序正确。

### 前端

6. `types/plugin.ts`：`PluginSummary` 加 `favoriteCount: number`。
7. `PluginMarketPage.vue`：从 `@lucide/vue` 增导 `Bookmark`；stats 行 Heart 之后插入
   `<span class="stat"><Bookmark :size="14" /> {{ fmtCount(p.favoriteCount) }}</span>`，与工具广场图标一致。
8. 详情页展示、收藏按钮态本次不做（YAGNI）。

## 测试

- 后端（先写失败测试，TDD）：扩展 `UnifiedFavoriteServiceTest`（如无则新建）——PLUGIN 收藏 toggle 后
  `favoriteCount` ±1 且 `score` ±4；取消收藏不低于 0；`PluginServiceTest` 断言 summary 含 favoriteCount；
  回填逻辑测试：预置 favorite 行 + favoriteCount=0，跑 initializer 后计数与 score 修正。
- 验证：`make lint`、后端 `gradlew test`（JBR21 JAVA_HOME）、前端 `vue-tsc`，浏览器实测列表页四 stats 展示与翻页。

## 数据与兼容性

- 新增列可空带默认，不破坏既有行；MySQL profile 本机不可用不影响（代码层双库兼容）。
- 提交拆分：后端 + 前端各一 commit，均 <1000 行，Conventional Commits（feat(plugin)）。
