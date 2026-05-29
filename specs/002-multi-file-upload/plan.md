# Implementation Plan: 工具上传功能优化 - 多文件支持

**Branch**: `001-ai-tool-square` | **Date**: 2026-05-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-multi-file-upload/spec.md`

## Summary

优化工具上传功能，添加多文件上传支持。前端页面支持选择多个文件并显示文件列表，后端将文件存储到以工具ID命名的独立文件夹中，同时将README内容保存为readme.md文件。

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.2.5) / Vue 3.4 + TypeScript 5.4

**Primary Dependencies**:
- Backend: Spring Boot Web, Spring Data JPA, Spring Security, MySQL Connector
- Frontend: Vue 3, Element Plus 2.7, Axios, Markdown-it

**Storage**: MySQL (工具元数据) + 本地文件系统 (上传文件)

**Testing**: JUnit 5 (Backend), Vitest (Frontend)

**Target Platform**: Web (Desktop/Mobile responsive)

**Project Type**: Full-stack web application

**Performance Goals**:
- API 响应时间 p95 < 500ms (写入操作)
- 支持单次上传至少 10 个文件

**Constraints**:
- 单文件大小限制: 50MB
- 总上传大小限制: 200MB
- 仅允许特定文件类型上传

## Constitution Check

*以下宪法条款需要检查:*

| 条款 | 检查项 | 状态 |
|------|--------|------|
| I. Code Quality | 类型安全、错误处理 | ✅ 使用 Optional/异常处理 |
| II. Testing | TDD、覆盖率 | ⚠️ 需要为新功能编写测试 |
| III. UX Consistency | 反馈一致性、错误提示 | ✅ 使用 ElMessage 统一反馈 |
| IV. Performance | 响应时间、文件大小限制 | ✅ 设置合理限制 |
| V. Observability | 结构化日志 | ✅ 已配置 logstash-logback-encoder |
| VI. Simplicity | YAGNI、避免过度设计 | ✅ 简单文件存储方案 |

**结论**: 宪法检查通过，无需额外复杂设计。

## Project Structure

### Documentation (this feature)

```text
specs/002-multi-file-upload/
├── plan.md              # 本文件
├── research.md          # Phase 0 输出 - 技术选型研究
├── spec.md             # 功能规范
├── data-model.md        # Phase 1 输出 - 数据模型
├── contracts/           # Phase 1 输出 - 接口契约
│   └── tool-files-api.md
├── quickstart.md        # Phase 1 输出 - 开发者快速指南
└── tasks.md             # Phase 2 输出 (由 /speckit.tasks 生成)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/iaihub/toolbox/
│   ├── controller/
│   │   └── ToolFileController.java     # [NEW] 文件上传控制器
│   ├── service/
│   │   ├── ToolService.java            # [MODIFY] 创建工具时支持文件关联
│   │   └── ToolFileService.java        # [NEW] 文件存储服务
│   ├── model/
│   │   └── ToolFile.java               # [NEW] 文件元数据实体
│   ├── repository/
│   │   └── ToolFileRepository.java     # [NEW] 文件仓库
│   ├── dto/
│   │   └── ToolFileDTO.java            # [NEW] 文件传输对象
│   └── config/
│       └── WebConfig.java              # [MODIFY] 文件上传配置
├── src/main/resources/
│   └── application.yml                 # [MODIFY] 添加文件上传配置
└── uploads/                            # [NEW] 文件存储根目录

frontend/
├── src/
│   ├── pages/
│   │   └── UploadPage.vue              # [MODIFY] 添加文件上传组件
│   ├── services/
│   │   └── api.ts                      # [MODIFY] 添加文件上传接口
│   └── types/
│       └── index.ts                    # [MODIFY] 添加文件相关类型
└── package.json                        # [MODIFY] 如需新增依赖
```

**Structure Decision**:
- 采用 Option 2: Web application 结构 (backend + frontend)
- 后端新增 ToolFileController、ToolFileService、ToolFile 实体
- 前端在 UploadPage 中集成 Element Plus el-upload 组件
- 文件存储使用本地文件系统，根目录为 `uploads/tools/{toolId}/`

## Complexity Tracking

> 无复杂度违规。所有设计均为满足需求的最小实现。

## Implementation Phases

### Phase 1: 后端文件存储服务

1. 创建 `ToolFile` 实体和 `ToolFileRepository`
2. 创建 `ToolFileService` 处理文件存储逻辑
3. 创建 `ToolFileController` 提供文件上传/下载/删除API
4. 配置 `application.yml` 添加文件上传限制
5. 修改 `ToolService` 在删除工具时清理文件

### Phase 2: 前端多文件上传组件

1. 修改 `UploadPage.vue` 添加 el-upload 组件
2. 实现文件选择后显示列表和移除功能
3. 提交时先创建工具记录获取ID，再上传文件
4. 添加上传进度显示

### Phase 3: API 集成

1. 后端修改 `createTool` 接口支持同时接收文件和元数据
2. 前端修改提交逻辑支持多文件上传
3. 添加错误处理和状态提示

### Phase 4: 测试与验证

1. 单元测试: ToolFileService 文件存储逻辑
2. 集成测试: 完整上传流程
3. 前端测试: 文件选择、移除、进度显示

## Dependencies

- Element Plus el-upload (已使用)
- 无需新增后端依赖
