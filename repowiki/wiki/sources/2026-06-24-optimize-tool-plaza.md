---
title: "2026-06-24-Optimize-Tool-Plaza"
type: Source
description: "重做工具广场前端导航：移除 `HomePage` 左侧 `GeneralizedSidebar`，卡片网格全宽；「我的收藏」「我的工具」合并为分类 pills 行右侧的 Tab pill；pill 行最右新增上传图标，点击弹出 Modal（复用 `UploadPage` 表单）。纯前端改动，后端 API 不变。"
aliases: [工具广场优化设计, optimize-tool-plaza-design]
origin: "openspec/changes/archive/2026-06-24-optimize-tool-plaza/design.md"
source_type: "md"
tags: [tool, frontend, openspec, design]
title: "工具广场优化设计"
version: "2026-06-24"
---
# 工具广场优化设计

## Summary
重做工具广场前端导航：移除 `HomePage` 左侧 `GeneralizedSidebar`，卡片网格全宽；「我的收藏」「我的工具」合并为分类 pills 行右侧的 Tab pill；pill 行最右新增上传图标，点击弹出 Modal（复用 `UploadPage` 表单）。纯前端改动，后端 API 不变。

## Key Points
- 单页面 Tab 切换（`activeTab` ref），切换重置分页与分类筛选、调用对应 API，不触发路由变更。
- 未登录用户隐藏「我的收藏/我的工具」pill，但保留上传图标（点击跳 `/login`）。
- 删除 `/me/tools`、`/me/favorites` 路由与页面组件、`AppHeader` 中对应导航。
- 风险：Modal 与 `UploadPage` 表单逻辑重复（短期可接受，长期建议抽 `ToolUploadForm` 公共组件）。

## Relevance
对应 [工具广场](../modules/工具广场.md) 的前端导航重构，是 [[统一互动架构]] 推广 `GeneralizedSidebar` 之前的工具侧独立形态。

## Referenced By
- [工具广场](../modules/工具广场.md)