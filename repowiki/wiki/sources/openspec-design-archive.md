---
title: OpenSpec 设计文档归档总览
type: source
---

# OpenSpec 设计文档归档总览

本页面汇总 `openspec/changes/archive/` 下 31 个已归档变更的设计文档，覆盖 CodingHub 平台从 2026-05 至 2026-07 的全部架构演进决策。

## 源文档清单

| 日期 | 变更名称 | 领域 | 关键决策 |
|------|---------|------|----------|
| 2026-05-20 | user-avatar-upload | 用户 | 头像上传存本地 uploads/avatars/ |
| 2026-05-23 | forum-module | 论坛 | 独立论坛模块，帖子/评论/分类/标签 |
| 2026-05-25 | forum-post-deletion | 论坛 | 软删除 + 级联评论标记 |
| 2026-05-27 | post-favorites | 论坛 | 帖子收藏，isOwner||isAdmin 权限 |
| 2026-05-29 | hot-posts-ranking | 论坛 | 热度公式 view×1+like×3+comment×5 |
| 2026-05-31 | overview-page | 概览 | 统计聚合 + 排行榜 |
| 2026-06-03 | auth-field-alignment | 认证 | 前后端字段对齐 |
| 2026-06-05 | mcp-tool-modify-delete | MCP | MCP 工具增删改 |
| 2026-06-07 | tool-file-format-open | 工具 | 附件格式开放 |
| 2026-06-09 | content-moderation | 审核 | XSS + 敏感词过滤 |
| 2026-06-11 | video-course-module | 微课 | 视频上传/播放/弹幕/互动 |
| 2026-06-13 | user-nickname-feature | 用户 | 昵称唯一性 + 修改限制 |
| 2026-06-15 | sort-and-pin | 工具 | 排序权重 + 置顶 |
| 2026-06-17 | user-nickname-feature-v2 | 用户 | 昵称功能迭代（同 06-13） |
| 2026-06-19 | user-role-approval | 管理 | 三级 RBAC + 审批流 |
| 2026-06-21 | tool-square-optimization | 工具 | 搜索/分类/分页优化 |
| 2026-06-23 | unified-interactions | 互动 | 多态统一互动表 |
| 2026-06-25 | feedback-module | 反馈 | 留言板 + 管理员回复 |
| 2026-06-27 | knowledge-base-module | 知识库 | RAG 知识库 CRUD |
| 2026-06-29 | kb-mcp-tools | MCP | 知识库 MCP 工具 |
| 2026-07-01 | async-batch-upload | 工具 | asyncio 并发上传 |
| 2026-07-03 | rag-direct-api | RAG | 绕过 MCP 直连 REST |
| 2026-07-05 | cover-desc-tags | 工具 | 封面/描述/标签增强 |
| 2026-07-07 | kb-ux-improvements | 知识库 | UX 改进 |
| 2026-07-09 | tool-ui-mcp-fixes | 工具 | UI 与 MCP 修复 |
| 2026-07-11 | comment-like-unification | 互动 | 评论点赞复用统一互动 |
| 2026-07-11 | tool-detail-enhancements | 工具 | 详情页增强 |
| 2026-07-21 | integrate-message-center-notifications | 通知 | 消息中心 + 未读计数 |
| 2026-07-21 | profile-my-interactions | 用户 | 个人中心互动聚合 |
| 2026-07-21 | tool-filter-by-tag | 工具 | 标签筛选 |
| 2026-07-26 | add-chat-room | 聊天 | WebSocket 实时聊天室 |
| 2026-07-26 | dual-database-config-driven | 基础 | MySQL/PG Profile 切换 |
| 2026-07-26 | rag-adaptive-chunking | RAG | 自适应分块 + 混合检索 |
| 2026-07-26 | tool-logo-and-stats | 工具 | Logo + 统计字段 |

## 涉及的核心实体

[[ForumPost]] · [[Tool]] · [[Video]] · [[User]] · [[KnowledgeBase]] · [[Tag]] · [[Notification]] · [[ChatMessage]] · [[FeedbackMessage]] · [[McpServer]] · [[RagService]] · [[ToolFile]]

## 涉及的核心概念

[[jwt-dual-token-auth]] · [[unified-interaction]] · [[soft-delete]] · [[rbac-permission]] · [[hotness-scoring]] · [[http-range-streaming]] · [[rag-adaptive-chunking]] · [[dual-database]] · [[async-batch-upload]] · [[content-moderation]] · [[notification-system]]
