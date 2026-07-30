---
title: "2026-06-24-Unify-Interactions"
type: Source
description: "将工具/论坛/微课三模块各自重复的 10 张点赞/评论/收藏表合并为 3 张通用表（`unified_like`/`unified_comment`/`unified_favorite`），统一 API 为 `/api/v1/interactions/*`，一套 Controller+Service 服务三模块。前端统一 `GeneralizedSidebar` 导航与 `UnifiedLikeB"
aliases: [统一互动设计, unify-interactions-design]
origin: "openspec/changes/archive/2026-06-24-unify-interactions/design.md"
source_type: "md"
tags: [interaction, unified, openspec, design]
title: "统一互动设计"
version: "2026-06-24"
---
# 统一互动设计

## Summary
将工具/论坛/微课三模块各自重复的 10 张点赞/评论/收藏表合并为 3 张通用表（`unified_like`/`unified_comment`/`unified_favorite`），统一 API 为 `/api/v1/interactions/*`，一套 Controller+Service 服务三模块。前端统一 `GeneralizedSidebar` 导航与 `UnifiedLikeButton`/`UnifiedCommentSection`/`UnifiedFavoriteButton` 组件。

## Key Points
- 多态设计：`target_type VARCHAR(20) + target_id BIGINT`（枚举 TOOL/FORUM_POST/VIDEO），无数据库级外键，应用层校验存在性。
- 砍掉评论点赞（unified_like 只服务内容主体）；主表保留冗余 `likeCount/commentCount`，Service 同步更新。
- 匿名点赞用 `ip_hash = SHA256(remoteAddr)`（登录时 `user_id` 非空、`ip_hash` 空）；嵌套评论用 `parent_id`/`root_id`。
- 一次性 SQL 迁移 10→3 张表，旧表 `RENAME` 为 `*_deprecated`（不 DROP）；MCP 工具不绕经 InteractionController，无需改动。
- 禁忌：新增互动功能禁止重复造轮子，复用 `Unified*Service` + 扩展 `TargetType` 枚举。

## Relevance
定义 [[统一互动架构]] 概念与 [[统一互动]] 实体实现，是平台横向能力的核心重构。

## Referenced By
- [[统一互动架构]]
- [[统一互动]]