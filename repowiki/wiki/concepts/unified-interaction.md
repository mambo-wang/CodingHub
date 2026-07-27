---
title: 统一互动多态设计
type: concept
---

# 统一互动多态设计

## 定义

通过单一多态表 `unified_interaction` 管理所有类型内容（工具/帖子/视频/评论）的点赞、收藏、分享等互动行为，避免为每种内容类型创建独立互动表。

## 表结构

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| user_id | Long | 操作用户（登录用户） |
| ip_hash | String | 匿名用户的 IP 哈希 |
| target_type | Enum | TOOL / FORUM_POST / VIDEO / FORUM_COMMENT / VIDEO_COMMENT |
| target_id | Long | 目标内容 ID |
| interaction_type | Enum | LIKE / FAVORITE / SHARE |
| created_at | DateTime | 操作时间 |

## 设计决策

### 为什么用多态表而非独立表？

1. **扩展性**: 新增内容类型只需加 Enum 值，无需建表
2. **统一查询**: 个人中心“我的互动”一次查询即可聚合所有类型
3. **代码复用**: 一个 Service 处理所有互动逻辑

### 匿名互动支持

- 未登录用户通过 `ip_hash`（SHA-256(IP + salt)）标识
- 同一 IP 对同一目标只能互动一次
- 登录后 ip_hash 记录可合并到 user_id

### 计数维护

- 互动时同步更新目标实体的 likeCount/favoriteCount
- 取消互动时递减
- 事务保证一致性

## API 端点

- `POST /api/v1/interactions` — 创建互动
- `DELETE /api/v1/interactions` — 取消互动
- `GET /api/v1/interactions/check` — 检查是否已互动
- `GET /api/v1/interactions/my` — 我的互动列表

## 关联页面

[Tool](../entities/Tool.md) · [ForumPost](../entities/ForumPost.md) · [Video](../entities/Video.md) · [User](../entities/User.md) · [Notification](../entities/Notification.md)

## 设计决策来源

- unified-interactions (2026-06-23)
- comment-like-unification (2026-07-11)
- profile-my-interactions (2026-07-21)
