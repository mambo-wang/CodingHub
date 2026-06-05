# 任务列表：工具版本管理

**功能分支**：`004-tool-version-management`
**生成日期**：2026-06-04
**输入**：来自 `/specs/004-tool-version-management/` 的设计文档

---

## 格式：`[ID] [P?] [Story] 描述`

- **[P]**：可并行运行（不同文件，无依赖）
- **[Story]**：此任务属于哪个用户故事（如 US1、US2、US3）
- 在描述中包含准确的文件路径

---

## 阶段 1：基础（数据库迁移）

**目的**：Tool 表结构变更，为所有用户故事提供基础

> ⚠️ 注意：MCP 相关任务 T011、T012 已完成

- [X] T001 执行数据库迁移脚本（backend/src/main/resources/db/migration/V4__add_tool_version.sql）
  - 添加 `version` VARCHAR(50) 字段到 tool 表
  - 删除旧唯一约束 `uk_tool_uploader_name`
  - 添加新唯一约束 `uk_tool_uploader_name_category`
  - 添加 `idx_tool_version` 索引

---

## 阶段 2：用户故事 1 - 创建工具时输入版本号（优先级：P1）🎯 MVP

**目标**：用户创建工具时可输入版本号，系统保存并展示版本信息

**独立测试**：创建工具 API 调用，验证响应中包含 version 字段

### 后端实现

- [X] T002 [P] [US1] 更新 Tool 实体：backend/src/main/java/com/iaihub/toolbox/model/Tool.java
  - 添加 `version` 字段（VARCHAR(50), NOT NULL）
  - 添加 `@Builder.Default` 初始值为 "1.0.0"
- [X] T003 [P] [US1] 更新 CreateToolRequest DTO：backend/src/main/java/com/iaihub/toolbox/dto/CreateToolRequest.java
  - 添加 `version` 字段
  - 添加 `@NotBlank` 校验注解
  - 添加 `@Pattern` 校验注解（`^\d+\.\d+\.\d+(-[a-zA-Z0-9]+)?$`）
- [X] T004 [P] [US1] 更新 ToolSummaryDTO：backend/src/main/java/com/iaihub/toolbox/dto/ToolSummaryDTO.java
  - 添加 `version` 字段
- [X] T005 [P] [US1] 更新 ToolDetailDTO：backend/src/main/java/com/iaihub/toolbox/dto/ToolDetailDTO.java
  - 添加 `version` 字段
- [X] T006 [US1] 更新 ToolService：backend/src/main/java/com/iaihub/toolbox/service/ToolService.java
  - createTool 方法中设置 version 字段
- [X] T007 [US1] 更新 ToolController：backend/src/main/java/com/iaihub/toolbox/controller/ToolController.java
  - 确保 createTool 接口响应包含 version 字段

### 前端实现

- [X] T008 [P] [US1] 更新工具类型：frontend/src/types/tool.ts
  - CreateToolRequest 添加 version 字段
  - ToolSummaryDTO/ToolDetailDTO 添加 version 字段
- [X] T009 [US1] 更新 CreateTool.vue 页面：frontend/src/pages/CreateTool.vue
  - 添加版本号输入框组件
  - 添加版本号格式校验（实时反馈）
  - 添加必填提示

**检查点**：用户故事 1 可独立测试 - 创建工具时版本号正常保存和显示

---

## 阶段 3：用户故事 2 - 修改工具时管理文件（优先级：P1）

**目标**：用户可删除已有文件，上传新文件（同名替换，不同名追加）

**独立测试**：编辑工具页面，删除文件、上传同名/不同名文件，验证结果

### 后端实现

- [X] T010 [P] [US2] 添加 ToolFileRepository 方法：backend/src/main/java/com/iaihub/toolbox/repository/ToolFileRepository.java
  - 添加 `findByToolIdAndOriginalNameAndStatus` 方法
- [X] T011 ~~[P]~~ ~~[US2]~~ MCP ToolSearchResult 已完成（version 字段已添加，2026-06-05 实际修复）
- [X] T012 ~~[US2]~~ McpSearchService 已完成（搜索结果包含 version，2026-06-05 实际修复）
- [X] T013 [US2] 更新 ToolFileService：backend/src/main/java/com/iaihub/toolbox/service/ToolFileService.java
  - uploadFiles 方法中添加同名文件检测逻辑
  - 检测到同名文件时：软删除旧记录 + 删除物理文件
  - 然后保存新文件

### 前端实现

- [X] T014 [P] [US2] 更新 FileUploader 组件：frontend/src/components/FileUploader.vue
  - 添加文件列表展示（已有文件）
  - 添加删除按钮和确认对话框
  - 上传时显示"同名文件将被替换"提示
- [X] T015 [US2] 更新 EditTool.vue 页面：frontend/src/pages/EditTool.vue
  - 加载并展示当前工具的文件列表
  - 集成 FileUploader 组件
  - 添加文件删除功能
  - 保存后更新文件列表显示

### 权限控制

- [X] T016 [US2] 添加工具文件删除权限校验：backend/src/main/java/com/iaihub/toolbox/service/ToolFileService.java
  - deleteToolFile 方法中校验用户是否为工具创建者或管理员
  - 无权限时抛出 `AccessDeniedException`

**检查点**：用户故事 2 可独立测试 - 文件删除和同名替换功能正常

---

## 阶段 4：用户故事 3 - 工具版本唯一性保证（优先级：P2）

**目标**：同用户+同分类下工具名称唯一，更新版本号需修改工具

**独立测试**：同一用户+分类下创建同名工具应返回 409 错误

### 后端实现

- [X] T017 [P] [US3] 添加 DuplicateVersionException：backend/src/main/java/com/iaihub/toolbox/exception/DuplicateVersionException.java
  - 包含 existingToolId 和 existingVersion 字段
- [X] T018 [P] [US3] 添加版本号唯一性校验 Repository 方法：backend/src/main/java/com/iaihub/toolbox/repository/ToolRepository.java
  - findByUploaderIdAndNameAndCategoryIdAndStatus 方法
  - findByUploaderIdAndNameAndCategoryIdAndStatusAndIdNot 方法（更新时排除自身）
- [X] T019 [US3] 更新 ToolService 版本号校验：backend/src/main/java/com/iaihub/toolbox/service/ToolService.java
  - createTool 方法中校验版本号唯一性
  - updateTool 方法中校验版本号唯一性（排除自身）
  - 冲突时抛出 DuplicateVersionException
- [X] T020 [US3] 更新 UpdateToolRequest DTO：backend/src/main/java/com/iaihub/toolbox/dto/UpdateToolRequest.java
  - 添加 `version` 字段（可选）
  - 添加 `@Pattern` 校验注解
- [X] T021 [US3] 全局异常处理：backend/src/main/java/com/iaihub/toolbox/exception/GlobalExceptionHandler.java
  - 添加 DuplicateVersionException 处理（返回 409）

**检查点**：用户故事 3 可独立测试 - 版本号唯一性校验正常工作

---

## 阶段 5：收尾与跨领域关注点

**目的**：确保代码质量和文档完整

- [X] T022 [P] 补充单元测试：backend/src/test/java/com/iaihub/toolbox/service/ToolServiceTest.java (2026-06-05)
  - 版本号字段测试（create/update/get 含 version 校验）
  - 版本号唯一性校验测试（同用户+同分类约束，不同分类/不同用户允许）
  - 权限校验测试（非所有者禁止编辑/删除）
  - 资源不存在异常测试
- [X] T023 [P] 补充单元测试：backend/src/test/java/com/iaihub/toolbox/service/ToolFileServiceTest.java (2026-06-05)
  - 同名文件替换测试（软删除旧记录 + 保存新记录）
  - 无同名文件时仅新增测试
  - 混合文件上传测试（部分替换部分新增）
- [X] T024 更新 API 文档：specs/004-tool-version-management/contracts/tool-api.md (2026-06-05)
  - 所有端点说明包含 version 字段（已验证）
  - API 质量检查清单 40 项全部通过
- [X] T025 更新 README.md（2026-06-05）
  - tool 表结构添加 version 字段
  - tool_file 表结构更新字段名
  - 文件上传 API 端点路径更新

---

## 依赖关系与执行顺序

### 阶段依赖

- **阶段 1（基础）**：无依赖，可立即开始
- **阶段 2（US1）**：依赖阶段 1 完成
- **阶段 3（US2）**：依赖阶段 1 完成
- **阶段 4（US3）**：依赖阶段 2、3 完成
- **阶段 5（收尾）**：依赖所有用户故事完成

### 用户故事依赖

- **US1 (P1)**：阶段 1 后开始 - 无其他故事依赖
- **US2 (P1)**：阶段 1 后开始 - 可与 US1 并行
- **US3 (P2)**：阶段 1 后开始 - 可与 US1/US2 并行

### 并行机会

- T002、T003、T004、T005 可并行（不同文件）
- T008、T009 可并行
- T010、T013 可并行
- T014、T015 可并行
- T017、T018 可并行

---

## 并行执行示例

```bash
# 阶段 1：数据库迁移（单独执行）
bash migrate.sh

# 阶段 2 和 3 可并行
Agent A: T002-T009 (US1 后端+前端)
Agent B: T010, T013-T015 (US2 后端+前端)
Agent C: T017-T021 (US3 后端)

# 阶段 5 可最后执行
Agent A/B/C: T022-T025 (收尾)
```

---

## 实现策略

### MVP 优先（仅用户故事 1）

1. 完成阶段 1：数据库迁移
2. 完成阶段 2：US1 实现
3. **停止并验证**：独立测试用户故事 1
4. 如就绪则部署/演示（MVP）

### 增量交付

1. 阶段 1 → 阶段 2 → 测试 US1 → 部署（MVP）
2. 添加阶段 3 → 测试 US2 → 部署
3. 添加阶段 4 → 测试 US3 → 部署
4. 添加阶段 5 → 完善

---

## 代码质量门禁

在提交前必须通过：

- [ ] 所有测试通过 `./gradlew test && npm run test`
- [ ] 代码覆盖率 ≥ 80%（新代码）
- [ ] 无 Checkstyle/ESLint 错误
- [ ] TypeScript 编译无错误
- [ ] 所有 DTO 经过 XSS 防护处理（XssSanitizer.sanitize()）

---

## 任务统计

| 类别 | 数量 |
|------|------|
| 总任务数 | 25 |
| 已完成（MCP） | 2 |
| 待执行 | 23 |
| US1 相关 | 8 |
| US2 相关 | 7 |
| US3 相关 | 5 |
| 收尾 | 4 |

---

## 备注

- [P] 任务 = 不同文件，无依赖，可并行
- [Story] 标签将任务映射到特定用户故事
- 每个用户故事应可独立完成和测试
- 后端遵循分层架构：controller → service → repository → model
- 前端分层：components → services → stores → types
