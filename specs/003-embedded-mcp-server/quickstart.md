# Quickstart: 嵌入式 MCP Server

**Feature**: 003-embedded-mcp-server
**Date**: 2026-05-31

## 1. 功能概述

嵌入式 MCP Server 为 AI IDE 提供工具广场的检索和查询能力。

### 核心功能

- 工具检索与详情查询
- 工具文件下载信息
- 帖子检索与内容查询

### 技术栈

- Java 17 + Spring Boot 3.2.5
- MySQL 8.x
- HTTP + SSE (Server-Sent Events)

## 2. 快速开始

### 2.1 启动 MCP Server

MCP Server 随 Spring Boot 应用自动启动，无需额外配置。

```bash
# 启动后端服务
cd backend
./gradlew bootRun

# MCP Server 将在 8082 端口启动
# 主应用 REST API 仍在 8082 端口
```

### 2.2 验证 MCP Server

```bash
# 健康检查
curl http://localhost:8082/mcp/health

# 预期响应
{"status":"ok","version":"1.0.0","timestamp":"2026-05-31T20:00:00Z"}
```

## 3. MCP 工具列表

### 3.1 列出所有工具

```bash
curl -X POST http://localhost:8082/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list",
    "params": {}
  }'
```

### 3.2 搜索工具

```bash
curl -X POST http://localhost:8082/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "h3_coding_hub_tool_search",
      "arguments": {
        "query": "图像处理",
        "limit": 10
      }
    }
  }'
```

### 3.3 获取工具详情

```bash
curl -X POST http://localhost:8082/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "h3_coding_hub_tool_get",
      "arguments": {
        "toolId": 123
      }
    }
  }'
```

### 3.4 获取工具文件

```bash
curl -X POST http://localhost:8082/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "tools/call",
    "params": {
      "name": "h3_coding_hub_tool_files",
      "arguments": {
        "toolId": 123
      }
    }
  }'
```

### 3.5 搜索帖子

```bash
curl -X POST http://localhost:8082/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 5,
    "method": "tools/call",
    "params": {
      "name": "h3_coding_hub_post_search",
      "arguments": {
        "query": "使用指南",
        "limit": 10
      }
    }
  }'
```

### 3.6 获取帖子内容

```bash
curl -X POST http://localhost:8082/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 6,
    "method": "tools/call",
    "params": {
      "name": "h3_coding_hub_post_get",
      "arguments": {
        "postId": 456
      }
    }
  }'
```

## 4. AI IDE 配置

在 AI IDE（如 Cursor, VS Code Cody）中配置 MCP Server：

```json
{
  "mcpServers": {
    "iaihub": {
      "url": "http://localhost:8082/mcp"
    }
  }
}
```

## 5. 测试

```bash
# 运行 MCP 相关测试
cd backend
./gradlew test --tests "*Mcp*"

# 预期: 所有测试通过
```

## 6. 故障排除

### MCP Server 未启动

检查日志中是否有 `MCP Server started on port 8082`：

```bash
tail -f backend/logs/application.log | grep MCP
```

### 连接被拒绝

确保 8082 端口未被占用：

```bash
lsof -i :8082
```

### 数据库连接错误

验证 MySQL 配置：

```bash
# 检查 application.yml 中的数据库配置
cat backend/src/main/resources/application.yml | grep -A 5 spring.datasource
```