---
title: "Openspec知识抽取总览"
type: Concept
description: "> 本页从 `openspec/changes/archive/` 下 32 个需求的 `design.md` 抽取结构化知识。源码导入 31 篇（2026-06-17-user-nickname 与 06-13 重复，判重跳过）。"
aliases: [OpenSpec知识图谱, 需求设计知识总览, openspec-knowledge-hub]
domain: "knowledge-hub"
tags: [openspec, knowledge, hub]
title: "OpenSpec 需求设计知识抽取总览"
---
# OpenSpec 需求设计知识抽取总览

> 本页从 `openspec/changes/archive/` 下 32 个需求的 `design.md` 抽取结构化知识。源码导入 31 篇（2026-06-17-user-nickname 与 06-13 重复，判重跳过）。

## 内容域模块（实体）
- [论坛模块](../entities/论坛模块.md) · [工具广场](../entities/工具广场.md) · [微课视频](../entities/微课视频.md) · [知识库](../entities/知识库.md) · [留言反馈](../entities/留言反馈.md) · [聊天室](../entities/聊天室.md)

## 横向能力（实体）
- [统一互动](../entities/统一互动.md) · [消息通知](../entities/消息通知.md) · [内容审核](../entities/内容审核.md) · [MCP工具集](../entities/MCP工具集.md) · [标签系统](../entities/标签系统.md) · [双数据库](../entities/双数据库.md)

## 核心模型（实体）
- [用户实体](../entities/用户实体.md)

## 关键概念（架构/算法/决策）
- [软删除状态机](软删除状态机.md) · [热度评分](热度评分.md) · [用户角色审批](用户角色审批.md) · [排序与置顶](排序与置顶.md) · [统一互动架构](统一互动架构.md)
- [RAG自适应分块](RAG自适应分块.md) · [异步批量上传](异步批量上传.md) · [RAG直连架构](RAG直连架构.md) · [双库配置驱动](双库配置驱动.md) · [聊天室架构](聊天室架构.md)

## 源文档摘要（按时间）
- 2026-06-13：[2026-06-13-add-forum-module](../sources/2026-06-13-add-forum-module.md) · [2026-06-13-add-post-delete](../sources/2026-06-13-add-post-delete.md) · [2026-06-13-add-user-avatar](../sources/2026-06-13-add-user-avatar.md) · [2026-06-13-hot-rankings](../sources/2026-06-13-hot-rankings.md) · [2026-06-13-forum-favorites-and-nav](../sources/2026-06-13-forum-favorites-and-nav.md) · [2026-06-13-update-auth-fields](../sources/2026-06-13-update-auth-fields.md) · [2026-06-13-overview-page](../sources/2026-06-13-overview-page.md) · [2026-06-13-user-nickname-feature](../sources/2026-06-13-user-nickname-feature.md)
- 2026-06-15：[2026-06-15-add-mcp-tool-modify-delete](../sources/2026-06-15-add-mcp-tool-modify-delete.md) · [2026-06-15-allow-any-tool-attachment-format](../sources/2026-06-15-allow-any-tool-attachment-format.md)
- 2026-06-24：[2026-06-24-add-content-moderation](../sources/2026-06-24-add-content-moderation.md) · [2026-06-24-add-sort-and-pin](../sources/2026-06-24-add-sort-and-pin.md) · [2026-06-24-add-user-role-approval](../sources/2026-06-24-add-user-role-approval.md) · [2026-06-24-add-micro-course](../sources/2026-06-24-add-micro-course.md) · [2026-06-24-optimize-tool-plaza](../sources/2026-06-24-optimize-tool-plaza.md) · [2026-06-24-unify-interactions](../sources/2026-06-24-unify-interactions.md)
- 2026-07-11：[2026-07-11-add-feedback-board](../sources/2026-07-11-add-feedback-board.md) · [2026-07-11-add-knowledge-base](../sources/2026-07-11-add-knowledge-base.md) · [2026-07-11-add-knowledge-mcp-tools](../sources/2026-07-11-add-knowledge-mcp-tools.md) · [2026-07-11-async-batch-upload](../sources/2026-07-11-async-batch-upload.md) · [2026-07-11-cover-description-tags](../sources/2026-07-11-cover-description-tags.md) · [2026-07-11-direct-rag-document-api](../sources/2026-07-11-direct-rag-document-api.md) · [2026-07-11-kb-ux-improvements](../sources/2026-07-11-kb-ux-improvements.md) · [2026-07-11-tool-ui-and-mcp-fixes](../sources/2026-07-11-tool-ui-and-mcp-fixes.md)
- 2026-07-21：[2026-07-21-integrate-message-center-notifications](../sources/2026-07-21-integrate-message-center-notifications.md) · [2026-07-21-profile-my-interactions](../sources/2026-07-21-profile-my-interactions.md) · [2026-07-21-tool-filter-by-tag](../sources/2026-07-21-tool-filter-by-tag.md)
- 2026-07-26：[2026-07-26-add-chat-room](../sources/2026-07-26-add-chat-room.md) · [2026-07-26-dual-database-config-driven](../sources/2026-07-26-dual-database-config-driven.md) · [2026-07-26-rag-adaptive-chunking](../sources/2026-07-26-rag-adaptive-chunking.md) · [2026-07-26-tool-logo-and-stats](../sources/2026-07-26-tool-logo-and-stats.md)

## 跨模块主线
- 互动能力收敛：各域自实现（[论坛模块](../entities/论坛模块.md)收藏、[微课视频](../entities/微课视频.md)互动）→ 统一为 [统一互动架构](统一互动架构.md)，并以单一注入点驱动 [消息通知](../entities/消息通知.md)。
- RAG 质量演进：[知识库](../entities/知识库.md) 经 [异步批量上传](异步批量上传.md) + [RAG直连架构](RAG直连架构.md) + [RAG自适应分块](RAG自适应分块.md) 形成完整管线。
- 治理与安全：[用户角色审批](用户角色审批.md) 支撑 [内容审核](../entities/内容审核.md) 的 isOwner||isAdmin 模型，叠加 [软删除状态机](软删除状态机.md) 保证可恢复。