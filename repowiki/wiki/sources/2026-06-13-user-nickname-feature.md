---
title: "2026-06-13-User-Nickname-Feature"
type: Source
description: "为 `User` 新增 `nickname` 字段（nullable，唯一索引），注册时填写并校验唯一性，展示时以「昵称(账号)」格式降级。前端新增 `AuthorBadge` 通用作者组件，`AppHeader` 右上角优先显示昵称。"
aliases: [昵称设计, user-nickname-design]
title: "用户昵称功能设计"
origin: "openspec/changes/archive/2026-06-13-user-nickname-feature/design.md"
source_type: "md"
version: "2026-06-13"
tags: [user, nickname, openspec, design]
---
# 用户昵称功能设计

## Summary
为 `User` 新增 `nickname` 字段（nullable，唯一索引），注册时填写并校验唯一性，展示时以「昵称(账号)」格式降级。前端新增 `AuthorBadge` 通用作者组件，`AppHeader` 右上角优先显示昵称。

## Key Points
- `User.nickname`：`@Column(length=50)`，`@Index unique`；迁移 `V20260602__add_user_nickname.sql` 加列并建唯一索引（NULL 不参与唯一约束）。
- `RegisterRequest.nickname`：2-10 位，仅中文/字母/数字/标点；`LoginResponse`/`UserDTO` 同步新增字段。
- `AuthorBadge` 显示 `昵称(账号)`；前端统一用 `nickname || username` 降级。
- 注：`2026-06-17-user-nickname-feature` 与之内容完全相同（内容抽取时判重跳过）。

## Relevance
对应 [[用户实体]] 的 `nickname` 扩展，是 [[用户头像功能设计]] 的 prerequisite。

## Referenced By
- [[用户实体]]
- [[用户头像功能设计]]