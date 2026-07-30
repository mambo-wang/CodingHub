---
title: "2026-07-21-Tool-Filter-By-Tag"
type: Source
description: "为工具广场增加「按标签筛选」能力：`GET /api/v1/tools` 支持可选 `tagId` 参数（与 `categoryId`/`keyword` 叠加），前端搜索栏加标签下拉框、卡片 `TagBadge` 可点击触发筛选，MCP `tool_search` 新增 `tag` 名称参数内存过滤。"
aliases: [工具按标签筛选设计, tool-filter-by-tag-design]
origin: "openspec/changes/archive/2026-07-21-tool-filter-by-tag/design.md"
source_type: "md"
tags: [tag, filter, tool, openspec, design]
title: "工具按标签筛选设计"
version: "2026-07-21"
---
# 工具按标签筛选设计

## Summary
为工具广场增加「按标签筛选」能力：`GET /api/v1/tools` 支持可选 `tagId` 参数（与 `categoryId`/`keyword` 叠加），前端搜索栏加标签下拉框、卡片 `TagBadge` 可点击触发筛选，MCP `tool_search` 新增 `tag` 名称参数内存过滤。

## Key Points
- 查询策略选 `EXISTS (SELECT 1 FROM ToolTag tt WHERE tt.toolId=t.id AND tt.tagId=:tagId)` 子查询，不动现有 `findByFilters` 结构，对无 tagId 调用零影响。
- 前端下拉单选（收起显示「标签: 全部标签」），`TagBadge` 用 `clickable` prop 控制是否可点击触发筛选。
- MCP `tool_search` 新增 `tag`（名称，忽略大小写），复用 `resolveTagsForTools` 批量解析后内存过滤，不新增 Repository 方法。
- 非目标：不支持多标签 AND/OR、不改标签 CRUD / MCP 搜索接口、不引搜索引擎。

## Relevance
对应 [[标签系统]] 的查询侧能力，作用于 [工具广场](../modules/工具广场.md) 筛选。

## Referenced By
- [[标签系统]]
- [工具广场](../modules/工具广场.md)