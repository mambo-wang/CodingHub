---
title: "2026-06-24-Add-Content-Moderation"
type: Source
description: "将工具/帖子/微课三类内容的删除与编辑权限从「仅创建者」扩展为「创建者或管理员（ADMIN/SUPER_ADMIN）」。三个 Service 的 `deleteXxx/updateXxx` 签名由 `Long userId` 改为 `User user`，权限判断统一为 `canModify = isOwner || isAdmin`，前端抽取 `useContentPermissions` co"
aliases: [内容治理权限设计, content-moderation-design]
origin: "openspec/changes/archive/2026-06-24-add-content-moderation/design.md"
source_type: "md"
tags: [permission, admin, openspec, design]
title: "内容治理权限统一设计"
version: "2026-06-24"
---
# 内容治理权限统一设计

## Summary
将工具/帖子/微课三类内容的删除与编辑权限从「仅创建者」扩展为「创建者或管理员（ADMIN/SUPER_ADMIN）」。三个 Service 的 `deleteXxx/updateXxx` 签名由 `Long userId` 改为 `User user`，权限判断统一为 `canModify = isOwner || isAdmin`，前端抽取 `useContentPermissions` composable。

## Key Points
- 权限对称：`canEdit === canDelete`（能删就能改），简化前端按钮逻辑。
- Service 直接读 `user.getRole()`，避免 `SecurityContextHolder` 隐式依赖；删除走 `ConfirmDialog` 二次确认，仍为软删除（可恢复）。
- 前端列表卡片 `opacity:0.35` → hover 高亮；无权限不显示按钮（后端仍独立校验，前端被绕过也拦截）。
- 帖子编辑复通 `PostEditorPage`（`/forum/posts/:id/edit`）；新增微课 `VideoEditPage`（仅改 title/description，不替换视频文件）。
- 不动 MCP 工具权限、不引入新角色模型、不改 `SecurityConfig` URL 级规则。

## Relevance
建立在 [[用户角色审批]] 的三级角色体系之上，是内容层治理能力的实现；与 [[软删除状态机]] 配合保证可恢复。

## Referenced By
- [[用户角色审批]]
- [[软删除状态机]]