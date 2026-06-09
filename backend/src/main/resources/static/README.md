# CodingHub

> 一个全栈 Web 应用，提供 AI 工具/资源的管理和展示平台。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.5 |
| 编程语言 | Java | 17 |
| 前端框架 | Vue 3 | 3.x |
| 构建工具 | Vite | 5.x |
| 数据库 | MySQL | 8.x |
| 认证 | JWT | 0.12.5 |

## 项目结构

```
iaihub/
├── backend/                    # Java Spring Boot 后端
│   └── src/main/java/com/iaihub/toolbox/
│       ├── controller/        # REST API 控制器
│       ├── service/           # 业务逻辑层
│       ├── repository/        # 数据访问层 (JPA)
│       ├── model/             # 实体类
│       ├── dto/               # 数据传输对象
│       ├── config/            # 配置类 (Security, JWT, Upload)
│       ├── exception/         # 异常处理
│       └── util/              # 工具类
├── frontend/                  # Vue 3 + TypeScript 前端
│   └── src/
│       ├── components/        # Vue 组件
│       ├── pages/             # 页面
│       ├── services/          # API 调用
│       ├── stores/            # 状态管理
│       ├── router/            # 路由配置
│       └── types/             # TypeScript 类型定义
├── docs/                      # 详细文档
├── design-system/             # 设计系统
├── harness/                   # Agent 基础设施
├── scripts/                   # 脚本工具
├── specs/                     # 功能规格说明
└── Makefile                   # 快速命令
```

## 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- MySQL 8.x
- npm 或 yarn

### 启动服务

```bash
# 初始化数据库
make db

# 安装前端依赖
make install

# 启动后端 (8080)
make backend

# 启动前端 (5173)
make frontend

# 同时启动后端+前端
make run

# 停止所有服务
make stop
```

### 端口配置

| 服务 | 端口 | 说明 |
|------|------|------|
| 后端 | 8080 | Spring Boot 应用 |
| 前端 | 5173 | Vite 开发服务器 |
| MySQL | 3306 | 数据库服务 |

## API 入口点

### 后端 API (http://localhost:8080)

#### 认证相关

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/register` | 用户注册 | 否 |
| POST | `/api/auth/login` | 用户登录 | 否 |
| GET | `/api/auth/me` | 获取当前用户 | 是 |

#### 工具相关

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/tools` | 工具列表 (分页) | 否 |
| GET | `/api/tools/{id}` | 工具详情 | 否 |
| POST | `/api/tools` | 创建工具 | 是 |
| PUT | `/api/tools/{id}` | 更新工具 | 是 |
| DELETE | `/api/tools/{id}` | 删除工具 | 是 |

#### 分类相关

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/categories` | 分类列表 | 否 |

#### 文件上传

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/files/upload` | 上传文件 | 是 |
| GET | `/api/files/{id}` | 下载文件 | 是 |

### 前端页面 (http://localhost:5173)

| 路径 | 说明 |
|------|------|
| `/` | 首页/工具列表 |
| `/login` | 登录页 |
| `/register` | 注册页 |
| `/tools/{id}` | 工具详情 |
| `/profile` | 用户中心 |

## 数据库表结构

- **user**: id, email, password, username, created_at, updated_at, last_login_at
- **category**: id, name, icon, sort_order, created_at
- **tool**: id, name, category_id, content, uploader_id, status, created_at, updated_at
- **tool_file**: id, tool_id, file_path, file_name, file_size, created_at

## 核心功能

- **用户认证**: 注册、登录、JWT 令牌认证
- **工具管理**: CRUD 操作，支持分类筛选
- **文件上传**: 工具相关文件上传与下载
- **XSS 防护**: 所有用户输入内容过滤

## 设计风格

本项目采用 **Cyberpunk Glassmorphism** 暗色主题风格：

- **背景色**: `#0D0D0D` 深黑
- **主色调**: `#00FFFF` Cyan（科技感）
- **辅助色**: `#FF00FF` Magenta
- **成功色**: `#00FF00` Matrix Green
- **字体**: Fira Code + Fira Sans

详细设计规范请参阅 [design-system/CodingHub/MASTER.md](design-system/CodingHub/MASTER.md)。

## 约束规则

### 代码约束

1. **禁止循环依赖**: controller → service → repository → model 单向依赖
2. **XSS 防护**: 所有用户输入通过 `XssSanitizer.sanitize()` 过滤
3. **JWT 认证**: 需要认证的接口在 Header 携带 `Authorization: Bearer <token>`
4. **禁止 null 返回**: 方法不返回 null，抛异常或返回 Optional
5. **禁止循环调用**: 禁止在循环中请求数据库或调用接口

### Git 约束

1. **禁止私自提交**: 需求开发过程中不得私自提交代码
2. 提交信息遵循 Conventional Commits 格式
3. 单次提交不超过 1000 行更改

## 相关文档

- [架构详情](docs/ARCHITECTURE.md) - 详细架构说明
- [Agent 导航地图](AGENTS.md) - AI 代理快速参考
- [环境配置](harness/config/environment.json) - 运行时环境变量

---

**最后更新**: 2026-06-01