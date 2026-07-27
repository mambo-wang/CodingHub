---
title: User 用户
type: entity
---

# User 用户

## 定义

User 是平台认证与权限体系的核心实体，承载用户身份、角色、个人资料。

## 代码位置

- 实体: `backend/src/main/java/com/iaihub/toolbox/model/User.java`
- 仓库: `backend/src/main/java/com/iaihub/toolbox/repository/UserRepository.java`
- 服务: `backend/src/main/java/com/iaihub/toolbox/service/UserService.java`, `AuthService.java`
- 控制器: `backend/src/main/java/com/iaihub/toolbox/controller/AuthController.java`, `UserController.java`, `AdminController.java`

## 关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| username | String | 用户名（唯一） |
| email | String | 邮箱（唯一） |
| password | String | BCrypt 哈希 |
| nickname | String | 昵称（唯一，可修改） |
| avatarUrl | String | 头像路径 |
| role | UserRole | USER / ADMIN / SUPER_ADMIN |
| status | UserStatus | PENDING / ACTIVE / REJECTED |

## 核心行为

- **认证**: [[jwt-dual-token-auth]]（access 15min + refresh 7d）
- **权限**: [[rbac-permission]] 三级角色模型
- **审批**: 注册后 PENDING → 管理员审批 → ACTIVE
- **头像**: 本地存储 uploads/avatars/，限制 2MB
- **昵称**: 唯一性校验，修改冷却期
- **个人中心**: 聚合我的帖子/工具/视频/互动

## API 端点

- `POST /api/v1/auth/register` — 注册
- `POST /api/v1/auth/login` — 登录
- `POST /api/v1/auth/refresh` — 刷新令牌
- `GET /api/v1/users/profile` — 个人资料
- `PUT /api/v1/users/profile` — 修改资料
- `POST /api/v1/users/avatar` — 上传头像
- `GET /api/v1/admin/users` — 用户管理（ADMIN）
- `PUT /api/v1/admin/users/{id}/approve` — 审批

## 关联实体

[ForumPost](ForumPost.md) · [Tool](Tool.md) · [Video](Video.md) · [[Notification]] · [[FeedbackMessage]]

## 设计决策来源

- user-avatar-upload (2026-05-20)
- user-nickname-feature (2026-06-13)
- user-role-approval (2026-06-19)
- auth-field-alignment (2026-06-03)
- profile-my-interactions (2026-07-21)
