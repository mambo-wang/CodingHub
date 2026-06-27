# 开发指南

## 1. 环境准备

### 1.1 必要依赖

| 依赖 | 版本 | 说明 |
|------|------|------|
| Java | 17+ | 后端运行环境 |
| Node.js | 18+ | 前端运行环境 |
| npm | 9+ | 前端包管理 |
| MySQL | 8.x | 数据库 |
| Gradle | 8.x | 后端构建 (项目自带 wrapper) |

### 1.2 数据库初始化

```bash
make db
```

这会创建 `ai_tool_square` 数据库并初始化表结构。

## 2. 开发命令

### 2.1 启动服务

```bash
# 仅后端 (8082端口)
make backend

# 仅前端 (5173端口)
make install   # 首次需要安装依赖
make frontend

# 同时启动后端和前端
make run

# 停止所有服务
make stop
```

### 2.2 代码检查

```bash
# 运行所有 lint 检查
make lint

# 架构层级检查
make lint-arch

# 代码质量检查
make lint-quality

# 循环依赖检查
make lint-deps
```

## 3. 项目结构

```
CodingHub/
├── backend/                     # Java Spring Boot 后端
│   └── src/main/java/com/iaihub/toolbox/
│       ├── controller/           # REST API (11 核心 + 11 子模块)
│       │   ├── forum/            #   论坛模块
│       │   ├── video/            #   微课模块
│       │   ├── feedback/         #   留言反馈
│       │   ├── kb/               #   知识库
│       │   ├── notification/     #   通知
│       │   └── tag/              #   统一标签
│       ├── service/              # 业务逻辑 (22)
│       ├── repository/           # JPA 数据访问 (26)
│       ├── model/                # JPA 实体 (35)
│       ├── dto/                  # 数据传输对象 (61)
│       ├── config/               # 配置 (7): Security, JWT, MCP, Upload, RAG
│       ├── exception/            # 异常处理 (9)
│       ├── util/                 # 工具类 (2)
│       └── mcp/                  # MCP 协议 (4 文件, 17 tools)
│   └── src/main/resources/db/migration/  # Flyway 迁移 (V1~V9)
├── frontend/                     # Vue 3 前端
│   └── src/
│       ├── components/           # 组件 (34)
│       │   ├── common/           #   通用 (ConfirmDialog, NotificationBell, 互动组件等)
│       │   ├── forum/            #   论坛组件
│       │   ├── video/            #   视频组件
│       │   ├── feedback/         #   留言组件
│       │   └── knowledge/        #   知识库组件
│       ├── pages/                # 页面 (29)
│       │   ├── admin/            #   管理页面
│       │   ├── forum/            #   论坛页面
│       │   ├── video/            #   微课页面
│       │   ├── knowledge/        #   知识库页面
│       │   └── feedback/         #   留言反馈页面
│       ├── services/             # API 调用 (9)
│       ├── stores/               # 状态管理 (3): auth, forum, theme
│       ├── router/               # Vue Router
│       ├── types/                # 类型定义 (7)
│       └── composables/          # 组合式函数 (2)
├── design-system/               # 设计系统规范 (双主题)
├── docs/                        # 文档
├── harness/                     # Agent 基础设施
├── specs/                       # 功能规格说明
├── scripts/                     # Lint + 迁移脚本
└── Makefile                     # 快速命令
```

## 4. API 开发

### 4.1 后端 API 结构

控制器位于 `backend/src/main/java/com/iaihub/toolbox/controller/`

```java
@RestController
@RequestMapping("/api/tools")
public class ToolController {

    @GetMapping
    public ApiResponse<List<ToolSummaryDTO>> getTools(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        // ...
    }

    @PostMapping
    public ApiResponse<ToolDetailDTO> createTool(@Valid @RequestBody CreateToolRequest request) {
        // ...
    }
}
```

### 4.2 前端 API 调用

服务位于 `frontend/src/services/`

```typescript
// services/tools.ts
export const getTools = async (page: number, size: number) => {
  const response = await axios.get('/api/tools', { params: { page, size } })
  return response.data.data
}

export const createTool = async (data: CreateToolRequest) => {
  const response = await axios.post('/api/tools', data)
  return response.data.data
}
```

## 5. 数据库操作

### 5.1 实体定义

```java
@Entity
@Table(name = "tool")
public class Tool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    // ...
}
```

### 5.2 Repository

```java
@Repository
public interface ToolRepository extends JpaRepository<Tool, Long> {

    // 自定义查询
    Page<Tool> findByStatus(ToolStatus status, Pageable pageable);

    Optional<Tool> findByIdAndStatus(Long id, ToolStatus status);
}
```

## 6. 安全规则

### 6.1 分层依赖

```
controller (L4) → service (L3) → repository (L2) → model (L1)
                    ↓
                  config (L0), util (L0)
```

**禁止**：
- L0 (config/util) 依赖任何内部包
- L1 (model/dto) 依赖 L2, L3, L4
- L2 (repository) 依赖 L3, L4
- service 层直接依赖 controller

### 6.2 代码约束

1. **禁止返回 null**：使用 `Optional` 或抛异常
2. **禁止循环依赖**：检查 `scripts/lint-deps.sh`
3. **禁止循环中调用数据库**：检查 `scripts/lint-quality.sh`

## 7. 测试

### 7.1 后端测试

```bash
# 运行全部后端测试
cd backend && ./gradlew test

# 运行指定测试类
cd backend && ./gradlew test --tests "com.iaihub.toolbox.service.ToolServiceTest"

# 运行指定测试方法
cd backend && ./gradlew test --tests "com.iaihub.toolbox.service.ToolServiceTest.testCreateTool"
```

测试报告位于 `backend/build/reports/tests/test/`。

### 7.2 前端类型检查

```bash
# TypeScript 类型检查（不生成输出文件）
cd frontend && npx vue-tsc --noEmit
```

> **注意**：前端目前使用 `vue-tsc` 做静态类型检查，无独立测试框架。如需添加单元测试，可引入 Vitest。

---

## 8. 环境变量

| 变量 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `MYSQL_PASSWORD` | 是 | `root` | MySQL 数据库密码 |
| `JAVA_HOME` | 是 | — | JDK 17 安装路径，如 `/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home` |
| `AIHUB_FILE_BASE_DIR` | 否 | `~/.aifiles` | 后端文件上传存储的根目录 |
| `VITE_API_BASE_URL` | 否 | `http://localhost:8082` | 前端请求后端 API 的基础地址 |
| `VITE_BACKEND_PORT` | 否 | `8082` | 前端 MCP 代理连接的后端端口 |
| `BACKEND_PORT` | 否 | `8082` | Vite 开发代理 (proxy) 的目标端口 |
| `JWT_SECRET` | 否 | 自动生成 | JWT 签名密钥，生产环境必须显式设置 |

环境变量可通过以下方式设置：

```bash
# 方式一：导出到 shell
export MYSQL_PASSWORD=root
export AIHUB_FILE_BASE_DIR=/Users/kirito/aifiles
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# 方式二：通过 .env 文件（前端 Vite 自动加载）
# frontend/.env.local
VITE_API_BASE_URL=http://localhost:8082
VITE_BACKEND_PORT=8082
```

---

## 9. 常见问题排查

### 9.1 端口冲突

**现象**：启动时报 `Address already in use` 或 `Port 8082/5173 is already in use`。

```bash
# 查看占用端口的进程
lsof -i :8082
lsof -i :5173

# 终止占用进程
kill -9 <PID>

# 或通过 make 停止所有服务
make stop
```

### 9.2 MySQL 连接失败

**现象**：后端启动报 `Communications link failure` 或 `Access denied`。

```bash
# 确认 MySQL 服务正在运行
mysqladmin ping -h localhost -u root -proot

# 确认数据库存在
mysql -u root -proot -e "SHOW DATABASES LIKE 'ai_tool_square'"

# 重新初始化数据库
make db
```

> 如果 MySQL 通过 Homebrew 安装，使用 `brew services start mysql` 启动。

### 9.3 Gradle Daemon 问题

**现象**：`./gradlew` 命令卡住、内存溢出或报 `Daemon startup failed`。

```bash
# 停止所有 Gradle Daemon
cd backend && ./gradlew --stop

# 强制清理 Daemon 进程
pkill -f "GradleDaemon"

# 使用 --no-daemon 模式运行（CI 环境推荐）
cd backend && ./gradlew bootRun --no-daemon
```

### 9.4 Node.js 版本不兼容

**现象**：前端启动报 `SyntaxError`、`ERR_UNSUPPORTED` 或 Vite 编译失败。

```bash
# 检查当前 Node.js 版本
node -v   # 需要 >= 18

# 使用 nvm 切换版本
nvm install 18
nvm use 18

# 清理并重新安装依赖
cd frontend && rm -rf node_modules package-lock.json && npm install
```

---

**最后更新**: 2026-05-29