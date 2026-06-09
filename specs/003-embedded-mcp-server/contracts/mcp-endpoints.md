# MCP HTTP Endpoint Contract

**Feature**: 003-embedded-mcp-server
**Date**: 2026-05-31

## 1. MCP Server 端点

MCP Server 运行在独立端口 `8082`，提供以下 HTTP 端点：

### 1.1 健康检查

```
GET /mcp/health
```

**Response**:
```json
{
  "status": "ok",
  "version": "1.0.0",
  "timestamp": "2026-05-31T20:00:00Z"
}
```

### 1.2 MCP 消息接收（POST）

```
POST /mcp
Content-Type: application/json

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
        "inputSchema": {
          "type": "object",
          "properties": {
            "query": {"type": "string"},
            "category": {"type": "string"},
            "limit": {"type": "integer", "default": 20}
          }
        }
      }
    ]
  }
}
```

### 1.3 SSE 事件流

```
GET /mcp/sse
```

**Response**: `text/event-stream` (SSE)

```
event: message
data: {"jsonrpc":"2.0","id":1,"result":{"tools":[...]}}

event: ping
data: {"timestamp":"2026-05-31T20:00:00Z"}
```

### 1.4 工具搜索

```
POST /mcp
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": {
    "name": "h3_coding_hub_tool_search",
    "arguments": {
      "query": "图片处理",
      "limit": 10
    }
  }
}
```

**Response**:
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "[{\"id\":1,\"name\":\"图像处理工具\",\"category\":\"工具\",\"description\":\"提供图像处理能力\"}]"
      }
    ]
  }
}
```

### 1.5 工具详情

```
POST /mcp
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "h3_coding_hub_tool_get",
    "arguments": {
      "toolId": 123
    }
  }
}
```

**Response**:
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "# 图像处理工具\n\n这是工具的 Markdown 文档内容..."
      }
    ]
  }
}
```

### 1.6 工具文件列表

```
POST /mcp
{
  "jsonrpc": "2.0",
  "id": 4,
  "method": "tools/call",
  "params": {
    "name": "h3_coding_hub_tool_files",
    "arguments": {
      "toolId": 123
    }
  }
}
```

**Response**:
```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "[{\"fileName\":\"tool.zip\",\"fileSize\":1024000,\"downloadUrl\":\"/api/files/download/1\"}]"
      }
    ]
  }
}
```

### 1.7 帖子搜索

```
POST /mcp
{
  "jsonrpc": "2.0",
  "id": 5,
  "method": "tools/call",
  "params": {
    "name": "h3_coding_hub_post_search",
    "arguments": {
      "query": "如何使用",
      "limit": 10
    }
  }
}
```

**Response**:
```json
{
  "jsonrpc": "2.0",
  "id": 5,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "[{\"id\":1,\"title\":\"使用指南\",\"authorName\":\"张三\",\"createdAt\":\"2026-05-30\"}]"
      }
    ]
  }
}
```

### 1.8 帖子内容

```
POST /mcp
{
  "jsonrpc": "2.0",
  "id": 6,
  "method": "tools/call",
  "params": {
    "name": "h3_coding_hub_post_get",
    "arguments": {
      "postId": 456
    }
  }
}
```

**Response**:
```json
{
  "jsonrpc": "2.0",
  "id": 6,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "# 帖子标题\n\n这是帖子的完整正文内容..."
      }
    ]
  }
}
```

## 2. 错误响应格式

```json
{
  "jsonrpc": "2.0",
  "id": null,
  "error": {
    "code": -32602,
    "message": "Invalid params: tool not found",
    "data": null
  }
}
```

**错误码**:
| 错误码 | 说明 |
|--------|------|
| -32600 | Invalid Request |
| -32602 | Invalid Params |
| -32603 | Internal Error |
| -32000 | Database Error |

## 3. 文件下载

文件下载通过标准 HTTP 完成，不走 MCP 协议：

```
GET /api/files/download/{fileId}
```

Response: 文件流（application/octet-stream）