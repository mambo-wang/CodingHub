# MCP Server 模块文档

## 简介

MCP（Model Context Protocol）Server 模块是 H3CodingHub 平台的核心集成层，基于 **MCP SDK 2.0.0** 实现，通过 **SSE（Server-Sent Events）** 传输协议为 AI 客户端（如 Claude、Cursor 等）提供标准化的工具调用接口。该模块将平台的工具管理、社区论坛和用户认证能力封装为 11 个 MCP 工具，使 AI 客户端能够搜索工具、获取详情、创建内容、管理文件等，实现 AI 与平台的双向交互。

> **架构定位**：MCP Server 作为 AI 客户端与 H3CodingHub 后端服务之间的桥梁，所有 AI 驱动的操作均通过此模块路由到对应的业务服务。

---

## 架构概览

```mermaid
graph TB
    subgraph AI客户端["AI 客户端"]
        Claude["Claude / Cursor / 其他 MCP 客户端"]
    end

    subgraph MCP模块["MCP Server 模块"]
        SSE["SSE 传输层<br/>/sse, /mcp/message"]
        SDK["McpSyncServer<br/>MCP SDK 2.0.0"]
        Handler["IaihubToolHandler<br/>工具处理器"]
        Resource["McpResourceHandler<br/>资源处理器"]
        Search["McpSearchService<br/>搜索服务"]
        Ctrl["McpController<br/>健康检查"]
        Config["McpServerConfig<br/>配置属性"]
    end

    subgraph 业务服务["业务服务层"]
        ToolSvc["ToolService"]
        FileSvc["ToolFileService"]
        PostSvc["ForumPostService"]
        UserSvc["UserService"]
    end

    subgraph 数据层["数据访问层"]
        ToolRepo["ToolRepository"]
        FileRepo["ToolFileRepository"]
        PostRepo["ForumPostRepository"]
        UserRepo["UserRepository"]
    end

    Claude -->|SSE 连接| SSE
    SSE --> SDK
    SDK -->|工具调用| Handler
    SDK -->|资源请求| Resource
    Handler --> Search
    Handler --> ToolSvc
    Handler --> FileSvc
    Handler --> PostSvc
    Handler --> UserSvc
    Resource --> Search
    Search --> ToolRepo
    Search --> FileRepo
    Search --> PostRepo
    Search --> UserRepo
    Ctrl -.->|健康检查| SDK

    style MCP模块 fill:#e1f5fe,stroke:#0288d1,stroke-width:2px
    style AI客户端 fill:#fff3e0,stroke:#f57c00
    style 业务服务 fill:#e8f5e9,stroke:#388e3c
    style 数据层 fill:#fce4ec,stroke:#c62828
```

## 模块依赖关系

```mermaid
graph LR
    subgraph mcp["MCP Server 模块"]
        McpSdkServerConfig
        IaihubToolHandler
        McpResourceHandler
        McpConnectionManager
        McpSearchService
        McpController
        McpServerConfig
        McpSearchRequest
        ToolSearchResult
        PostSearchResult
    end

    subgraph auth["authentication 模块"]
        UserService
        LoginRequest
        LoginResponse
    end

    subgraph tool["tool-management 模块"]
        ToolService
        ToolFileService
        Tool
        ToolFile
        CreateToolRequest
        UpdateToolRequest
        ToolDetailDTO
        ToolSummaryDTO
        ToolRepository
        ToolFileRepository
    end

    subgraph forum["forum 模块"]
        ForumPostService
        ForumPost
        ForumPostCreateRequest
        ForumPostDTO
        ForumPostRepository
    end

    IaihubToolHandler --> UserService
    IaihubToolHandler --> ToolService
    IaihubToolHandler --> ToolFileService
    IaihubToolHandler --> ForumPostService
    IaihubToolHandler --> McpSearchService
    McpSearchService --> ToolRepository
    McpSearchService --> ToolFileRepository
    McpSearchService --> ForumPostRepository
    McpResourceHandler --> McpSearchService
    McpSdkServerConfig --> IaihubToolHandler

    style mcp fill:#e1f5fe,stroke:#0288d1,stroke-width:2px
    style auth fill:#fff3e0,stroke:#f57c00
    style tool fill:#e8f5e9,stroke:#388e3c
    style forum fill:#f3e5f5,stroke:#7b1fa2
```

> 模块依赖说明：MCP Server 模块依赖 [authentication](authentication.md) 模块进行用户认证，依赖 [tool-management](tool-management.md) 模块进行工具 CRUD 操作，依赖 [forum](forum.md) 模块进行帖子管理。

---

## 核心组件详解

### 1. McpSdkServerConfig — MCP SDK 服务器配置

**文件**：`backend/src/main/java/com/iaihub/toolbox/mcp/McpSdkServerConfig.java`

这是 MCP Server 的核心配置类，负责：

- **初始化 MCP SDK 同步服务器**：使用 `McpServer.sync()` 创建 `McpSyncServer` 实例，服务器名称为 `H3CodingHub-MCP-Server`，版本 `2.0.0`
- **配置 SSE 传输层**：通过 `HttpServletSseServerTransportProvider` 提供 SSE 传输，注册到 `/sse` 和 `/mcp/message` 端点
- **注册 11 个 MCP 工具**：每个工具包含名称、描述、JSON Schema 输入定义和处理函数
- **配置 JSON 映射器**：使用 Jackson 作为 JSON 序列化/反序列化引擎

#### 注册的 MCP 工具列表

| 工具名称 | 功能描述 | 是否需要认证 |
|---------|---------|:----------:|
| `h3_coding_hub_tool_search` | 按关键词和分类搜索工具列表 | ❌ |
| `h3_coding_hub_tool_get` | 获取工具详情（含完整 Markdown 文档） | ❌ |
| `h3_coding_hub_tool_files` | 获取工具文件下载信息 | ❌ |
| `h3_coding_hub_post_search` | 搜索社区帖子 | ❌ |
| `h3_coding_hub_post_get` | 获取帖子内容（含完整 Markdown） | ❌ |
| `h3_coding_hub_tool_download` | 获取工具文件下载链接 | ❌ |
| `h3_coding_hub_tool_create` | 创建新工具 | ✅ |
| `h3_coding_hub_post_create` | 创建新帖子 | ✅ |
| `h3_coding_hub_tool_file_upload` | 获取文件上传接口信息 | ❌ |
| `h3_coding_hub_tool_modify` | 修改已创建的工具 | ✅ |
| `h3_coding_hub_tool_file_delete` | 删除工具文件 | ✅ |

#### 工具注册流程

```mermaid
flowchart TD
    A["McpSdkServerConfig 启动"] --> B["创建 McpJsonMapper<br/>(Jackson)"]
    B --> C["创建 HttpServletSseServerTransportProvider<br/>端点: /sse, /mcp/message"]
    C --> D["注册 ServletRegistrationBean"]
    D --> E["创建 McpSyncServer<br/>serverInfo: H3CodingHub-MCP-Server v2.0.0"]
    E --> F["配置 ServerCapabilities<br/>tools: true, logging: true"]
    F --> G["循环注册 11 个工具"]
    G --> H["每个工具: 解析 JSON Schema → 构建 Tool → 注册 callHandler"]
    H --> I["MCP Server 就绪"]

    style A fill:#e1f5fe,stroke:#0288d1
    style I fill:#e8f5e9,stroke:#388e3c
```

#### 关键 Bean 定义

| Bean 名称 | 类型 | 作用 |
|-----------|------|------|
| `mcpJsonMapper` | `JacksonMcpJsonMapper` | MCP 协议 JSON 序列化 |
| `servletSseServerTransportProvider` | `HttpServletSseServerTransportProvider` | SSE 传输提供者 |
| `customServletBean` | `ServletRegistrationBean` | Servlet 注册（`/sse`, `/mcp/message`） |
| `mcpSyncServer` | `McpSyncServer` | MCP 同步服务器（destroyMethod = "close"） |

---

### 2. IaihubToolHandler — MCP 工具处理器

**文件**：`backend/src/main/java/com/iaihub/toolbox/mcp/IaihubToolHandler.java`

这是 MCP 工具调用的核心处理器，所有 11 个工具的实际业务逻辑都在此实现。它作为 MCP SDK 与业务服务层之间的适配器，负责：

- **接收 MCP 工具调用请求**：从 `McpSchema.CallToolRequest` 中提取参数
- **路由到对应业务服务**：调用 `ToolService`、`ToolFileService`、`ForumPostService`、`UserService` 等
- **认证处理**：对于需要认证的操作，使用 MCP 客户端传入的账号密码进行登录验证
- **结果封装**：将业务结果转换为 `McpSchema.CallToolResult`（成功/错误）

#### 依赖注入的服务

```mermaid
graph LR
    Handler["IaihubToolHandler"]
    Handler -->|搜索| SearchSvc["McpSearchService"]
    Handler -->|工具 CRUD| ToolSvc["ToolService"]
    Handler -->|文件管理| FileSvc["ToolFileService"]
    Handler -->|帖子操作| PostSvc["ForumPostService"]
    Handler -->|用户认证| UserSvc["UserService"]
    Handler -->|JSON 序列化| Mapper["ObjectMapper"]

    style Handler fill:#e1f5fe,stroke:#0288d1,stroke-width:2px
```

#### 工具处理方法详解

##### 查询类工具（无需认证）

| 方法 | 对应工具 | 核心逻辑 |
|------|---------|---------|
| `handleToolSearch` | `h3_coding_hub_tool_search` | 调用 `McpSearchService.searchTools()` 按关键词/分类搜索 |
| `handleToolGet` | `h3_coding_hub_tool_get` | 调用 `McpSearchService.getToolById()` 获取完整工具详情 |
| `handleToolFiles` | `h3_coding_hub_tool_files` | 调用 `McpSearchService.getToolFiles()` 获取文件列表 |
| `handlePostSearch` | `h3_coding_hub_post_search` | 调用 `McpSearchService.searchPosts()` 搜索帖子 |
| `handlePostGet` | `h3_coding_hub_post_get` | 调用 `McpSearchService.getPostById()` 获取帖子详情 |
| `handleToolDownload` | `h3_coding_hub_tool_download` | 调用 `McpSearchService.getToolFile()` 获取下载链接 |
| `handleToolFileUploadInfo` | `h3_coding_hub_tool_file_upload` | 返回 REST API 上传接口信息（告知客户端如何上传） |

##### 写操作类工具（需要认证）

| 方法 | 对应工具 | 认证方式 | 核心逻辑 |
|------|---------|---------|---------|
| `handleToolCreate` | `h3_coding_hub_tool_create` | 账号密码登录 | 登录后调用 `ToolService.createTool()` |
| `handlePostCreate` | `h3_coding_hub_post_create` | 账号密码登录 | 登录后调用 `ForumPostService.createPost()` |
| `handleToolModify` | `h3_coding_hub_tool_modify` | 账号密码登录 | 登录后调用 `ToolService.updateTool()`，支持版本自动递增 |
| `handleToolFileDelete` | `h3_coding_hub_tool_file_delete` | 账号密码登录 | 登录后调用 `ToolFileService.deleteToolFile()` |

#### 认证流程

```mermaid
sequenceDiagram
    participant Client as MCP 客户端
    participant SDK as McpSyncServer
    participant Handler as IaihubToolHandler
    participant UserSvc as UserService
    participant BizSvc as 业务服务

    Client->>SDK: 调用工具（含 username/password）
    SDK->>Handler: handleToolCreate(name, ..., username, password)
    Handler->>UserSvc: login(LoginRequest)
    UserSvc-->>Handler: LoginResponse(userId, token)
    Handler->>BizSvc: createTool(request, userId)
    BizSvc-->>Handler: ToolSummaryDTO
    Handler-->>SDK: CallToolResult(success, JSON)
    SDK-->>Client: 工具调用结果
```

#### 版本号自动递增逻辑

`handleToolModify` 方法中实现了版本号自动递增功能（`incrementVersion` 方法）：

| 当前版本 | 递增后 | 说明 |
|---------|--------|------|
| `1.0.0` | `1.0.1` | 最后一位数字 +1 |
| `1.0.0-beta` | `1.0.1-beta` | 保留后缀，数字部分 +1 |
| `1.0.alpha` | `1.0.alpha.1` | 最后一段非数字，追加 `.1` |
| `null` / 空 | `1.0.1` | 默认值 |

#### 内部响应 DTO

`IaihubToolHandler` 定义了多个内部静态 DTO 类用于封装 MCP 工具响应：

| DTO 类 | 用途 |
|--------|------|
| `ToolSearchResponse` | 工具搜索结果（含 tools 列表和 count） |
| `ToolDetailResponse` | 工具详情（id, name, version, content, category） |
| `ToolFilesResponse` | 工具文件列表（含 files 和 toolId） |
| `PostSearchResponse` | 帖子搜索结果（含 posts 列表和 count） |
| `PostDetailResponse` | 帖子详情（id, title, content, authorId, createdAt） |
| `FileDownloadResponse` | 文件下载信息（fileId, fileName, downloadUrl 等） |
| `FileUploadInfoResponse` | 文件上传接口信息（uploadUrl, httpMethod, contentType 等） |
| `FileDeleteResponse` | 文件删除结果（toolId, fileId, deleted） |
| `ErrorResponse` | 错误响应（error message） |

---

### 3. McpResourceHandler — MCP 资源处理器

**文件**：`backend/src/main/java/com/iaihub/toolbox/mcp/McpResourceHandler.java`

负责 MCP 协议中的资源（Resource）管理，提供以下能力：

| 方法 | 功能 |
|------|------|
| `listTools()` | 列出所有可用工具作为 MCP 资源（最多 50 个），每个资源包含 name、description 和 inputSchema |
| `searchTools(query, category, limit)` | 代理调用 `McpSearchService.searchTools()` 进行工具搜索 |
| `getToolContent(toolId)` | 获取指定工具的 Markdown 内容 |

> **注意**：当前 MCP SDK 配置中未显式注册资源处理器到 `McpSyncServer`，资源功能作为辅助能力存在。

---

### 4. McpSearchService — MCP 搜索服务

**文件**：`backend/src/main/java/com/iaihub/toolbox/service/McpSearchService.java`

这是 MCP 模块的搜索服务层，封装了对工具和帖子的检索逻辑，直接操作 Repository 层。

#### 核心方法

| 方法 | 数据源 | 说明 |
|------|--------|------|
| `searchTools(query, category, limit)` | `ToolRepository` | 搜索已审核工具，返回 `ToolSearchResult` 列表（内容截取前 100 字符作为摘要） |
| `getToolById(toolId)` | `ToolRepository` | 获取工具详情（含分类和上传者关联） |
| `getToolFiles(toolId)` | `ToolFileRepository` | 获取工具的正常状态文件列表 |
| `searchPosts(query, limit)` | `ForumPostRepository` + `UserRepository` | 搜索帖子，关联查询作者名称 |
| `getPostById(postId)` | `ForumPostRepository` | 获取帖子详情 |
| `getToolFile(toolId, fileId)` | `ToolFileRepository` | 获取指定工具下的指定文件 |

#### 搜索数据流

```mermaid
flowchart LR
    subgraph 输入
        Q["query 关键词"]
        C["category 分类"]
        L["limit 数量"]
    end

    subgraph 搜索服务
        Search["McpSearchService"]
    end

    subgraph 数据库查询
        ToolQuery["ToolRepository<br/>.findApprovedToolsWithCategory()"]
        PostQuery["ForumPostRepository<br/>.searchByTitle() / .findByStatusOrderByCreatedAtDesc()"]
        UserQuery["UserRepository<br/>.findById()"]
    end

    subgraph 输出
        ToolResult["List<ToolSearchResult>"]
        PostResult["List<PostSearchResult>"]
    end

    Q --> Search
    C --> Search
    L --> Search
    Search -->|工具搜索| ToolQuery
    Search -->|帖子搜索| PostQuery
    PostQuery -->|关联作者| UserQuery
    ToolQuery --> ToolResult
    PostQuery --> PostResult

    style Search fill:#e1f5fe,stroke:#0288d1,stroke-width:2px
```

---

### 5. McpController — MCP HTTP 端点

**文件**：`backend/src/main/java/com/iaihub/toolbox/controller/McpController.java`

REST 控制器，映射到 `/mcp` 路径，提供健康检查端点。

> **重要说明**：实际的 SSE 连接和 MCP 协议交互由 `McpSdkServerConfig` 中注册的 `HttpServletSseServerTransportProvider` Servlet 处理（映射到 `/sse` 和 `/mcp/message`），此控制器仅提供辅助端点。

| 端点 | 方法 | 功能 |
|------|------|------|
| `/mcp/health` | GET | 健康检查，返回服务器状态、版本和时间戳 |

---

### 6. McpServerConfig — 服务器配置属性

**文件**：`backend/src/main/java/com/iaihub/toolbox/config/McpServerConfig.java`

使用 `@ConfigurationProperties(prefix = "mcp.server")` 绑定配置属性：

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `port` | `8082` | MCP Server 监听端口 |
| `host` | `0.0.0.0` | 监听地址 |
| `enabled` | `true` | 是否启用 MCP Server |
| `maxConnections` | `10` | 最大连接数 |
| `connectionTimeoutMs` | `30000` | 连接超时时间（毫秒） |

---

### 7. McpConnectionManager — SSE 连接管理器（已弃用）

**文件**：`backend/src/main/java/com/iaihub/toolbox/mcp/McpConnectionManager.java`

> ⚠️ **已弃用**：此类标记为 `@Deprecated`，连接管理已由 MCP SDK 的 `HttpServletSseServerTransportProvider` 内部处理。保留此代码仅为向后兼容参考。

原功能包括：
- SSE 连接注册与生命周期管理（超时 30 分钟）
- 事件广播（`broadcastEvent`）和定向发送（`sendToEmitter`）
- 心跳检测和连接清理
- 自定义 `SseEmitter` 包装类（避免命名冲突）

---

### 8. DTO 组件

#### McpSearchRequest

**文件**：`backend/src/main/java/com/iaihub/toolbox/dto/McpSearchRequest.java`

MCP 搜索请求 DTO，包含输入验证：

| 字段 | 类型 | 验证规则 | 说明 |
|------|------|---------|------|
| `query` | `String` | `@Size(max = 200)` | 搜索关键词 |
| `category` | `String` | — | 分类名称 |
| `limit` | `Integer` | `@Min(1)`, `@Max(100)` | 返回数量限制，默认 20 |

#### ToolSearchResult

**文件**：`backend/src/main/java/com/iaihub/toolbox/dto/ToolSearchResult.java`

工具搜索结果 DTO，使用 `@JsonProperty` 注解确保 JSON 字段命名：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `Long` | 工具 ID |
| `name` | `String` | 工具名称 |
| `description` | `String` | 工具描述（内容前 100 字符） |
| `category` | `String` | 分类名称 |
| `version` | `String` | 版本号 |
| `createdAt` | `String` | 创建时间 |

#### PostSearchResult

**文件**：`backend/src/main/java/com/iaihub/toolbox/dto/PostSearchResult.java`

帖子搜索结果 DTO：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `Long` | 帖子 ID |
| `title` | `String` | 帖子标题 |
| `summary` | `String` | 帖子摘要（内容前 100 字符） |
| `authorName` | `String` | 作者用户名 |
| `createdAt` | `String` | 创建时间 |

---

## 端到端数据流

### 工具搜索流程

```mermaid
sequenceDiagram
    participant AI as AI 客户端
    participant SSE as SSE Transport
    participant SDK as McpSyncServer
    participant Handler as IaihubToolHandler
    participant Search as McpSearchService
    participant Repo as ToolRepository
    participant DB as 数据库

    AI->>SSE: SSE 连接 /sse
    SSE-->>AI: 连接建立
    AI->>SSE: 工具调用 h3_coding_hub_tool_search
    SSE->>SDK: CallToolRequest
    SDK->>Handler: handleToolSearch(query, category, limit)
    Handler->>Search: searchTools(query, category, limit)
    Search->>Repo: findApprovedToolsWithCategory(query, pageable)
    Repo->>DB: SELECT 查询
    DB-->>Repo: List<Tool>
    Repo-->>Search: 工具实体列表
    Search-->>Handler: List<ToolSearchResult>
    Handler-->>SDK: CallToolResult(JSON)
    SDK-->>SSE: 响应
    SSE-->>AI: 搜索结果 JSON
```

### 创建工具流程（含认证）

```mermaid
sequenceDiagram
    participant AI as AI 客户端
    participant SDK as McpSyncServer
    participant Handler as IaihubToolHandler
    participant UserSvc as UserService
    participant ToolSvc as ToolService
    participant DB as 数据库

    AI->>SDK: h3_coding_hub_tool_create(name, categoryId, content, version, username, password)
    SDK->>Handler: handleToolCreate(...)
    
    Handler->>UserSvc: login(username, password)
    UserSvc->>DB: 验证用户凭据
    DB-->>UserSvc: User 实体
    UserSvc-->>Handler: LoginResponse(userId, token)
    
    Handler->>ToolSvc: createTool(CreateToolRequest, userId)
    ToolSvc->>DB: INSERT 工具记录
    DB-->>ToolSvc: Tool 实体
    ToolSvc-->>Handler: ToolSummaryDTO
    
    Handler-->>SDK: CallToolResult(success, JSON)
    SDK-->>AI: {id, name, version, ...}
```

### 文件上传信息获取流程

```mermaid
sequenceDiagram
    participant AI as AI 客户端
    participant SDK as McpSyncServer
    participant Handler as IaihubToolHandler
    participant Search as McpSearchService

    AI->>SDK: h3_coding_hub_tool_file_upload(toolId)
    SDK->>Handler: handleToolFileUploadInfo(toolId)
    Handler->>Search: getToolById(toolId)
    Search-->>Handler: Tool 实体
    
    Handler-->>SDK: FileUploadInfoResponse
    Note over Handler: 返回 REST API 信息:<br/>URL: /api/v1/tools/{toolId}/files<br/>Method: POST<br/>Content-Type: multipart/form-data<br/>限制: 50MB/文件, 200MB/总计
    
    SDK-->>AI: 上传接口信息 JSON
    
    Note over AI: 客户端根据返回信息<br/>直接通过 HTTP Multipart POST 上传文件
```

---

## MCP 工具调用分类

```mermaid
graph TB
    subgraph 查询类["查询类工具（无需认证）"]
        TS["h3_coding_hub_tool_search<br/>搜索工具"]
        TG["h3_coding_hub_tool_get<br/>工具详情"]
        TF["h3_coding_hub_tool_files<br/>工具文件"]
        PS["h3_coding_hub_post_search<br/>搜索帖子"]
        PG["h3_coding_hub_post_get<br/>帖子详情"]
        TD["h3_coding_hub_tool_download<br/>下载链接"]
        FU["h3_coding_hub_tool_file_upload<br/>上传信息"]
    end

    subgraph 写操作类["写操作类工具（需要认证）"]
        TC["h3_coding_hub_tool_create<br/>创建工具"]
        PC["h3_coding_hub_post_create<br/>创建帖子"]
        TM["h3_coding_hub_tool_modify<br/>修改工具"]
        FD["h3_coding_hub_tool_file_delete<br/>删除文件"]
    end

    subgraph 认证流程["认证流程"]
        Login["账号密码登录<br/>username + password<br/>默认密码: 123456"]
    end

    TC --> Login
    PC --> Login
    TM --> Login
    FD --> Login

    style 查询类 fill:#e8f5e9,stroke:#388e3c
    style 写操作类 fill:#fff3e0,stroke:#f57c00
    style 认证流程 fill:#fce4ec,stroke:#c62828
```

---

## 与其他模块的交互

### 与 authentication 模块的交互

MCP Server 通过 `UserService.login()` 方法进行用户认证。对于需要认证的写操作工具，MCP 客户端需传入 `username` 和 `password` 参数，处理器内部调用 `UserService.login()` 获取 `userId`，再将 `userId` 传递给后续业务服务。

> 详细认证机制请参考 [authentication 模块文档](authentication.md)。

### 与 tool-management 模块的交互

| MCP 工具 | 调用的服务方法 | 说明 |
|---------|--------------|------|
| `h3_coding_hub_tool_create` | `ToolService.createTool()` | 创建工具，含 XSS 过滤和重名校验 |
| `h3_coding_hub_tool_modify` | `ToolService.updateTool()` | 修改工具，仅更新传入字段 |
| `h3_coding_hub_tool_file_delete` | `ToolFileService.deleteToolFile()` | 删除文件（物理文件 + 数据库记录） |
| `h3_coding_hub_tool_file_upload` | 返回 REST API 信息 | 客户端直接调用 `/api/v1/tools/{toolId}/files` |

> 详细工具管理逻辑请参考 [tool-management 模块文档](tool-management.md)。

### 与 forum 模块的交互

| MCP 工具 | 调用的服务方法 | 说明 |
|---------|--------------|------|
| `h3_coding_hub_post_create` | `ForumPostService.createPost()` | 创建帖子，支持标签关联 |

> 详细论坛逻辑请参考 [forum 模块文档](forum.md)。

---

## 配置与部署

### 配置项

MCP Server 通过 `application.yml` 或环境变量进行配置：

```yaml
mcp:
  server:
    port: 8082           # MCP Server 端口
    host: 0.0.0.0        # 监听地址
    enabled: true         # 是否启用
    max-connections: 10   # 最大连接数
    connection-timeout-ms: 30000  # 连接超时
```

### 端点映射

| 端点 | 协议 | 处理者 | 说明 |
|------|------|--------|------|
| `/sse` | SSE | `HttpServletSseServerTransportProvider` | SSE 连接建立 |
| `/mcp/message` | HTTP POST | `HttpServletSseServerTransportProvider` | MCP 消息交互 |
| `/mcp/health` | HTTP GET | `McpController` | 健康检查 |
| `/api/v1/tools/{toolId}/files` | HTTP POST (multipart) | `ToolFileController` | 文件上传（MCP 客户端直接调用） |
| `/api/v1/tools/{toolId}/files/{fileId}/download` | HTTP GET | `ToolFileController` | 文件下载 |

### 文件上传限制

| 限制项 | 值 |
|--------|-----|
| 单文件最大 | 50 MB |
| 总上传最大 | 200 MB |
| 认证要求 | 无（已放通权限） |

---

## 安全注意事项

1. **认证密码默认值**：MCP 工具的认证密码默认为 `123456`，生产环境应修改默认密码
2. **文件上传放通**：`/api/v1/tools/{toolId}/files` 端点已放通认证，任何 MCP 客户端均可上传文件
3. **XSS 防护**：工具内容通过 `XssSanitizer.sanitize()` 进行 XSS 过滤（由 `ToolService` 处理）
4. **权限校验**：修改和删除操作通过 `userId` 校验资源所有权（由业务服务层处理）
5. **搜索限制**：`McpSearchRequest` 限制 query 最大 200 字符，limit 范围 1-100

---

## 组件关系总览

```mermaid
graph TB
    subgraph 配置层
        McpServerConfig["McpServerConfig<br/>配置属性"]
        McpSdkServerConfig["McpSdkServerConfig<br/>SDK 服务器配置"]
    end

    subgraph 传输层
        Transport["HttpServletSseServerTransportProvider<br/>SSE 传输"]
        Servlet["ServletRegistrationBean<br/>Servlet 注册"]
    end

    subgraph 协议层
        SyncServer["McpSyncServer<br/>MCP 同步服务器"]
    end

    subgraph 处理层
        ToolHandler["IaihubToolHandler<br/>工具处理器（11个工具）"]
        ResourceHandler["McpResourceHandler<br/>资源处理器"]
    end

    subgraph 服务层
        SearchService["McpSearchService<br/>搜索服务"]
    end

    subgraph 控制器层
        McpCtrl["McpController<br/>健康检查"]
    end

    subgraph DTO层
        SearchReq["McpSearchRequest"]
        ToolResult["ToolSearchResult"]
        PostResult["PostSearchResult"]
    end

    subgraph 已弃用
        ConnMgr["McpConnectionManager<br/>⚠️ 已弃用"]
    end

    McpServerConfig --> McpSdkServerConfig
    McpSdkServerConfig --> Transport
    McpSdkServerConfig --> Servlet
    McpSdkServerConfig --> SyncServer
    SyncServer --> ToolHandler
    ResourceHandler --> SearchService
    ToolHandler --> SearchService
    McpCtrl --> SyncServer
    SearchService --> ToolResult
    SearchService --> PostResult
    SearchReq --> SearchService

    style 配置层 fill:#e1f5fe,stroke:#0288d1
    style 传输层 fill:#e8f5e9,stroke:#388e3c
    style 协议层 fill:#fff3e0,stroke:#f57c00
    style 处理层 fill:#f3e5f5,stroke:#7b1fa2
    style 服务层 fill:#fce4ec,stroke:#c62828
    style 已弃用 fill:#ffebee,stroke:#b71c1c,stroke-dasharray: 5 5
```
