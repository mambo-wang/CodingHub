---
title: "2026-06-13-Update-Auth-Fields"
type: Source
description: "调整认证相关字段与逻辑：`User` 增加 `username` 唯一索引；`LoginRequest` 的 `email` 改为 `username`，支持用户名/邮箱自动判断；`RegisterRequest` 密码校验简化为长度 ≥ 6；`AuthService.login` 支持用户名查找。"
aliases: [认证字段设计, auth-fields-design]
title: "认证字段更新设计"
origin: "openspec/changes/archive/2026-06-13-update-auth-fields/design.md"
source_type: "md"
version: "2026-06-13"
tags: [auth, openspec, design]
---
# 认证字段更新设计

## Summary
调整认证相关字段与逻辑：`User` 增加 `username` 唯一索引；`LoginRequest` 的 `email` 改为 `username`，支持用户名/邮箱自动判断；`RegisterRequest` 密码校验简化为长度 ≥ 6；`AuthService.login` 支持用户名查找。

## Key Points
- `LoginRequest` 字段 `email` → `username`，登录时按用户名或邮箱自动识别。
- `RegisterRequest` 移除复杂密码规则，仅保留 `password` 长度 ≥ 6。
- `User.username` 设 `unique=true`；`Category` 预置数据移除 API 类型。
- 覆盖后端 DTO/Service/Controller/Repository 与前端 `auth.ts`/`Login.vue`/`Register.vue`/types。

## Relevance
奠定 [[用户实体]] 的认证字段基础，影响后续 [[用户昵称功能设计]]、[[用户头像功能设计]] 与 [[用户角色审批]]。

## Referenced By
- [[用户实体]]
- [[用户昵称功能设计]]