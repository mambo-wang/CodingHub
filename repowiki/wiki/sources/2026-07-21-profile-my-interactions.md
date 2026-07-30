---
title: "2026-07-21-Profile-My-Interactions"
type: Source
description: "在个人中心 `ProfilePage` 内嵌「我的评论 / 我的收藏 / 我的点赞」三个聚合标签页。后端补齐「按用户查询」接口（点赞、评论），复用收藏的 DTO 构建与软删除过滤模式，不改动任何「按目标」接口行为。"
aliases: [我的互动设计, profile-my-interactions-design]
origin: "openspec/changes/archive/2026-07-21-profile-my-interactions/design.md"
source_type: "md"
tags: [profile, interaction, openspec, design]
title: "个人中心我的互动设计"
version: "2026-07-21"
---
# 个人中心我的互动设计

## Summary
在个人中心 `ProfilePage` 内嵌「我的评论 / 我的收藏 / 我的点赞」三个聚合标签页。后端补齐「按用户查询」接口（点赞、评论），复用收藏的 DTO 构建与软删除过滤模式，不改动任何「按目标」接口行为。

## Key Points
- 「我的点赞」`GET /interactions/likes/mine?targetType=` 与「我的评论」`GET /interactions/comments/mine` 镜像 `UnifiedFavoriteService.getMyFavorites` 实现（分页 + 目标 DTO 构建 + 软删除过滤）。
- 评论返回轻量 DTO `{id,targetType,targetId,targetTitle,content,createdAt}`，后端解析目标标题，前端不二次请求。
- 仅登录用户（401）；匿名以 ip_hash 存储不属「我的」。三种类型各调一次收藏接口（首屏最多 7 并发请求）。
- 非目标：不做评论锚点定位、不新建独立路由页、不改匿名逻辑。

## Relevance
对应 [[统一互动]] 实体能力在个人中心的聚合展示，基于 [[统一互动架构]] 的统一服务。

## Referenced By
- [[统一互动]]
- [[统一互动架构]]