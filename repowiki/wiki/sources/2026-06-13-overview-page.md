---
title: "2026-06-13-Overview-Page"
type: Source
description: "新增平台概览页，展示统计信息（用户数/帖子总数/工具总数）与工具热榜、帖子热榜（按类别分组）。后端 `OverviewController`/`OverviewService` 复用现有 repository；前端 `OverviewPage` 整合 `StatsCard`/`ToolRankList`/`PostRankList`/`RankItem`。"
aliases: [概览页设计, overview-page-design]
title: "概览页设计"
origin: "openspec/changes/archive/2026-06-13-overview-page/design.md"
source_type: "md"
version: "2026-06-13"
tags: [overview, openspec, design]
---
# 概览页设计

## Summary
新增平台概览页，展示统计信息（用户数/帖子总数/工具总数）与工具热榜、帖子热榜（按类别分组）。后端 `OverviewController`/`OverviewService` 复用现有 repository；前端 `OverviewPage` 整合 `StatsCard`/`ToolRankList`/`PostRankList`/`RankItem`。

## Key Points
- API：`GET /api/overview/stats`、`/tool-ranks`、`/post-ranks`（空数据返回空数组而非 null）。
- 组件状态含 loading（骨架屏）/error（重试按钮）/empty（暂无数据）。
- 设计系统：暗色玻璃态卡片 + 霓虹强调色，复用 `@lucide/vue-next` 与 Pinia。
- 响应式：<640px 单列，640-1024px 统计卡 2 列，>1024px 统计卡 3 列 + 热榜 2 列。

## Relevance
对应 [[概览页]] 模块，数据来自 [[热度评分]] 计算的热榜与全局统计。

## Referenced By
- [[概览页]]
- [[热度评分]]