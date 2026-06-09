# Research: 嵌入式 MCP Server

**Feature**: 003-embedded-mcp-server
**Date**: 2026-05-31

## 1. MCP 协议研究

### 1.1 MCP 协议概述

Model Context Protocol (MCP) 是一种用于 AI 助手与外部工具/资源交互的标准化协议。2024-11.05 版本是当前主流实现。

### 1.2 通信方式决策

**决策**: HTTP + SSE (Server-Sent Events)

**理由**:
- 便于调试和监控
- 支持服务端推送（AI 可以实时获取更新）
- 与现有 Spring Boot 技术栈兼容
- 不同于 stdio 方式需要单独进程通信

**替代方案评估**:
| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| stdio | 适合本地进程 | 需要独立进程管理 | 放弃 |
| HTTP + SSE | 便于调试，支持推送 | 复杂度略高 | 采用 |
| WebSocket | 双向通信 | 过度设计 | 放弃 |

## 2. 技术实现研究

### 2.1 Spring Boot 内嵌 Server 实现

使用 `@PostConstruct` 或 `ApplicationRunner` 在 Spring Boot 启动时启动 MCP Server。

```java
@Component
public class McpServerInitializer {
    @Autowired
    private McpServer mcpServer;

    @PostConstruct
    public void init() {
        mcpServer.start(); // 在独立端口 8082 启动
    }
}
```

### 2.2 HTTP + SSE 实现

使用 Spring MVC 的 `ResponseBodyEmitter` 实现 SSE：

```java
@GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public ResponseBodyEmitter stream() {
    ResponseBodyEmitter emitter = new ResponseBodyEmitter();
    mcpServer.registerEmitter(emitter);
    return emitter;
}
```

### 2.3 MCP 消息格式

MCP 协议使用 JSON-RPC 2.0 格式：

**Request**:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/list",
  "params": {}
}
```

**Response**:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "h3_coding_hub_tool_search",
        "description": "Search tools in CodingHub",
        "inputSchema": {...}
      }
    ]
  }
}
```

## 3. MCP 工具定义

根据 FR-003 至 FR-007，定义以下 MCP 工具：

| 工具名 | 功能 | 参数 |
|--------|------|------|
| h3_coding_hub_tool_search | 搜索工具 | query, category, limit |
| h3_coding_hub_tool_get | 获取工具详情 | toolId |
| h3_coding_hub_tool_files | 获取工具文件列表 | toolId |
| h3_coding_hub_post_search | 搜索帖子 | query, limit |
| h3_coding_hub_post_get | 获取帖子内容 | postId |

## 4. 数据库查询优化

### 4.1 工具检索

```sql
SELECT t.*, c.name as category_name
FROM tool t
LEFT JOIN category c ON t.category_id = c.id
WHERE t.status = 'approved'
  AND (t.name LIKE CONCAT('%', ?, '%') OR t.content LIKE CONCAT('%', ?, '%'))
ORDER BY t.created_at DESC
LIMIT ?
```

### 4.2 帖子检索

```sql
SELECT p.*, u.username as author_name
FROM post p
LEFT JOIN user u ON p.author_id = u.id
WHERE p.title LIKE CONCAT('%', ?, '%')
   OR p.content LIKE CONCAT('%', ?, '%')
ORDER BY p.created_at DESC
LIMIT ?
```

## 5. 异常处理策略

| 场景 | 处理方式 |
|------|----------|
| 工具不存在 | 返回 MCP 错误码: -32602 (Invalid params) |
| 数据库错误 | 记录日志，返回 500 错误 |
| MCP Server 启动失败 | 仅记录错误，不影响主应用 |
| 连接超时 | 返回超时错误，客户端自动重连 |

## 6. 性能考虑

- 数据库连接池: 使用 HikariCP（Spring Boot 默认）
- 索引优化: tool.name, tool.category_id, post.title, post.content
- 分页: 默认 limit=20，最大 limit=100
- 缓存: 可考虑对热门工具结果缓存（可选优化）