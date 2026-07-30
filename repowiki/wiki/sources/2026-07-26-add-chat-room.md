---
title: "2026-07-26-Add-Chat-Room"
type: Source
description: "新增全局公共实时聊天能力（登录用户 + 游客均可收发），与既有异步 REST 互动（统一互动）在传输模型、数据归属、生命周期上明确区分：独立建模 `chat_message`，不复用 `UnifiedComment`。技术栈 WebSocket + STOMP + SimpleBroker（单实例，预留 Redis relay 扩展点）。"
aliases: [聊天室设计, chat-room-design]
origin: "openspec/changes/archive/2026-07-26-add-chat-room/design.md"
source_type: "md"
tags: [chat, websocket, real-time, openspec, design]
title: "聊天室设计"
version: "2026-07-26"
---
# 聊天室设计

## Summary
新增全局公共实时聊天能力（登录用户 + 游客均可收发），与既有异步 REST 互动（统一互动）在传输模型、数据归属、生命周期上明确区分：独立建模 `chat_message`，不复用 `UnifiedComment`。技术栈 WebSocket + STOMP + SimpleBroker（单实例，预留 Redis relay 扩展点）。

## Key Points
- 握手鉴权用查询参数 `?token=<jwt>`（`HandshakeInterceptor` 构造 `ChatPrincipal{userId?,displayName,avatarUrl?,ipHash,admin,sessionId}`），无 token 视为游客需填 displayName。
- 频率限制 `ChatService` 内存 `ConcurrentHashMap`（2s/条），限流回送错误帧不入库；管理员软删除 `status=DELETED` + 广播 `{type:DELETE}` 事件。
- 在线人数 `ChatPresenceListener` 监听连接事件广播 `{online:N}`；进房加载最近 50 条历史；`status` 仅 ACTIVE/DELETED。
- 前端双入口复用单组件 `ChatRoom.vue` + 单 store `chat.ts`：全屏 `/chat` 与全站悬浮抽屉 `ChatLauncher.vue`。

## Relevance
对应 [[聊天室]] 实体模块，定义 [[聊天室架构]] 概念；复用统一互动的 IP 哈希/XSS/软删除/JWT 做法但不复用其表。

## Referenced By
- [[聊天室]]
- [[聊天室架构]]