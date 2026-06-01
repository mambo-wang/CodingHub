---
name: backend-mcp-migration
overview: 使用原生 MCP SDK 0.9.0 重新实现 backend 模块的 MCP Server，替换当前自定义的 JSON-RPC 协议实现
todos:
  - id: create-mcp-config
    content: 创建 McpServerConfig.java，使用 MCP SDK 同步服务器配置
    status: completed
  - id: create-tool-handler
    content: 创建 IaihubToolHandler.java，实现 SDK 工具处理接口
    status: completed
    dependencies:
      - create-mcp-config
  - id: create-mcp-controller
    content: 创建 McpController.java，暴露 SSE 和消息端点
    status: completed
    dependencies:
      - create-mcp-config
  - id: refactor-mcp-connection-manager
    content: 重构 McpConnectionManager.java，适配 SDK 传输层
    status: completed
    dependencies:
      - create-mcp-controller
  - id: delete-custom-protocol
    content: 删除 mcp/protocol/ 自定义协议类（不再需要）
    status: completed
    dependencies:
      - create-mcp-controller
  - id: delete-old-mcp-server
    content: 删除旧的 McpServer.java 和 McpToolHandler.java
    status: completed
    dependencies:
      - delete-custom-protocol
---

## 用户需求

使用 `io.modelcontextprotocol.sdk:mcp-bom` 和 `io.modelcontextprotocol.sdk:mcp` (版本 0.9.0) 重新实现 backend 中原有的 MCP Server 功能。

## 现状分析

1. **backend/build.gradle** 已添加 MCP SDK 依赖管理 BOM 和 mcp 依赖
2. **backend 现有实现** 采用自定义 JSON-RPC 2.0 协议，结构如下：

- `mcp/protocol/` - 自定义协议类（McpMessage, McpResponse, McpError）
- `McpServer.java` - 自定义核心服务器类
- `McpToolHandler.java` - 工具处理器
- `McpConnectionManager.java` - SSE 连接管理
- `McpResourceHandler.java` - 资源处理

3. **McpSearchService.java** - 现有搜索服务（可复用）
4. **mcp-server/** - 独立模块，使用 MCP SDK 的参考实现

## 核心功能

- 工具搜索（searchTools）
- 工具详情获取（getTool）
- 工具文件列表（getToolFiles）
- 帖子搜索（searchPosts）
- 帖子详情获取（getPostGet）

## 实现目标

用原生 MCP SDK 替换自定义实现，遵循 MCP 协议标准，通过 SSE 传输提供 MCP 服务。

## 技术方案

### 技术选型

- **MCP SDK**: io.modelcontextprotocol.sdk:mcp:0.9.0
- **传输层**: HTTP Servlet SSE (HttpServletSseServerTransportProvider)
- **协议模式**: 同步模式 (McpSyncServer)
- **服务端口**: 复用 backend 8080 端口

### 实现策略

1. **删除** 自定义 protocol 包，使用 SDK 内置的 McpSchema 类
2. **重构** McpServer.java，使用 `McpServer.sync()` API 创建服务器
3. **重构** McpToolHandler.java，实现 SDK 的 `ListToolsHandler` 和 `CallToolHandler` 接口
4. **重构** McpConnectionManager.java，使用 SDK 传输层
5. **添加** McpController 暴露 SSE 端点 (`/mcp/sse`) 和消息端点 (`/mcp/message`)
6. **复用** McpSearchService 处理业务逻辑

### 目录结构

```
backend/src/main/java/com/iaihub/toolbox/
├── mcp/
│   ├── McpServerConfig.java      # [NEW] MCP Server 配置与初始化
│   ├── McpController.java        # [NEW] MCP HTTP 端点控制器
│   ├── IaihubToolHandler.java    # [NEW] 工具处理实现（实现 SDK 接口）
│   ├── McpConnectionManager.java # [KEEP] SSE 连接管理（简化版）
│   └── McpResourceHandler.java   # [KEEP] 资源处理（如需保留）
└── service/
    └── McpSearchService.java     # [KEEP] 搜索服务（已存在）
```

### MCP 工具定义

| 工具名 | 描述 | 参数 |
| --- | --- | --- |
| h3_coding_hub_tool_search | 搜索工具列表 | query, category, limit |
| h3_coding_hub_tool_get | 获取工具详情 | toolId |
| h3_coding_hub_tool_files | 获取工具文件 | toolId |
| h3_coding_hub_post_search | 搜索帖子 | query, limit |
| h3_coding_hub_post_get | 获取帖子详情 | postId |