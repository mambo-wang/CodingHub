---
title: "2026-06-24-Add-User-Role-Approval"
type: Source
description: "引入三级单角色（`USER`/`ADMIN`/`SUPER_ADMIN`）与账号状态（`ACTIVE`/`PENDING`/`REJECTED`/`DISABLED`）。`User` 表加 `role`/`status` 枚举列；`DataInitializer` 启动创建内置超管 `admin/Cloud@1234`；`JwtAuthenticationFilter` 查库读 role/stat"
aliases: [角色审批设计, user-role-approval-design]
origin: "openspec/changes/archive/2026-06-24-add-user-role-approval/design.md"
source_type: "md"
tags: [auth, role, admin, openspec, design]
title: "用户角色审批设计"
version: "2026-06-24"
---
# 用户角色审批设计

## Summary
引入三级单角色（`USER`/`ADMIN`/`SUPER_ADMIN`）与账号状态（`ACTIVE`/`PENDING`/`REJECTED`/`DISABLED`）。`User` 表加 `role`/`status` 枚举列；`DataInitializer` 启动创建内置超管 `admin/Cloud@1234`；`JwtAuthenticationFilter` 查库读 role/status 构建权限，变更立即生效。

## Key Points
- 角色存 `User.role` 枚举（`@Enumerated(STRING)`），状态存 `User.status`，正交分离「是什么角色」与「能否使用」。
- JWT 仅放 `userId`，filter 每次请求查库读 role/status → 审批/封禁后立即生效（不选把 role 放 claim 的方案）。
- 注册选 `USER`→直接激活；选 `ADMIN`→`PENDING` 待超管审批；`SUPER_ADMIN` 不可注册。
- 超管内置账号密码经 `app.super-admin.password` 配置、环境变量 `APP_SUPER_ADMIN_PASSWORD` 覆盖。
- API 统一前缀 `/api/v1/admin/**`（`pending-users`/`approve`/`reject`/`users`/`status`/`users/{id}`）。

## Relevance
定义 [[用户角色审批]] 概念与 [[用户实体]] 的 role/status 字段，是 [[内容治理权限统一设计]] 的前置。

## Referenced By
- [[用户角色审批]]
- [[用户实体]]