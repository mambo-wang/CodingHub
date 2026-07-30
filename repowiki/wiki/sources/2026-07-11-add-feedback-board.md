---
title: "2026-07-11-Add-Feedback-Board"
type: Source
description: "新增轻量留言板：用户可匿名提交留言（内容必填，昵称/联系方式/分类可选），提交后公开倒序展示；管理员可回复与软删除。单表 `feedback_message`，管理员回复写在同表 `admin_reply/replied_by/replied_at` 字段，不建独立回复表。"
aliases: [留言反馈板设计, feedback-board-design]
origin: "openspec/changes/archive/2026-07-11-add-feedback-board/design.md"
source_type: "md"
tags: [feedback, openspec, design]
title: "留言反馈板设计"
version: "2026-07-11"
---
# 留言反馈板设计

## Summary
新增轻量留言板：用户可匿名提交留言（内容必填，昵称/联系方式/分类可选），提交后公开倒序展示；管理员可回复与软删除。单表 `feedback_message`，管理员回复写在同表 `admin_reply/replied_by/replied_at` 字段，不建独立回复表。

## Key Points
- 独立实现匿名模式（参考 `UnifiedInteractionController.getCurrentUser()`，有 JWT 关联 userId，无则 ipHash），不接入统一互动系统（`TargetType` 不新增）。
- API 走 `/api/v1/feedback`（新模块统一 v1 前缀，与论坛 `/api/forum` 历史遗留不同）。
- `category` 用 VARCHAR 存枚举（`SUGGESTION/BUG_REPORT/PRAISE/OTHER`），后端 Java 枚举校验。
- `status` 仅 `NORMAL/DELETED`；迁移 `V8__create_feedback_table.sql`，无破坏性变更。

## Relevance
对应 [留言反馈](../modules/留言反馈.md) 实体模块；匿名模式与 [[统一互动架构]] 思路一致但未复用其表。

## Referenced By
- [留言反馈](../modules/留言反馈.md)
- [[统一互动架构]]