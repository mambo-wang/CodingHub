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

## 数据库（双库共存，配置切换）

- 应用同时兼容 MySQL 与 PostgreSQL，通过 Spring Profile 选择，业务代码零改动（见 OpenSpec change `dual-database-config-driven`）。
- 默认 profile = mysql（向后兼容）；`--spring.profiles.active=postgresql` 切换。
- MySQL: 主机 localhost:3306, 用户 root, 密码 root, 数据库 ai_tool_square
- PostgreSQL: 主机 localhost:5432, 用户 codinghub, 密码 codinghub, 数据库 ai_tool_square
- 关键约束（双库共存实现要点，已实测）：
  - `application.yml` 移除硬编码方言（Hibernate 6 按 JDBC 连接自动探测）。两个 JDBC 驱动共存于 build.gradle。
  - `User.java` **保留** `@Table(name = "`user`")` 反引号；`globally_quoted_identifiers=true` **仅放在 postgresql profile 段**（不放 common/test，否则 H2 MODE=MySQL 把双引号当字符串导致建表失败）。PG 下 Hibernate 把反引号归一化为 `"user"`。
  - 实体中 `columnDefinition = "TEXT"` 须用小写 `"text"`（PG 全局引号会把 `TEXT` 引为无效的 `"TEXT"`）。
  - PG 初始化：`scripts/init-db-postgres.sql` 仅种子（Schema 由 Hibernate ddl-auto 生成）；`make db-pg` 建库、`make db-pg-seed` 种子。**已修复**：脚本曾误写 `category.updated_at`（实体无此列，有 `logo_url`）与 `forum_category.icon`（实体无此列），导致种子 0 行、分类接口返回空，现已去除，`make db-pg-seed` 可正确写入 6 工具分类 + 4 论坛分类。
  - 测试：`./gradlew test` 基线 10 失败为预存在，双库改动不新增失败。
  - **默认 profile 必须为 `mysql`**（`spring.profiles.default: mysql`）：种子数据在 MySQL，且此前曾误设为 postgresql 导致空库+残留进程占端口。切换 PG 用 `--spring.profiles.active=postgresql`。
  - **接口路径坑**：论坛相关接口是 `/api/forum/...`（**不含 /v1**），其余模块是 `/api/v1/...`。调用论坛接口时不要多写 `/v1`。

## MCP Server

- 嵌入式 MCP Server 已实现，运行在 http://localhost:8080/mcp
- 提供 5 个 MCP 工具：h3_coding_hub_tool_search, h3_coding_hub_tool_get, h3_coding_hub_tool_files, h3_coding_hub_post_search, h3_coding_hub_post_get
- 健康检查端点: GET /mcp/health
- 消息端点: POST /mcp (JSON-RPC 2.0)
- SSE 端点: GET /mcp/sse