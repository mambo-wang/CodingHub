---
title: "2026-06-13-Add-User-Avatar"
type: Source
description: "为 `User` 新增头像能力：后端 `UploadConfig` 扩展头像专属目录/大小/扩展名白名单，`UserService` 提供上传/删除/公开资料，`AvatarStaticController` 提供静态资源访问并做路径穿越防护。前端新增 `UserAvatar` 通用组件（URL + 首字母降级 + 哈希色兜底）。"
aliases: [头像设计, user-avatar-design]
title: "用户头像功能设计"
origin: "openspec/changes/archive/2026-06-13-add-user-avatar/design.md"
source_type: "md"
version: "2026-06-13"
tags: [user, avatar, openspec, design]
---
# 用户头像功能设计

## Summary
为 `User` 新增头像能力：后端 `UploadConfig` 扩展头像专属目录/大小/扩展名白名单，`UserService` 提供上传/删除/公开资料，`AvatarStaticController` 提供静态资源访问并做路径穿越防护。前端新增 `UserAvatar` 通用组件（URL + 首字母降级 + 哈希色兜底）。

## Key Points
- 数据库迁移 `V20260610__add_user_avatar.sql`：`ALTER TABLE user ADD COLUMN avatar_url VARCHAR(255) NULL`（老用户零迁移）。
- `AvatarUtil` 安全校验：MIME 白名单 + 扩展名黑名单（禁 svg/html/xml）+ `..` 过滤 + 用户 ID 数字校验。
- 头像 URL 形如 `/api/v1/static/avatars/{userId}.{ext}`，带 `?v={updatedAt}` 破缓存，`Cache-Control: public, max-age=3600`。
- API：`POST/DELETE /api/v1/users/me/avatar`、`GET /api/v1/users/{id}`（公开，剔除 password/email）、`GET /api/v1/static/avatars/{userId}`。
- 依赖 [[用户昵称功能设计]] 与认证框架；风险等级 L1。

## Relevance
对应 [[用户实体]] 的 `avatarUrl` 字段扩展，影响 `AuthorBadge`/`AppHeader` 等展示组件。

## Referenced By
- [[用户实体]]
- [[用户昵称功能设计]]