---
title: "2026-06-13-Hot-Rankings"
type: Source
description: "引入综合热度评分 `score` 并据此排序展示工具/帖子热榜。为 `Tool` 与 `ForumPost` 增加统计字段与 `score`，新增 `ToolLike` 实体记录点赞，详情页新增点赞/评论交互。"
aliases: [热榜设计, hot-ranking-design]
title: "热榜页面优化设计"
origin: "openspec/changes/archive/2026-06-13-hot-rankings/design.md"
source_type: "md"
version: "2026-06-13"
tags: [ranking, score, openspec, design]
---
# 热榜页面优化设计

## Summary
引入综合热度评分 `score` 并据此排序展示工具/帖子热榜。为 `Tool` 与 `ForumPost` 增加统计字段与 `score`，新增 `ToolLike` 实体记录点赞，详情页新增点赞/评论交互。

## Key Points
- 评分公式：`score = viewCount*1 + likeCount*3 + commentCount*5`（评论权重最高）。
- `Tool`/`ForumPost` 增加 `viewCount/likeCount/commentCount/score`，并对 `score` 建索引优化排序。
- 新增 `ToolLike` 实体（`uk_tool_like_tool_user` 唯一约束防重复点赞）。
- 工具 API：`POST/DELETE /api/tools/{id}/like`、`/view`、`/likes`、`/like-status`；评论复用现有接口。
- 数据库迁移初始化已有数据 `score`（forum_post 用 `COALESCE` 防止 NULL）。

## Relevance
定义 [[热度评分]] 概念，是 [[概览页]] 统计/排行与 [[统一互动架构]] 点赞能力的前置基础。

## Referenced By
- [[热度评分]]
- [[概览页]]
- [[统一互动架构]]