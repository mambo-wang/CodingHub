---
title: FeedbackMessage 留言反馈
type: entity
---

# FeedbackMessage 留言反馈

## 定义

FeedbackMessage 是留言板模块的实体，支持用户留言和管理员回复的树形结构。

## 代码位置

- 实体: `backend/src/main/java/com/iaihub/toolbox/model/FeedbackMessage.java`
- 服务: `backend/src/main/java/com/iaihub/toolbox/service/FeedbackService.java`
- 控制器: `backend/src/main/java/com/iaihub/toolbox/controller/feedback/FeedbackController.java`
- 前端: `frontend/src/pages/feedback/`, `frontend/src/components/feedback/`

## 关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| author | User | 留言者 |
| content | String | 留言内容 |
| parentId | Long | 父留言 ID（回复场景） |
| isAdminReply | Boolean | 是否管理员回复 |
| status | FeedbackStatus | ACTIVE / DELETED |

## 核心行为

- **留言**: 用户发布留言，经 [[content-moderation]] 过滤
- **回复**: 管理员可回复，形成树形结构
- **权限**: 删除需 isOwner || isAdmin（[[rbac-permission]]）
- **软删除**: [[soft-delete]] 策略

## API 端点

- `GET /api/v1/feedback` — 留言列表（分页）
- `POST /api/v1/feedback` — 发布留言
- `POST /api/v1/feedback/{id}/reply` — 管理员回复
- `DELETE /api/v1/feedback/{id}` — 删除

## 关联实体

[User](User.md)

## 设计决策来源

- feedback-module (2026-06-25)
