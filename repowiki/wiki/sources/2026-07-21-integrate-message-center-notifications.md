---
title: "2026-07-21-Integrate-Message-Center-Notifications"
type: Source
description: "将已有的 `NotificationService` 三个通知方法（`createCommentNotification`/`createLikeNotification`/`createAdminNotification`）接入真实业务事件，使消息中心产生内容。评论/点赞通知在统一互动服务注入，注册审批在 `UserService` 注入，全部 try-catch 包裹为「尽力副作用」。"
aliases: [消息中心通知集成设计, message-center-notifications-design]
origin: "openspec/changes/archive/2026-07-21-integrate-message-center-notifications/design.md"
source_type: "md"
tags: [notification, openspec, design]
title: "消息中心通知集成设计"
version: "2026-07-21"
---
# 消息中心通知集成设计

## Summary
将已有的 `NotificationService` 三个通知方法（`createCommentNotification`/`createLikeNotification`/`createAdminNotification`）接入真实业务事件，使消息中心产生内容。评论/点赞通知在统一互动服务注入，注册审批在 `UserService` 注入，全部 try-catch 包裹为「尽力副作用」。

## Key Points
- 单一注入点：`UnifiedCommentService.addComment` 与 `UnifiedLikeService.toggleLike` 注入 → 一处覆盖 TOOL/FORUM_POST/VIDEO 三域，避免各域重复接线。
- 仅登录用户、不通知自己（`actorId==ownerId` 跳过）；`resolveTargetOwnerId(TargetType,targetId)` 复用仓库解析目标所有者。
- 前端枚举对齐：`LIKE`/`COMMENT_REPLY`/`ADMIN_APPROVED`/`ADMIN_REJECTED`，移除后端不存在的 `FAVORITE`/`FOLLOW`。
- 非目标：不新增类型、不改表、不引入实时推送（保持 30s 轮询）。

## Relevance
对应 [[消息通知]] 实体模块，依赖 [[统一互动架构]] 的统一服务作为事件源。

## Referenced By
- [[消息通知]]
- [[统一互动架构]]