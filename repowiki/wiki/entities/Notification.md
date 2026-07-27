---
title: Notification 通知
type: entity
---

# Notification 通知

## 定义

Notification 是消息中心的核心实体，记录用户收到的各类通知（点赞、评论、收藏、系统消息等）。

## 代码位置

- 实体: `backend/src/main/java/com/iaihub/toolbox/model/Notification.java`
- 服务: `backend/src/main/java/com/iaihub/toolbox/service/NotificationService.java`
- 控制器: `backend/src/main/java/com/iaihub/toolbox/controller/notification/NotificationController.java`
- 前端: `frontend/src/components/NotificationBell.vue`

## 关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| recipient | User | 接收者 |
| sender | User | 发送者（可为 null = 系统） |
| type | NotificationType | LIKE/COMMENT/FAVORITE/SYSTEM/FOLLOW |
| targetType | String | 关联目标类型 |
| targetId | Long | 关联目标 ID |
| content | String | 通知内容摘要 |
| isRead | Boolean | 是否已读 |

## 核心行为

- **触发**: 互动事件（点赞/评论/收藏）自动创建通知
- **未读计数**: `GET /api/v1/notifications/unread-count`
- **标记已读**: 单条/全部标记
- **前端轮询**: NotificationBell 组件定时拉取未读数

## API 端点

- `GET /api/v1/notifications` — 通知列表（分页）
- `GET /api/v1/notifications/unread-count` — 未读数
- `PUT /api/v1/notifications/{id}/read` — 标记已读
- `PUT /api/v1/notifications/read-all` — 全部已读

## 关联实体

[User](User.md) · [ForumPost](ForumPost.md) · [Tool](Tool.md) · [Video](Video.md)

## 设计决策来源

- integrate-message-center-notifications (2026-07-21)
