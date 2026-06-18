# MCP Module

## 概述

MCP（Model Context Protocol）模块是 IAIHub Toolbox 平台的核心集成层，基于 **MCP SDK 2.0.0** 实现，为 AI 客户端（如 Claude Desktop、Cursor 等）提供标准化的工具调用接口。通过 MCP 协议，AI 客户端可以搜索工具、获取工具详情、下载文件、创建/修改工具、搜索和创建社区帖子等，实现 AI 与平台数据的深度交互。

MCP Server 在独立端口（默认 `8082`）运行，通过 SSE（Server-Sent Events）传输协议与客户端通信，注册了 **11 个工具**，覆盖工具管理和论坛社区的核心操作。

## 架构总览

```mermaid
graph TB
    subgraph "MCP 客户端"
        Client["AI 客户端<br/>(Claude / Cursor / 其他)"]
    end

    subgraph "MCP Module"
        subgraph "服务器配置与传输"
            SdkConfig["McpSdkServerConfig<br/>SDK 服务器配置"]
            ServerConfig["McpServerConfig<br/>端口/连接配置"]
            Controller["McpController<br/>健康检查端点"]
            ConnMgr["McpConnectionManager<br/>(已弃用)"]
        end

        subgraph "工具处理器"
            ToolHandler["IaihubToolHandler<br/>11个工具调用处理器"]
            ResourceHandler["McpResourceHandler<br/>资源列表与检索"]
        end

        subgraph "搜索服务"
            SearchService["McpSearchService<br/>工具/帖子搜索"]
            SearchReq["McpSearchRequest<br/>搜索请求DTO"]
        end
    end

    subgraph "依赖模块"
        ToolModule["Tool Module<br/>工具管理"]
        ForumModule["Forum Module<br/>论坛社区"]
        AuthModule["Auth & User Module<br/>认证与用户"]
    end

    Client -->|"SSE /mcp/message"| SdkConfig
    Client -->|"GET /mcp/health"| Controller
    SdkConfig -->|"注册工具 & 分发调用"| ToolHandler
    SdkConfig -->|"注册资源"| ResourceHandler
    ToolHandler -->|"搜索/获取"| SearchService
    ToolHandler -->|"创建/修改工具"| ToolModule
    ToolHandler -->|"创建帖子"| ForumModule
    ToolHandler -->|"认证登录"| AuthModule
    ResourceHandler -->|"搜索/获取"| SearchService
    SearchService -->|"查询工具"| ToolModule
    SearchService -->|"查询帖子"| ForumModule
    SearchService -->|"查询用户"| AuthModule
```

## 子模块说明

MCP Module 由三个子模块组成，各子模块职责清晰、层次分明：

| 子模块 | 文档 | 职责 |
|--------|------|------|
| MCP服务器配置子模块 | [MCP服务器配置子模块.md](MCP服务器配置子模块.md) | MCP Server 的初始化、SSE 传输配置、工具注册、健康检查及连接管理 |
| MCP工具处理子模块 | [MCP工具处理子模块.md](MCP工具处理子模块.md) | 11 个 MCP 工具的具体业务逻辑处理，包括搜索、详情、创建、修改、删除等操作 |
| MCP搜索服务子模块 | [MCP搜索服务子模块.md](MCP搜索服务子模块.md) | 封装工具和帖子的数据检索逻辑，作为工具处理器与底层数据层的桥梁 |

## 核心工作流程

### MCP 工具调用流程

```mermaid
sequenceDiagram
    participant Client as MCP 客户端
    participant SSE as SSE Transport Provider
    participant SdkServer as McpSyncServer
    participant Handler as IaihubToolHandler
    participant Search as McpSearchService
    participant Service as 业务 Service

    Client->>SSE: 建立 SSE 连接 (/sse)
    SSE-->>Client: 连接建立成功
    Client->>SSE: 发送工具调用请求 (/mcp/message)
    SSE->>SdkServer: 路由到对应工具处理器
    SdkServer->>Handler: 调用对应 handle 方法
    Handler->>Search: 搜索/查询数据
    Search->>Service: 调用底层 Repository
    Service-->>Search: 返回数据
    Search-->>Handler: 返回结果
    Handler-->>SdkServer: 返回 CallToolResult (JSON)
    SdkServer-->>SSE: 封装响应
    SSE-->>Client: 返回工具调用结果
```

### 认证工具调用流程

对于需要认证的工具（如创建工具、创建帖子、修改工具、删除文件），MCP 客户端需在调用参数中传入用户名和密码：

```mermaid
sequenceDiagram
    participant Client as MCP 客户端
    participant Handler as IaihubToolHandler
    participant UserService as UserService
    participant BizService as 业务 Service

    Client->>Handler: 调用认证工具 (含 username/password)
    Handler->>UserService: login(username, password)
    UserService-->>Handler: 返回 LoginResponse (含 userId)
    Handler->>BizService: 使用 userId 执行业务操作
    BizService-->>Handler: 返回操作结果
    Handler-->>Client: 返回 JSON 结果
```

## 注册的 MCP 工具一览

| 工具名称 | 描述 | 是否需要认证 |
|----------|------|:------------:|
| `h3_coding_hub_tool_search` | 搜索工具列表，支持关键词和分类筛选 | 否 |
| `h3_coding_hub_tool_get` | 获取工具详情，包括完整 markdown 文档 | 否 |
| `h3_coding_hub_tool_files` | 获取工具文件下载信息 | 否 |
| `h3_coding_hub_post_search` | 搜索社区帖子 | 否 |
| `h3_coding_hub_post_get` | 获取帖子内容，包括完整 markdown | 否 |
| `h3_coding_hub_tool_download` | 获取工具文件的下载链接 | 否 |
| `h3_coding_hub_tool_create` | 创建新工具 | 是 |
| `h3_coding_hub_post_create` | 创建新帖子 | 是 |
| `h3_coding_hub_tool_file_upload` | 获取文件上传接口信息 | 否 |
| `h3_coding_hub_tool_modify` | 修改已创建的工具 | 是 |
| `h3_coding_hub_tool_file_delete` | 删除工具文件 | 是 |

## 跨模块依赖关系

MCP Module 作为集成层，依赖以下模块的核心服务：

- **[Tool Module](Tool Module.md)**：通过 `ToolService`、`ToolFileService` 实现工具的创建、修改和文件管理；通过 `ToolRepository`、`ToolFileRepository` 实现工具数据检索
- **[Forum Module](Forum Module.md)**：通过 `ForumPostService` 实现帖子创建；通过 `ForumPostRepository` 实现帖子数据检索
- **[Auth & User Module](Auth & User Module.md)**：通过 `UserService` 实现认证登录；通过 `UserRepository` 查询用户信息

```mermaid
graph LR
    MCP["MCP Module"]
    MCP -->|"ToolService / ToolFileService"| ToolMod["Tool Module"]
    MCP -->|"ForumPostService"| ForumMod["Forum Module"]
    MCP -->|"UserService / UserRepository"| AuthMod["Auth & User Module"]
    MCP -->|"ToolSearchResult / PostSearchResult"| CommonMod["Overview & Common Module"]
```

## 技术要点

- **MCP SDK 2.0.0**：使用原生 Java MCP SDK，通过 `HttpServletSseServerTransportProvider` 处理 SSE 传输
- **独立端口部署**：MCP Server 运行在独立端口（默认 8082），与主应用隔离
- **SSE 传输协议**：通过 `/sse` 端点建立连接，`/mcp/message` 端点处理消息
- **认证机制**：需要认证的工具通过 MCP 客户端传入用户名/密码，由 `UserService.login()` 完成认证
- **版本自动递增**：修改工具时若未指定版本号，系统自动将最后一位数字 +1（如 `1.0.0` → `1.0.1`）
- **JSON 序列化**：使用 Jackson `ObjectMapper` 统一序列化工具调用结果
