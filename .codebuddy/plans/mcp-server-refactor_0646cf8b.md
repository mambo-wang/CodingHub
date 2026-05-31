---
name: mcp-server-refactor
overview: 使用 Spring AI 官方 MCP Server Starter 重构内嵌 MCP Server，支持 HTTP/SSE 和 STDIO 两种通信方式，让 CodeBuddy IDE 可以发现并连接
todos:
  - id: upgrade-spring-boot
    content: 升级 Spring Boot 版本到 3.4.4
    status: completed
  - id: add-spring-ai-deps
    content: 添加 Spring AI MCP Server 依赖
    status: completed
    dependencies:
      - upgrade-spring-boot
  - id: create-mcp-tools
    content: 创建 McpTools.java 工具定义类
    status: completed
    dependencies:
      - add-spring-ai-deps
  - id: create-mcp-config
    content: 创建 McpToolConfig.java 工具配置类
    status: completed
    dependencies:
      - add-spring-ai-deps
  - id: configure-mcp-server
    content: 配置 application.yml MCP 参数
    status: completed
    dependencies:
      - create-mcp-config
  - id: test-mcp-server
    content: 测试 MCP Server HTTP/SSE 模式
    status: completed
    dependencies:
      - configure-mcp-server
  - id: configure-stdio
    content: 配置 STDIO 模式支持
    status: completed
    dependencies:
      - test-mcp-server
  - id: cleanup-old-mcp
    content: 清理旧的手写 MCP 实现文件
    status: completed
    dependencies:
      - configure-stdio
---

## 用户需求

使用 Spring AI 官方方式重构 MCP Server，支持 HTTP/SSE 和 STDIO 两种通信方式，供 CodeBuddy IDE 使用。

## 核心功能

1. **MCP 工具检索**：`h3_coding_hub_tool_search`、`h3_coding_hub_tool_get`、`h3_coding_hub_tool_files`
2. **MCP 帖子检索**：`h3_coding_hub_post_search`、`h3_coding_hub_post_get`
3. **双通信模式**：HTTP/SSE（远程 IDE）+ STDIO（本地 Desktop）
4. **自动启动**：随 Spring Boot 服务启动

## 技术约束

- 升级 Spring Boot 3.2.5 → 3.4.x（Spring AI MCP Server 要求）
- 使用 Spring AI 1.0.0-M7
- 复用现有 McpSearchService、Repository 层
- 兼容现有数据库表结构（tool、forum_post、tool_file）

## 技术方案

### 依赖升级

- Spring Boot: 3.2.5 → 3.4.4
- Spring AI: 1.0.0-M7
- 添加 `spring-ai-starter-mcp-server-webflux` (SSE)
- 添加 `spring-ai-starter-mcp-server-webmvc` (STDIO)

### MCP 工具定义

使用 `@Tool` + `@ToolParam` 注解定义 5 个工具，封装现有 McpSearchService：

```java
public class McpTools {
    private final McpSearchService searchService;
    
    @Tool(description = "搜索 AI 工具广场的工具，支持按名称、分类、关键词检索")
    public List<ToolSearchResult> toolSearch(
        @ToolParam(description = "搜索关键词") String query,
        @ToolParam(description = "分类名称") String category,
        @ToolParam(description = "返回结果数量，默认 20") Integer limit
    ) { ... }
    
    // ... 其他 4 个工具
}
```

### 工具配置类

通过 `MethodToolCallbackProvider` 注册所有工具到 MCP Server

### 配置参数

```
spring:
  ai:
    mcp:
      server:
        name: H3CodingHub
        version: 1.0.0
        type: ASYNC  # SSE 模式
```

### 目录结构

```
backend/src/main/java/com/iaihub/toolbox/
├── mcp/
│   ├── McpTools.java          # [NEW] MCP 工具定义
│   └── McpToolConfig.java      # [NEW] 工具配置
├── dto/
│   ├── McpSearchRequest.java   # [KEEP]
│   ├── ToolSearchResult.java    # [KEEP]
│   └── PostSearchResult.java    # [KEEP]
└── service/
    └── McpSearchService.java   # [KEEP]
```