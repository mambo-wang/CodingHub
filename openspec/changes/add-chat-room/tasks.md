## 1. 依赖与基础配置

- [ ] 1.1 后端 `backend/build.gradle` 新增 `implementation 'org.springframework.boot:spring-boot-starter-websocket'`
- [ ] 1.2 前端 `frontend/package.json` 新增依赖 `@stomp/stompjs` 并 `npm install`
- [ ] 1.3 新增 `config/WebSocketConfig`：`@EnableWebSocketMessageBroker`，`enableSimpleBroker("/topic")`、`setApplicationDestinationPrefixes("/app")`，注册端点 `/ws`（`setAllowedOriginPatterns("*")`，不启用 SockJS），挂载握手拦截器
- [ ] 1.4 `config/SecurityConfig` 放行 `/ws/**` 与 `GET /api/v1/chat/messages`
- [ ] 1.5 后端单元测试：`WebSocketConfig` 加载与 broker/前缀配置断言（Spring context 测试）

## 2. 数据模型与仓储

- [ ] 2.1 新增实体 `model/ChatMessage`：`id / roomId(默认 global) / userId(可空) / displayName / avatarUrl(可空) / content / status(ACTIVE|DELETED) / createdAt`（JPA 注解，`ddl-auto: update` 自动建表）
- [ ] 2.2 新增 `repository/ChatMessageRepository`：查询 `roomId + status=ACTIVE` 按 `createdAt` 倒序取 `limit` 条（供历史加载）；软删除更新方法
- [ ] 2.3 后端单元测试：`ChatMessageRepository` 的最近 N 条查询与仅返回 ACTIVE 的断言（`@DataJpaTest`）

## 3. 握手鉴权与身份

- [ ] 3.1 新增 `config/ChatHandshakeInterceptor`：从查询参数 `?token=` 取 JWT，复用 `JwtUtil.validateToken/getUserIdFromToken` 解析；有效则加载 `User` 构造登录身份，否则视为游客；捕获客户端 IP 并计算 `ipHash`（复用现有 IP 哈希做法）
- [ ] 3.2 新增 `ChatPrincipal`（承载 `userId/displayName/avatarUrl/ipHash/admin/sessionId`），存入握手属性/Principal
- [ ] 3.3 后端单元测试：拦截器对有效/无效/缺失 token 分别构造登录/游客身份的断言

## 4. 聊天服务与消息处理

- [ ] 4.1 新增 `service/ChatService`：`handleMessage(principal, payload)` — 频率限制（`u:{userId}`/`ip:{ipHash}`，2s 窗口，`ConcurrentHashMap`）→ 非空与 ≤1000 校验 → `XssSanitizer.sanitize()` 净化正文与游客昵称 → 持久化 → 经 `SimpMessagingTemplate` 广播到 `/topic/chat.{roomId}`
- [ ] 4.2 `ChatService` 历史查询方法：返回最近 50 条 ACTIVE（正序）
- [ ] 4.3 `ChatService` 软删除方法：置 `status=DELETED` 并广播 `{type:"DELETE", id}`
- [ ] 4.4 命中限流/校验失败时通过 user-queue 向发送者回送错误帧（不入库、不广播）
- [ ] 4.5 后端单元测试：限流命中/放行、超长拒绝、空白拒绝、XSS 净化、软删除广播 的断言（mock `SimpMessagingTemplate` 与 repository）

## 5. WebSocket 与 REST 端点

- [ ] 5.1 新增 `controller/ChatWsController`：`@MessageMapping("/chat.send")` 调用 `ChatService.handleMessage`
- [ ] 5.2 新增 `controller/ChatController`：`GET /api/v1/chat/messages?roomId&limit`（公开，历史）；`DELETE /api/v1/chat/messages/{id}`（仅 ADMIN/SUPER_ADMIN，软删除）
- [ ] 5.3 新增 `config/ChatPresenceListener`：监听 `SessionConnectedEvent`/`SessionDisconnectEvent`，维护线程安全在线计数，变化时广播到 `/topic/chat.presence`
- [ ] 5.4 后端单元测试：`ChatController` 历史返回与删除鉴权（非管理员 403）的 `@WebMvcTest`；presence 计数增减断言

## 6. 前端类型、服务与 Store

- [ ] 6.1 新增 `types/chat.ts`：`ChatMessage`、`PresencePayload`、`DeleteEvent`、`SendPayload` 等类型
- [ ] 6.2 新增 `services/chat.ts`：封装 `GET /api/v1/chat/messages`、`DELETE /api/v1/chat/messages/{id}`
- [ ] 6.3 新增 `stores/chat.ts`（Pinia）：管理单一 STOMP 连接（`@stomp/stompjs`，携带 `?token=`，Enter 发送/Shift+Enter 换行的发送方法）、消息列表、在线人数、未读计数（抽屉关闭累加、打开清零）、自动重连 + STOMP 心跳；处理 DELETE 与 presence 事件

## 7. 前端组件与入口

- [ ] 7.1 新增 `components/chat/ChatRoom.vue`：消息列表（气泡区分自己/他人/游客）、输入框（限流禁用态、超长提示）、在线人数、连接状态、空态/加载/错误态；双主题 + 可访问性（依 design-system.md）
- [ ] 7.2 新增 `pages/ChatPage.vue`（`/chat` 全屏），内嵌 `ChatRoom.vue`
- [ ] 7.3 新增 `components/chat/ChatLauncher.vue`：全站右下角悬浮按钮 + 侧滑抽屉（`role="dialog"`、Esc 关闭、未读角标），内嵌 `ChatRoom.vue`
- [ ] 7.4 `router/index.ts` 注册 `/chat` 路由；`App.vue` 全局挂载 `ChatLauncher.vue`；导航栏增加"聊天室"入口
- [ ] 7.5 管理员在消息气泡 hover 显示删除按钮，调用软删除接口

## 8. 联调与验证

- [ ] 8.1 端到端联调：登录用户与游客在 `/chat` 与悬浮抽屉双入口实时收发、历史加载、在线人数、限流、管理员删除、断线重连
- [ ] 8.2 MySQL 与 PostgreSQL 双库分别验证 `chat_message` 自动建表与读写
- [ ] 8.3 暗/亮双主题切换视觉与可访问性（焦点环、键盘、`aria-live`）检查
- [ ] 8.4 运行 `make lint`（lint-arch + lint-quality + lint-deps）确认无新增违规

## N. 受影响模块回归测试（基于 impact-analysis.md）

> impact-analysis.md 已跳过：本变更为**全新独立模块**（新增 controller/service/repository/model/config 与前端页面/组件/store），仅对 `SecurityConfig`（放行新路径）、`build.gradle`/`package.json`（加依赖）、`App.vue`/`router`（挂载入口）做**新增式**改动，不修改现有能力的既有行为，故无需专门的受影响模块回归清单。

- [ ] N.1 冒烟验证：现有登录/鉴权流程与 `JwtAuthenticationFilter` 不受 `SecurityConfig` 放行新增路径影响（L1 风险 — 改动了安全放行规则）
