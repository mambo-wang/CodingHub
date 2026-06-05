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
# 仅后端 (8081端口)
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
iaihub/
├── backend/           # Java Spring Boot 后端
│   └── src/main/java/com/iaihub/toolbox/
│       ├── controller/   # REST API
│       ├── service/     # 业务逻辑
│       ├── repository/   # 数据访问
│       ├── model/        # 实体
│       ├── dto/          # 数据传输对象
│       ├── config/       # 配置
│       ├── exception/    # 异常
│       └── util/         # 工具
├── frontend/          # Vue 3 前端
│   └── src/
│       ├── components/   # 组件
│       ├── pages/        # 页面
│       ├── services/     # API 调用
│       ├── stores/       # 状态管理
│       ├── router/       # 路由
│       └── types/        # 类型定义
├── docs/               # 文档
├── harness/            # Agent 基础设施
└── Makefile           # 快速命令
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

```bash
# 后端测试
cd backend && ./gradlew test

# 前端测试 (如果有)
cd frontend && npm run test
```

---

**最后更新**: 2026-05-29