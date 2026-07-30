---
title: "2026-06-13-Add-Forum-Module"
type: Source
description: "为平台引入完整的论坛子系统，覆盖帖子、分类、标签、评论（楼中楼）、点赞。后端按 controller/service/repository/model/dto 分层新建 forum 子包；前端新增 forum 页面、组件、store、service、types。数据库新增 forum_category / forum_tag / forum_post / forum_post_tag / foru"
aliases: [论坛模块设计, forum-module-design]
title: "新增论坛模块设计"
origin: "openspec/changes/archive/2026-06-13-add-forum-module/design.md"
source_type: "md"
version: "2026-06-13"
tags: [forum, openspec, design]
---
# 新增论坛模块设计

## Summary
为平台引入完整的论坛子系统，覆盖帖子、分类、标签、评论（楼中楼）、点赞。后端按 controller/service/repository/model/dto 分层新建 forum 子包；前端新增 forum 页面、组件、store、service、types。数据库新增 forum_category / forum_tag / forum_post / forum_post_tag / forum_comment / forum_like 六张表。

## Key Points
- 帖子状态机：`status ENUM('NORMAL','DELETED','HIDDEN')`，删除采用软删除（见 [[软删除状态机]]）。
- 评论采用楼中楼结构：`parent_id` + `root_id` 双字段定位，支持嵌套回复。
- 匿名评论通过 `HttpServletRequest.getRemoteAddr()` 取 IP 并 SHA-256 hash 存储，首次评论需填昵称。
- 论坛 API 前缀为 `/api/forum/...`（不含 `/v1`，与核心 API 不同）。
- 前端 Markdown 渲染需识别 `/tools/\d+` 链接与外部链接标记；编辑器用 `@tiptap/vue-3`。

## Relevance
本项目核心内容域之一，对应 [[论坛模块]] 实体实现；为后续 [[统一互动架构]]、收藏、置顶等需求提供基础。

## Referenced By
- [[论坛模块]]
- [[软删除状态机]]