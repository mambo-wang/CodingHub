---
title: "2026-06-13-Forum-Favorites-And-Nav"
type: Source
description: "为论坛新增帖子收藏能力与「我的帖子 / 我的收藏」导航。后端新建 `PostFavorite` 实体及配套 repository/service/controller；前端新增 `MyPostsPage`、`MyFavoritesPage` 与左侧导航栏。"
aliases: [论坛收藏设计, forum-favorites-design]
title: "论坛收藏与导航设计"
origin: "openspec/changes/archive/2026-06-13-forum-favorites-and-nav/design.md"
source_type: "md"
version: "2026-06-13"
tags: [forum, favorite, openspec, design]
---
# 论坛收藏与导航设计

## Summary
为论坛新增帖子收藏能力与「我的帖子 / 我的收藏」导航。后端新建 `PostFavorite` 实体及配套 repository/service/controller；前端新增 `MyPostsPage`、`MyFavoritesPage` 与左侧导航栏。

## Key Points
- 新增表 `post_favorite`，收藏状态用唯一索引 `(user_id, post_id)` 防重复收藏。
- API：`POST/DELETE /api/post-favorites/{postId}`、`GET /api/post-favorites`、`GET /api/post-favorites/check/{postId}`（注意前缀为 `/api` 非 `/api/v1`）。
- 前端收藏按钮点击时校验登录态，未登录提示登录；左侧导航对未登录用户可见但点击提示登录。
- `PostCard.vue` 添加收藏按钮；`PostListPage.vue` 添加左侧导航。

## Relevance
论坛域的收藏实现，后被 [[统一互动架构]] 的通用收藏能力取代/统一，是收藏需求的早期形态。

## Referenced By
- [[论坛模块]]
- [[统一互动架构]]