# Feature Specification: 嵌入式 MCP Server

**Feature Branch**: `[003-embedded-mcp-server]`

**Created**: 2026-05-31

**Status**: Clarification Complete

## Clarifications

### Session 2026-05-31

- Q: MCP Server 通信协议选择 → A: HTTP + SSE (Server-Sent Events) - 便于调试，支持服务端推送，适合 IDE 集成
- Q: 认证机制选择 → A: 无需认证 - MCP Server 为内嵌服务，部署在内网环境，无需额外认证
- Q: MCP 工具命名风格 → A: H3CodingHub - MCP 工具使用项目英文名称作为前缀，保持统一风格
- Q: 数据规模预期 → A: 100-500 个工具，500-2000 个帖子 - 适合中小型社区平台的初期规模
- Q: 工具文件格式 → A: 支持多种格式（ZIP、JSON、NPM 包等），根据工具广场已有实现返回

## User Scenarios & Testing *(mandatory)*

### User Story 1 - AI IDE 连接工具广场 MCP Server (Priority: P1)

AI 编程 IDE（如 VS Code、Cursor 等）在配置阶段，通过 MCP 协议连接工具广场后端服务，获取工具和帖子的检索能力。

**Why this priority**: 这是核心场景，没有 MCP 连接就无法实现任何 AI 增强功能。

**Independent Test**: 可以通过任何 MCP 客户端（如 Cody、Cursor）连接 `localhost:8082` 并验证工具检索功能是否正常。

**Acceptance Scenarios**:

1. **Given** AI IDE 已配置 MCP Server 地址为 `http://localhost:8082`，**When** 开发者打开 AI 辅助面板，**Then** IDE 能成功建立 MCP 连接并获取可用工具列表
2. **Given** MCP 连接已建立，**When** 开发者在 IDE 中输入 "搜索图片处理工具"，**Then** 系统返回工具名称、描述和分类信息
3. **Given** MCP 连接已建立，**When** 开发者选中某个工具并请求详情，**Then** 系统返回工具的完整 Markdown 文档内容

---

### User Story 2 - 工具文件下载 (Priority: P2)

AI IDE 可以通过 MCP 接口获取工具的安装包文件，供开发者在本地安装使用。

**Why this priority**: 工具文件下载是工具广场的核心价值之一，让用户真正使用到工具。

**Independent Test**: 可以通过 MCP 协议请求指定工具的下载链接和文件信息，然后通过标准 HTTP 下载文件。

**Acceptance Scenarios**:

1. **Given** MCP 连接已建立，**When** 开发者请求某个工具的安装包信息，**Then** 系统返回文件下载地址、大小和格式
2. **Given** 获取到下载地址，**When** IDE 执行文件下载操作，**Then** 文件能正确下载到本地

---

### User Story 3 - 帖子检索与内容查询 (Priority: P3)

AI IDE 能够通过 MCP 接口搜索社区帖子，获取问题解答和使用指南。

**Why this priority**: 帖子检索为 AI 提供上下文支持，帮助 AI 给出更准确的回答。

**Independent Test**: 可以通过 MCP 客户端搜索帖子标题或内容关键词，验证返回结果的相关性。

**Acceptance Scenarios**:

1. **Given** MCP 连接已建立，**When** 开发者搜索 "如何使用这个工具"，**Then** 系统返回相关帖子列表，包含标题、摘要和发布时间
2. **Given** 搜索到帖子，**When** 开发者请求查看帖子正文，**Then** 系统返回帖子的完整 Markdown 内容

---

### Edge Cases

- 当 MCP 客户端请求的工具 ID 不存在时，系统应返回明确的错误信息而非空数据
- 当工具没有关联文件时，下载接口应返回空列表而非错误
- 当帖子内容为空或已被删除时，应返回友好提示
- MCP 连接异常中断后，客户端能自动重连

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系统 MUST 在 Spring Boot 应用启动时自动启动内嵌 MCP Server
- **FR-002**: MCP Server MUST 支持 HTTP + SSE (Server-Sent Events) 通信方式
- **FR-003**: MCP Server MUST 提供工具检索功能，支持按名称、分类、关键词搜索
- **FR-004**: MCP Server MUST 提供工具 Markdown 文档内容查询功能
- **FR-005**: MCP Server MUST 提供工具文件下载信息查询功能
- **FR-006**: MCP Server MUST 提供帖子检索功能，支持按标题、内容搜索
- **FR-007**: MCP Server MUST 提供帖子内容查询功能，返回完整正文
- **FR-008**: MCP Server MUST 支持同时处理多个并发连接
- **FR-009**: MCP Server 异常时不应影响主应用功能，应优雅降级
- **FR-010**: MCP Server 无需身份验证，AI IDE 可直接连接（部署于内网环境）

### Key Entities

- **Tool（工具）**: 代表工具广场中的单个工具，包含名称、描述、分类、Markdown 文档、文件列表
- **Post（帖子）**: 代表社区帖子，包含标题、摘要、正文、作者、发布时间
- **Category（分类）**: 工具的分类信息，用于工具检索时的过滤条件
- **ToolFile（工具文件）**: 工具关联的安装包文件，包含文件名、路径、大小、格式

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: AI IDE 能在 3 秒内成功建立 MCP 连接并获取首个响应
- **SC-002**: 工具检索请求的响应时间不超过 500ms（100 条工具数据规模下）
- **SC-003**: MCP Server 能稳定支持至少 5 个并发连接
- **SC-004**: 帖子检索功能能在 1 秒内返回前 20 条匹配结果
- **SC-005**: 所有 MCP 接口返回的数据格式符合 MCP 协议规范
- **SC-006**: MCP Server 启动失败不影响主应用（8082 端口）的正常服务

## Assumptions

- AI IDE 通过 HTTP + SSE 协议与 MCP Server 通信，支持服务端推送
- MCP Server 运行在独立的端口（8082），与主应用分离避免冲突
- 认证机制：无（内嵌服务，内网部署，无需认证）
- 工具和帖子的数据直接复用现有的数据库表结构（tool, post, category 表）
- MCP 协议版本使用 2024-11.05 版本，这是目前主流的 MCP 实现版本
- 工具 Markdown 文档内容存储在 tool 表的 content 字段中
- 文件下载采用返回下载地址的方式，实际下载通过标准 HTTP 完成
- 数据规模：100-500 个工具，500-2000 个帖子