---
title: ChatMessage 聊天消息
type: entity
---

# ChatMessage 聊天消息

## 定义

ChatMessage 是实时聊天室模块的消息实体，通过 WebSocket 实现即时通讯。

## 代码位置

- 实体: `backend/src/main/java/com/iaihub/toolbox/model/chat/ChatMessage.java`
- 服务: `backend/src/main/java/com/iaihub/toolbox/service/chat/ChatService.java`
- WebSocket: `backend/src/main/java/com/iaihub/toolbox/config/WebSocketConfig.java`
- 前端: `frontend/src/pages/ChatRoomPage.vue`

## 关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| roomId | String | 房间标识 |
| sender | User | 发送者 |
| content | String | 消息内容（XSS 过滤） |
| messageType | MessageType | TEXT / SYSTEM / JOIN / LEAVE |
| createdAt | LocalDateTime | 发送时间 |

## 核心行为

- **WebSocket 连接**: STOMP over WebSocket，`/ws/chat`
- **房间管理**: 默认公共房间 + 按主题分房
- **消息广播**: SimpMessagingTemplate 向房间订阅者推送
- **历史加载**: REST API 分页获取历史消息
- **在线状态**: JOIN/LEAVE 系统消息通知
- **内容过滤**: [[content-moderation]] XSS + 敏感词

## API 端点

- `GET /api/v1/chat/messages` — 历史消息（分页）
- `GET /api/v1/chat/online` — 在线用户列表
- WebSocket: `SEND /app/chat.send` → `SUBSCRIBE /topic/chat.{roomId}`

## 关联实体

[User](User.md)

## 设计决策来源

- add-chat-room (2026-07-26)
