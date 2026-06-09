# Tasks: 嵌入式 MCP Server

**Input**: Design documents from `/specs/003-embedded-mcp-server/`

**Prerequisites**: plan.md (✅), spec.md (✅), research.md (✅), data-model.md (✅), contracts/ (✅)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: MCP Server 项目初始化和基础配置

- [x] T001 [P] Create `config/McpServerConfig.java` - MCP Server 自动配置类，在 8082 端口启动
- [x] T002 [P] Create `mcp/protocol/McpMessage.java` - MCP 消息模型（JSON-RPC 2.0）
- [x] T003 [P] Create `mcp/protocol/McpResponse.java` - MCP 响应模型
- [x] T004 [P] Create `mcp/protocol/McpError.java` - MCP 错误模型
- [x] T005 [P] Create `dto/McpSearchRequest.java` - MCP 搜索请求 DTO
- [x] T006 [P] Create `dto/ToolSearchResult.java` - 工具搜索结果 DTO
- [x] T007 [P] Create `dto/PostSearchResult.java` - 帖子搜索结果 DTO

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: MCP 核心组件和 Spring Boot 集成

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T008 [P] Create `mcp/McpServer.java` - MCP Server 核心类，管理连接和消息处理
- [x] T009 [P] Create `mcp/McpConnectionManager.java` - MCP 连接管理器（支持 SSE）
- [x] T010 [P] Create `service/McpSearchService.java` - MCP 搜索服务，封装工具和帖子检索
- [x] T011 [P] Create `controller/McpController.java` - MCP HTTP 端点（POST /mcp, GET /mcp/sse, GET /mcp/health）
- [x] T012 Create `config/McpSecurityConfig.java` - 豁免 MCP 端点的安全配置（允许 /mcp/** 无认证访问）
- [x] T013 Create `util/McpServerInitializer.java` - MCP Server 初始化器，使用 @PostConstruct 启动

**Checkpoint**: Foundation ready - MCP Server 可在 8082 端口启动并处理请求

---

## Phase 3: User Story 1 - AI IDE 连接工具广场 MCP Server (Priority: P1) 🎯 MVP

**Goal**: AI IDE 可以连接 MCP Server 并获取工具列表和检索工具

**Independent Test**: 通过 curl 发送 MCP 请求，验证工具检索返回正确结果

### Implementation for User Story 1

- [x] T016 [P] [US1] Implement tool list handler in `mcp/McpResourceHandler.java` - 返回工具列表
- [x] T017 [P] [US1] Implement `h3_coding_hub_tool_search` tool handler - 工具搜索功能
- [x] T018 [P] [US1] Implement `h3_coding_hub_tool_get` tool handler - 获取工具详情（Markdown 内容）
- [x] T019 [US1] Add input validation for tool search (query 非空，limit 1-100)
- [x] T020 [US1] Add logging for MCP tool calls

**Checkpoint**: User Story 1 fully functional - AI IDE 可搜索工具并获取工具详情

---

## Phase 4: User Story 2 - 工具文件下载 (Priority: P2)

**Goal**: AI IDE 可以获取工具的安装包信息

**Independent Test**: 请求工具文件列表，验证返回文件下载地址、大小和格式

### Implementation for User Story 2

- [x] T022 [P] [US2] Implement `h3_coding_hub_tool_files` tool handler - 获取工具文件列表
- [x] T023 [US2] Add file not found handling (返回空列表而非错误)
- [x] T024 [US2] Add file size and format in response

**Checkpoint**: User Story 2 fully functional - AI IDE 可获取工具文件下载信息

---

## Phase 5: User Story 3 - 帖子检索与内容查询 (Priority: P3)

**Goal**: AI IDE 可以搜索社区帖子并获取帖子正文

**Independent Test**: 搜索帖子并验证返回标题、摘要、作者和时间

### Implementation for User Story 3

- [x] T026 [P] [US3] Implement `h3_coding_hub_post_search` tool handler - 帖子搜索功能
- [x] T027 [P] [US3] Implement `h3_coding_hub_post_get` tool handler - 获取帖子详情（Markdown 内容）
- [x] T028 [US3] Add post not found handling (返回友好提示)
- [x] T029 [US3] Add post content summary generation (前 100 字符)

**Checkpoint**: User Story 3 fully functional - AI IDE 可搜索帖子并获取帖子内容

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: 改进和跨切割关注点

- [ ] T030 [P] Add database indexes for tool.name, post.title, post.content
- [ ] T031 [P] Add unit tests for all MCP tools in `McpServerTest.java`
- [ ] T032 Add performance test for tool search (< 500ms)
- [ ] T033 Add SSE heartbeat to keep connection alive
- [ ] T034 Add graceful shutdown handling for MCP Server
- [ ] T035 Run quickstart.md validation
- [ ] T036 Update AGENTS.md with MCP Server documentation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: ✅ Completed
- **Foundational (Phase 2)**: ✅ Completed - All user stories now available
- **User Stories (Phase 3-5)**: ✅ Completed
- **Polish (Final Phase)**: Pending

---

## Notes

- ✅ Phase 1-5 tasks completed (T001-T029)
- [ ] Tests (T014, T015, T021, T025, T031) not implemented (marked as OPTIONAL in spec)
- [ ] Polish phase (T030-T036) pending

---

## Implementation Complete

MCP Server 实现已完成，涵盖：
- MCP 协议模型（McpMessage, McpResponse, McpError）
- MCP 控制器（POST /mcp, GET /mcp/sse, GET /mcp/health）
- MCP 工具处理器（h3_coding_hub_tool_search, h3_coding_hub_tool_get, h3_coding_hub_tool_files, h3_coding_hub_post_search, h3_coding_hub_post_get）
- 安全配置豁免 /mcp/** 端点
- 自动初始化机制