---
title: "2026-06-13-Add-Post-Delete"
type: Source
description: "为论坛帖子提供前端删除入口与交互。后端删除逻辑（软删除 + 作者权限校验）已在「新增论坛模块」中实现并测试，本需求为纯前端变更：新增通用 `ConfirmDialog` 组件，在帖子详情页与「我的帖子」页挂载删除按钮。"
aliases: [帖子删除设计, post-delete-design]
title: "帖子删除功能设计"
origin: "openspec/changes/archive/2026-06-13-add-post-delete/design.md"
source_type: "md"
version: "2026-06-13"
tags: [forum, delete, openspec, design]
---
# 帖子删除功能设计

## Summary
为论坛帖子提供前端删除入口与交互。后端删除逻辑（软删除 + 作者权限校验）已在「新增论坛模块」中实现并测试，本需求为纯前端变更：新增通用 `ConfirmDialog` 组件，在帖子详情页与「我的帖子」页挂载删除按钮。

## Key Points
- 删除按钮仅当 `isLoggedIn && currentUserId === post.authorId` 时渲染。
- 新增 `ConfirmDialog.vue`：毛玻璃 + ESC 关闭 + 遮罩点击关闭 + `role=dialog`/`aria-modal` 无障碍。
- `PostCard.vue` 新增可选 `deletable` prop 与 `@delete` 事件（默认 false，向后兼容）。
- `forum` store 新增 `deletePost(id)` action，返回 `{ success, errorCode }`，errorCode ∈ {AUTH, FORBIDDEN, NOT_FOUND, UNKNOWN}。
- 风险等级 L1（修改公共组件 API）；无后端改动。

## Relevance
依赖 [[论坛模块]] 的后端删除能力，是 [[软删除状态机]] 在前端的交互体现。

## Referenced By
- [[论坛模块]]
- [[软删除状态机]]