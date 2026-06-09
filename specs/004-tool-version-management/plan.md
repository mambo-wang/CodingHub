# 实现计划：工具版本管理

**分支**：`004-tool-version-management` | **日期**：2026-06-04 | **规格说明**：[spec.md](./spec.md)

**输入**：来自 `/specs/004-tool-version-management/spec.md` 的功能规格说明

## 摘要

本功能为 CodingHub系统添加版本号管理能力和增强的文件管理功能：

1. **版本号支持**：在创建和修改工具时支持输入版本号，版本号在同用户+同分类下唯一
2. **增强文件管理**：修改工具时支持文件删除和重新上传，同名文件自动替换，不同名文件追加

## 技术上下文

**语言/版本**：Java 17 / Spring Boot 3.2.5（后端）、Vue 3 + TypeScript（前端）

**构建工具**：Gradle（后端）、npm + Vite（前端）

**主要依赖**：
- 后端：Spring Boot, Spring Security, Spring Data JPA, MySQL Connector, Lombok
- 前端：Vue 3, Vue Router, Pinia, Axios, TypeScript

**存储**：MySQL 8.x（用户: root, 密码: root）

**测试框架**：JUnit 5 + Mockito（后端）、Vitest（前端）

**目标平台**：Windows/Linux 服务器

**项目类型**：全栈 Web 应用（后端 API + 前端 SPA）

**端口**：后端 8081，前端 5173

**性能目标**：API p95 < 200ms（读）、< 500ms（写）；UI 60fps

**约束条件**：JWT 认证、XSS 防护、无 null 返回

## 宪法检查

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 代码质量标准 | ✅ 通过 | 遵循分层架构（L0-L4） |
| 测试标准 | ✅ 通过 | 新功能需 TDD 循环，覆盖率 ≥ 80% |
| 安全要求 | ✅ 通过 | JWT 认证、XSS 防护（XssSanitizer） |
| 无循环依赖 | ✅ 通过 | controller → service → repository → model |
| 方法不返回 null | ✅ 通过 | 使用 Optional 或抛出异常 |

## 研究发现

### 1. 语义化版本号（SemVer）验证

**决策**：使用正则表达式验证版本号格式

**理由**：SemVer 是行业标准格式（MAJOR.MINOR.PATCH），支持预发布版本后缀

**实现**：`^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9]+)?$`

### 2. 文件同名检测策略

**决策**：后端检测同名文件并自动替换

**理由**：前端上传时无法预知服务器端文件名，需要后端处理

**实现**：
1. 上传前查询工具现有文件列表
2. 比对文件名，存在则删除旧文件记录和物理文件
3. 保存新文件

## 项目结构

### 文档（此功能）

```text
specs/004-tool-version-management/
├── plan.md              # 本文件
├── research.md          # 研究发现
├── data-model.md        # 数据模型设计
├── quickstart.md        # 快速开始指南
├── contracts/           # API 契约文档
│   └── tool-api.md      # 工具 API 详细契约
└── tasks.md             # 任务列表（/speckit.tasks 生成）
```

### 源代码（仓库根目录）

```text
# 后端 - Java Spring Boot
backend/src/main/java/com/iaihub/toolbox/
├── controller/
│   └── ToolController.java      # 新增 version 字段支持
├── service/
│   └── ToolService.java          # 新增版本号校验、文件替换逻辑
├── repository/
│   └── ToolRepository.java       # 新增版本号唯一性查询
├── model/
│   └── Tool.java                 # 新增 version 字段
├── dto/
│   ├── CreateToolRequest.java    # 新增 version 字段 + 校验
│   ├── UpdateToolRequest.java    # 新增 version 字段
│   ├── ToolDetailDTO.java       # 新增 version 字段
│   └── ToolSummaryDTO.java       # 新增 version 字段
├── exception/
│   └── DuplicateVersionException.java  # 新增版本号冲突异常

# 前端 - Vue 3 TypeScript
frontend/src/
├── pages/
│   ├── CreateTool.vue           # 新增版本号输入框
│   └── EditTool.vue             # 新增版本号编辑、文件管理
├── components/
│   └── FileUploader.vue         # 增强：支持同名替换提示
└── stores/
    └── toolStore.ts             # 新增版本号相关状态
```

## API 设计约定

### RESTful 端点（扩展）

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| GET | /api/v1/tools | 列表（含版本号） | 可选 |
| GET | /api/v1/tools/{id} | 详情（含版本号） | 可选 |
| POST | /api/v1/tools | 创建（含版本号） | 必须 |
| PUT | /api/v1/tools/{id} | 更新（含版本号） | 必须 |
| DELETE | /api/v1/tools/{id} | 删除工具及关联文件 | 必须 |
| POST | /api/v1/tools/{id}/files | 上传文件（同替换逻辑） | 必须 |
| DELETE | /api/v1/tools/{id}/files/{fileId} | 删除工具文件 | 必须 |

### 请求/响应格式扩展

**创建工具请求**：
```json
{
  "name": "图像识别工具",
  "content": "这是一个图像识别工具",
  "categoryId": 1,
  "version": "1.0.0"
}
```

**错误响应（版本号冲突）**：
```json
{
  "code": 409,
  "message": "该分类下已存在同名工具",
  "data": {
    "existingToolId": 123,
    "existingVersion": "1.0.0"
  }
}
```

## 数据模型变更

### Tool 实体变更

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| version | VARCHAR(50) | NOT NULL | 版本号 |

### 唯一性约束

**变更前**：`uk_tool_uploader_name` (uploader_id, name, status)

**变更后**：`uk_tool_uploader_name_category` (uploader_id, name, category_id, status)

## 测试策略

### 后端测试（JUnit 5 + Mockito）

- **Controller 测试**：MockMvc 集成测试，验证版本号字段处理
- **Service 测试**：Mockito 单元测试，验证版本号校验逻辑
- **Repository 测试**：@DataJpaTest + H2 内存数据库，验证唯一性约束

### 前端测试（Vitest）

- **组件测试**：@vue/test-utils，测试版本号输入框
- **E2E 测试**：可选（Playwright）

### TDD 红绿重构

所有功能开发必须遵循：
1. **Red**：先写失败的测试
2. **Green**：实现最小代码让测试通过
3. **Refactor**：重构优化，保持测试通过

## 实现任务概览

| 序号 | 任务 | 优先级 |
|------|------|--------|
| 1 | 数据库迁移：Tool 表添加 version 字段 | P1 |
| 2 | Tool 实体添加 version 字段及校验注解 | P1 |
| 3 | CreateToolRequest 添加 version 字段 | P1 |
| 4 | UpdateToolRequest 添加 version 字段 | P1 |
| 5 | ToolDetailDTO/ToolSummaryDTO 添加 version 字段 | P1 |
| 6 | ToolService 添加入版本号校验逻辑 | P1 |
| 7 | ToolFileService 增强同名文件替换逻辑 | P1 |
| 8 | 前端 CreateTool.vue 添加版本号输入框 | P1 |
| 9 | 前端 EditTool.vue 添加版本号编辑和文件管理 | P1 |
| 10 | 单元测试编写 | P2 |
| 11 | MCP ToolSearchResult 添加 version 字段 | P1 |
| 12 | McpSearchService 更新工具搜索结果 | P1 |
