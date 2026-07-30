---
title: "2026-06-24-Add-Sort-And-Pin"
type: Source
description: "为工具/论坛/微课三模块统一增加「热度」「最新」两种排序与管理员置顶能力。Repository 层用 JPQL `ORDER BY pinned DESC, score DESC` 复合排序；各模块新增 `GET /{module}/hot-top5` 返回前 5 ID 列表；置顶用 `POST/DELETE /{id}/pin`。"
aliases: [排序置顶设计, sort-and-pin-design]
origin: "openspec/changes/archive/2026-06-24-add-sort-and-pin/design.md"
source_type: "md"
tags: [sort, pin, ranking, openspec, design]
title: "排序与置顶设计"
version: "2026-06-24"
---
# 排序与置顶设计

## Summary
为工具/论坛/微课三模块统一增加「热度」「最新」两种排序与管理员置顶能力。Repository 层用 JPQL `ORDER BY pinned DESC, score DESC` 复合排序；各模块新增 `GET /{module}/hot-top5` 返回前 5 ID 列表；置顶用 `POST/DELETE /{id}/pin`。

## Key Points
- 热度排序：`pinned DESC, score DESC`；最新排序：纯 `createdAt DESC`（忽略置顶）。
- `Video` 实体补全 `score` 字段与 `updateScore()`，三模块统一公式（`view*1+like*3+comment*5`）。
- `hot-top5` 独立轻量接口（返回 `List<Long>`），前端可缓存；列表查询零改动。
- 前端排序 Tab（`sortBy`）用组件内 `ref`，不进 Pinia；全局热度前 5 显示火苗、置顶显示箭头。
- 迁移：V3 脚本加 `pinned` 列（三表）+ `score` 列（video 表）+ 索引，可逆。

## Relevance
直接落地 [[热度评分]] 概念，并为 [[概览页]] 排行榜与全局热度提供数据基础。

## Referenced By
- [[热度评分]]
- [[概览页]]