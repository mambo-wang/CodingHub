# 内存 - AI 工具广场项目

## 项目核心规则

1. **禁止私自提交代码**: 需求开发过程中不得私自提交代码，提交代码必须经过人工确认
2. 禁止在循环中请求数据库或调用接口
3. 需要遍历集合时，优先使用 for 循环，尽量不使用 while/foreach/stream/iterator
4. 方法不要返回 null，可以用抛异常或者返回 Optional 代替

## 项目架构

- 后端: Java 17 / Spring Boot 3.2.5 + MySQL
- 前端: Vue 3 / TypeScript / Vite
- 端口: 后端 8080, 前端 5173

## 设计系统

- 风格: Cyberpunk Glassmorphism 暗色主题
- 配色: #0D0D0D 深黑底, #00FFFF Cyan / #FF00FF Magenta / #00FF00 Matrix Green 强调色
- 图标: @lucide/vue-next

## 数据库

- MySQL, 主机 localhost:3306, 用户 root, 密码 root, 数据库 ai_tool_square

## MCP Server

- 嵌入式 MCP Server 已实现，运行在 http://localhost:8080/mcp
- 提供 5 个 MCP 工具：h3_coding_hub_tool_search, h3_coding_hub_tool_get, h3_coding_hub_tool_files, h3_coding_hub_post_search, h3_coding_hub_post_get
- 健康检查端点: GET /mcp/health
- 消息端点: POST /mcp (JSON-RPC 2.0)
- SSE 端点: GET /mcp/sse