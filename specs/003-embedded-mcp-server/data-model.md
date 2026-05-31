# Data Model: 嵌入式 MCP Server

**Feature**: 003-embedded-mcp-server
**Date**: 2026-05-31

## 1. 复用现有数据模型

本功能复用工具广场现有的数据模型，无需新增数据库表。

### 1.1 Tool（工具）

对应现有 `tool` 表：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 工具名称 |
| content | String | Markdown 文档内容（FR-004） |
| category_id | Long | 关联分类 |
| uploader_id | Long | 上传者 ID |
| status | String | 状态（approved/rejected/pending） |
| created_at | Timestamp | 创建时间 |
| updated_at | Timestamp | 更新时间 |

### 1.2 Post（帖子）

对应现有 `post` 表：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| title | String | 帖子标题 |
| content | String | 正文内容（FR-007） |
| author_id | Long | 作者 ID |
| created_at | Timestamp | 创建时间 |
| updated_at | Timestamp | 更新时间 |

### 1.3 Category（分类）

对应现有 `category` 表：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 分类名称 |

### 1.4 ToolFile（工具文件）

对应现有 `tool_file` 表：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| tool_id | Long | 关联工具 ID |
| file_path | String | 文件路径 |
| file_name | String | 文件名 |
| file_size | Long | 文件大小 |
| created_at | Timestamp | 创建时间 |

## 2. MCP 协议数据模型

### 2.1 McpMessage

MCP 消息格式（JSON-RPC 2.0）：

```java
public class McpMessage {
    private String jsonrpc = "2.0";
    private Integer id;
    private String method;
    private Map<String, Object> params;
}
```

### 2.2 McpResponse

MCP 响应格式：

```java
public class McpResponse {
    private String jsonrpc = "2.0";
    private Integer id;
    private Object result;  // 成功时
    private McpError error; // 失败时
}

public class McpError {
    private Integer code;
    private String message;
    private Object data;
}
```

### 2.3 McpTool

MCP 工具定义：

```java
public class McpTool {
    private String name;           // e.g., "h3_coding_hub_tool_search"
    private String description;    // 工具描述
    private Object inputSchema;    // JSON Schema for input
}
```

### 2.4 McpResource

MCP 资源定义：

```java
public class McpResource {
    private String uri;             // e.g., "tool://123"
    private String name;
    private String description;
    private String mimeType;
}
```

## 3. DTO 模型

### 3.1 McpSearchRequest

```java
public class McpSearchRequest {
    private String query;          // 搜索关键词
    private String category;       // 分类筛选（可选）
    private Integer limit;         // 返回数量（默认 20）
}
```

### 3.2 ToolSearchResult

```java
public class ToolSearchResult {
    private Long id;
    private String name;
    private String description;
    private String category;
    private String createdAt;
}
```

### 3.3 PostSearchResult

```java
public class PostSearchResult {
    private Long id;
    private String title;
    private String summary;         // 前 100 字符
    private String authorName;
    private String createdAt;
}
```

## 4. 关系图

```
McpServer
    │
    ├── McpResourceHandler (处理工具检索)
    │       │
    │       └── ToolService (复用现有)
    │              ├── ToolRepository
    │              └── ToolFileRepository
    │
    ├── McpToolHandler (处理帖子检索)
    │       │
    │       └── PostService (复用现有)
    │              └── PostRepository
    │
    └── McpController (HTTP + SSE 端点)
```

## 5. 验证规则

| 字段 | 验证规则 |
|------|----------|
| query | 非空，最大 200 字符 |
| limit | 1-100 范围内整数 |
| toolId | 正整数 |
| postId | 正整数 |