# 聊天室可扩展功能调研

> 调研目的：在已敲定的 MVP（单一全局公共聊天室 + STOMP + 游客昵称 + 持久化 + 频率限制 + 管理员删帖 + 在线人数）基础上，梳理业界聊天室普遍具备、可作为后续增强的功能。
> 调研时间：2026-07-25 ｜ 来源：各聊天平台官方文档与实时通信 SDK 厂商的一手资料。

## 功能域总览

| 域 | 代表功能 | 与本项目 MVP 的关系 |
|----|----------|----------------------|
| 消息基础 | 编辑/撤回、表情回应(reactions)、回复引用、富文本/Markdown | 多数可增量叠加，不破坏现有 `chat_message` 表 |
| 消息组织 | 话题串(thread)、搜索、置顶、收藏 | 需新增表或字段，属中等工程量 |
| 实时反馈 | 正在输入提示、已读回执、在线状态 | 在线状态 MVP 已做；输入/已读可加 |
| 互动通知 | @提及、推送、未读 | 未读抽屉角标 MVP 已做；@提及需解析 |
| 媒体分享 | 文件/图片上传、链接预览、GIF、语音/视频 | 文件上传需对象存储，工作量较大 |
| 结构扩展 | 多房间/频道、私聊、角色权限、机器人/斜杠命令 | 多房间已预留 `room_id`；其余为后续大特性 |
| 安全治理 | 自动审核、敏感词、封禁/隐封(shadowban)、联邦、端到端加密 | 治理类需持续投入 |
| 智能化 | AI 摘要、翻译、智能回复 | 可借本项目已有的 RAG/LLM 能力 |

---

## 1. 消息基础增强

### 1.1 编辑 / 撤回（Edit / Delete）
- 发送后允许作者在时间窗内编辑或撤回，撤回后显示「该消息已撤回」。
- 来源：CometChat 核心功能文档将消息编辑/删除列为标准能力 [1]；Rocket.Chat、Discord 均支持。
- **适配评估**：低工程量。在 `chat_message` 加 `edited` / `deleted` 标记（撤回可复用项目既有的 `status=DELETED` 软删除约定），前端按标记渲染。

### 1.2 表情回应（Reactions）
- 消息下方可叠加 emoji 计数（👍❤️😂…），不新增消息体。
- 来源：CometChat 文档将 reactions 列入核心功能 [1]；PubNub Chat 文档列为现代互动体验要素 [3]；Discord 的 reactions 是社区互动基石 [5]。
- **适配评估**：中等。需 `chat_reaction` 表（message_id, user_id, emoji, 游客按 IP/nickname），广播 `chat.reactions` 目的地。MVP 后高性价比。

### 1.3 回复引用（Quote / Reply）
- 对某一消息发起「回复」，展示被引消息摘要，形成对话上下文。
- 来源：Rocket.Chat、Discord 均支持引用回复；CometChat 核心功能含「引用」类互动 [1]。
- **适配评估**：低。在 `chat_message` 加 `reply_to`(可空) 自关联即可。

### 1.4 Markdown / 富文本 / 代码块
- 支持加粗、行内代码、代码块（对 CodingHub 这种开发者社区尤其有用）。
- 来源：开发者社区聊天普遍支持；Rocket.Chat 支持代码高亮 [4]。
- **适配评估**：低。前端渲染 Markdown（注意走 XSS 清洗，避免绕开 `XssSanitizer`）。

---

## 2. 消息组织

### 2.1 话题串（Threads）
- 在某条消息下开子讨论，不打断主频道流。
- 来源：Discord 的 Threads 是社区组织的核心 [5]；Rocket.Chat、Slack 均有 threads；开源项目 circlechat 把 threads 作为标准包 [6]。
- **适配评估**：高工程量。需 `chat_thread` 表 + 线程消息归属。建议作为「多房间之后」的下一阶段大特性。

### 2.2 消息搜索
- 按关键词/用户/时间检索历史消息。
- 来源：CometChat 核心功能含 search [1]；Ably 完整指南将搜索列为必备 [2]。
- **适配评估**：中。已有持久化表，加全文检索（PG 可用 `pgvector`/FTS，MySQL 用 `LIKE` 或全文索引）。可借本项目已有的 PG + pgvector 基建。

### 2.3 置顶 / 收藏（Pin / Bookmark）
- 管理员或作者置顶重要消息；用户收藏供自己回看。
- 来源：Slack/Discord/Rocket.Chat 均支持置顶 [4][5]。
- **适配评估**：低-中。置顶加 `pinned` 标记；收藏需 `chat_bookmark` 表。

---

## 3. 实时反馈

### 3.1 正在输入提示（Typing Indicator）
- 显示「XXX 正在输入…」，超时自动消失。
- 来源：CometChat 核心功能含 typing indicators [1]；PubNub Chat 文档明确列出 [3]；Ably 指南列为关键 UX [2]。
- **适配评估**：低。STOMP 广播 `chat.typing` 目的地，前端按 timeout 渲染。高性价比、强烈推荐 MVP 之后第一个加。

### 3.2 已读回执（Read Receipts）
- 显示消息已被多少人/谁阅读。
- 来源：CometChat 文档含 receipts [1]；Ably 指南列为标准能力 [2]。
- **适配评估**：中。公共大厅「已读」语义弱（不像 1:1），可降级为「已送达/在线人数」，价值有限，优先级低于输入提示。

---

## 4. 互动与通知

### 4.1 @提及（Mentions）
- 输入 `@昵称` 提及他人，被提及者高亮/可触发通知。
- 来源：Discord/Slack/Rocket.Chat 核心互动 [4][5]；Ably 指南列出 [2]。
- **适配评估**：中。需解析消息中的 `@`，在 `chat_message` 存 `mentions`(JSON)，并对接我们**已决定不接入**的 notification 模块——可改为仅前端高亮 + 抽屉未读角标 +1。

### 4.2 推送 / 离线通知
- 来源：CometChat 文档含 push 通知 [1]。
- **适配评估**：暂不推荐。MVP 已决定不接入 notification 系统；全局聊天推送价值低、易扰民。

---

## 5. 媒体与分享

### 5.1 文件 / 图片上传
- 来源：Rocket.Chat 支持文件附件 [4][6]；CometChat 含 media sharing [1]；Ably 指南列为标配 [2]。
- **适配评估**：高。需对象存储（本项目上传走 `Upload` 配置，可复用） + 消息类型区分（text/media）。

### 5.2 链接预览 / Embed
- 粘贴 URL 自动抓取标题/缩略图。
- 来源：Rocket.Chat 支持媒体嵌入与链接预览 [4]。
- **适配评估**：中。需服务端抓取，注意防 SSRF。

### 5.3 语音 / 视频（Voice / Video Channels）
- 来源：Discord 语音频道是核心 [5]；Rocket.Chat 含语音视频 [4]。
- **适配评估**：很高。需 WebRTC/SFU，远超文本聊天范畴，建议长期规划。

---

## 6. 结构扩展

### 6.1 多房间 / 频道（Channels）
- 来源：Rocket.Chat 以 channels 为核心 [4]；Discord 服务器=多频道 [5]；Slack 同理。
- **适配评估**：MVP 已预留 `room_id`，是**最自然的第一延伸方向**，可直接复用现有管道。

### 6.2 私聊（DM）
- 来源：Rocket.Chat/Slack 均含 DMs [4]；开源 circlechat 含 DMs [6]。
- **适配评估**：高。需 1:1 会话路由 + 隐私模型，比公共房间复杂。

### 6.3 角色与权限（Roles）
- 来源：Discord 以角色体系驱动权限 [5]；本项目已有 `USER/ADMIN/SUPER_ADMIN` 可复用。
- **适配评估**：中。可基于现有权限体系扩展聊天内角色（版主等）。

### 6.4 机器人 / 斜杠命令（Bots / Slash Commands）
- 来源：Discord 的 bots 与 slash commands 是生态核心 [5]；Rocket.Chat 支持集成。
- **适配评估**：中-高。可借本项目已有的 **MCP 服务**（18 tools）暴露为聊天机器人，是差异化亮点。

---

## 7. 安全与治理

### 7.1 自动审核（Auto-Moderation）
- 来源：2026 聊天审核指南强调「机器过滤 + 人工」组合 [7]；Discord 有 AutoMod [5]。
- **适配评估**：中。MVP 已做频率限制 + 管理员删帖，可叠加自动屏蔽规则。

### 7.2 敏感词 / 内容过滤
- 来源：同上 [7]。
- **适配评估**：低-中。MVP 决策 7 选了 B（不含敏感词），可作为后续低成本叠加。

### 7.3 封禁 / 隐封（Ban / Shadowban）
- 来源：shadowban 研究（不通知地降权）见于信息系统学期刊 [8]；Discord 社区有相关实践 [5]。
- **适配评估**：中。需用户/游客封禁表；对匿名游客需按 IP/nickname 维度。

### 7.4 联邦（Federation）与端到端加密（E2E）
- 来源：Matrix/Element 以联邦 + E2E 加密为标志 [9]。
- **适配评估**：很高。超出单站点社区需求，长期可选。

---

## 8. 智能化（可借本项目 RAG/LLM 能力）

- **AI 会话摘要**：定时总结频道讨论要点。
- **实时翻译**：跨语言聊天。
- **智能助手**：把本项目 MCP 工具接入聊天（见 6.4），让用户在聊天室里直接调用工具。
- **适配评估**：中-高，且是 CodingHub（AI 工具导航）的**差异化卖点**——聊天室不只是聊天，而是「边聊边用 AI 工具」的入口。

---

## 建议的后续路线图（基于以上调研 + 已定 MVP）

**P1（MVP 之后低成本高性价比，强烈推荐）**
1. 正在输入提示（Typing Indicator）— §3.1
2. 表情回应 Reactions — §1.2
3. 回复引用 + Markdown/代码块 — §1.3、§1.4
4. 消息编辑/撤回 — §1.1

**P2（结构化扩展）**
5. 多房间/频道（已预留 `room_id`）— §6.1
6. @提及高亮 — §4.1
7. 消息搜索（借 PG + pgvector）— §2.2
8. 置顶/收藏 — §2.3

**P3（差异化与生态）**
9. 聊天机器人接入 MCP 工具 — §6.4、§8
10. AI 会话摘要 / 翻译 — §8
11. 文件/图片上传 — §5.1

**P4（重投入，长期）**
12. 话题串 Threads — §2.1
13. 私聊 DM — §6.2
14. 语音/视频频道 — §5.3
15. 联邦 / E2E 加密 — §7.4

---

## 引用来源

[1] CometChat — Core Features (messaging, media sharing, receipts, presence, reactions, search, edit/delete). https://www.cometchat.com/docs/ui-kit/android/core-features
[2] Ably — Chat and messaging application features: The complete guide. https://ably.com/blog/chat-and-messaging-application-features
[3] PubNub — Chat API & SDK Docs (typing indicators, threaded messages, emoji). https://www.pubnub.com/docs/chat/overview
[4] Rocket.Chat — GitHub / 功能概览（channels, DMs, threads, reactions, file attachments, voice/video, link preview, 代码高亮）. https://github.com/RocketChat/Rocket.Chat
[5] Discord — 社区功能（threads, reactions, roles, bots, slash commands, voice channels, AutoMod）. 参考：https://zapier.com/blog/discord-features/ 与 Discord 官方特性说明
[6] circlechat (开源 self-hosted team chat) — channels, DMs, threads, reactions, file attachments. https://github.com/tashfeenahmed/circlechat
[7] Ethora — Chat Moderation 2026: AI, Manual & Compliance Guide. https://ethora.com/blog/chat-moderation/
[8] Information Systems Research — Content Moderation with Shadowbanning (2025). https://pubsonline.informs.org/doi/full/10.1287/isre.2024.1140
[9] Matrix / Element — 联邦 + 端到端加密（E2E）架构. https://matrix.org/
