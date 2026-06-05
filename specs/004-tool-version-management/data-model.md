# 数据模型：工具版本管理

**功能**：004-tool-version-management
**日期**：2026-06-04

## 1. 实体变更

### Tool 实体

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | Long | PK, AUTO_INCREMENT | 主键 |
| name | String(100) | NOT NULL | 工具名称 |
| category_id | Long | FK, NOT NULL | 分类 ID |
| content | TEXT | NOT NULL | 工具描述 |
| uploader_id | Long | FK, NOT NULL | 上传者 ID |
| status | Enum | NOT NULL, DEFAULT NORMAL | 状态 |
| version | String(50) | NOT NULL | **新增**：版本号 |
| created_at | DateTime | NOT NULL | 创建时间 |
| updated_at | DateTime | NOT NULL | 更新时间 |

### 唯一性约束变更

| 约束名 | 字段 | 说明 |
|--------|------|------|
| ~~uk_tool_uploader_name~~ | uploader_id, name, status | **已废弃** |
| uk_tool_uploader_name_category | uploader_id, name, category_id, status | **新增**：包含分类 |

---

## 2. DTO 变更

### CreateToolRequest

| 字段 | 类型 | 校验 | 说明 |
|------|------|------|------|
| name | String | @NotBlank, @Size(1,100), @Pattern | 工具名称 |
| categoryId | Long | @NotNull | 分类 ID |
| content | String | @NotBlank, @Size(max=5000) | 工具描述 |
| version | String | @NotBlank, @Pattern(regexp) | **新增**：版本号 |

### UpdateToolRequest

| 字段 | 类型 | 校验 | 说明 |
|------|------|------|------|
| name | String | @Size(1,100), @Pattern | 工具名称 |
| categoryId | Long | - | 分类 ID |
| content | String | @Size(max=5000) | 工具描述 |
| version | String | @Pattern(regexp) | **新增**：版本号 |

### ToolDetailDTO

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 工具 ID |
| name | String | 工具名称 |
| categoryName | String | 分类名称 |
| categoryIcon | String | 分类图标 |
| content | String | 工具描述 |
| version | String | **新增**：版本号 |
| uploaderId | Long | 上传者 ID |
| uploaderUsername | String | 上传者用户名 |
| viewCount | Integer | 浏览次数 |
| likeCount | Integer | 点赞次数 |
| commentCount | Integer | 评论次数 |
| score | BigDecimal | 综合评分 |
| createdAt | DateTime | 创建时间 |
| updatedAt | DateTime | 更新时间 |

### ToolSummaryDTO

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 工具 ID |
| name | String | 工具名称 |
| categoryName | String | 分类名称 |
| categoryIcon | String | 分类图标 |
| content | String | 工具描述（截断） |
| version | String | **新增**：版本号 |
| uploaderUsername | String | 上传者用户名 |
| viewCount | Integer | 浏览次数 |
| likeCount | Integer | 点赞次数 |
| score | BigDecimal | 综合评分 |
| createdAt | DateTime | 创建时间 |

---

## 3. Repository 方法

### ToolRepository

```java
// 新增：按用户+名称+分类查询（版本号唯一性验证）
Optional<Tool> findByUploaderIdAndNameAndCategoryIdAndStatus(
    Long uploaderId, String name, Long categoryId, Tool.Status status);

// 新增：根据用户、分类、名称排除指定 ID 查询
Optional<Tool> findByUploaderIdAndNameAndCategoryIdAndStatusAndIdNot(
    Long uploaderId, String name, Long categoryId, Tool.Status status, Long excludeId);
```

---

## 4. 异常类型

### DuplicateVersionException

| 字段 | 说明 |
|------|------|
| code | 409 (Conflict) |
| message | "该分类下已存在同名工具" |
| data.existingToolId | 已存在工具 ID |
| data.existingVersion | 已存在工具版本号 |

---

## 5. 数据库迁移脚本

```sql
-- 1. 添加 version 字段
ALTER TABLE tool ADD COLUMN version VARCHAR(50) NOT NULL DEFAULT '1.0.0';

-- 2. 删除旧的唯一约束
ALTER TABLE tool DROP INDEX uk_tool_uploader_name;

-- 3. 添加新的唯一约束（包含 category_id）
ALTER TABLE tool ADD CONSTRAINT uk_tool_uploader_name_category 
    UNIQUE (uploader_id, name, category_id, status);

-- 4. 添加索引（优化查询）
CREATE INDEX idx_tool_version ON tool(version);
```

---

## 6. Entity 关系图

```
┌─────────────────┐       ┌─────────────────┐
│      User       │       │    Category      │
├─────────────────┤       ├─────────────────┤
│ id (PK)         │       │ id (PK)         │
│ username        │       │ name            │
│ email           │       │ icon            │
└────────┬────────┘       └────────┬────────┘
         │                         │
         │ 1       ┌───────────────┘
         │         │ N
         ▼         ▼
┌─────────────────────────────────────────┐
│                  Tool                   │
├─────────────────────────────────────────┤
│ id (PK)                                 │
│ name                                    │
│ version  ◄── 新增                        │
│ content                                 │
│ category_id (FK) ───────────► Category   │
│ uploader_id (FK) ───────────► User      │
│ status (NORMAL/DELETED)                 │
│ created_at                              │
│ updated_at                              │
└────────┬────────────────────────────────┘
         │
         │ 1
         │ N
         ▼
┌─────────────────────────────────────────┐
│               ToolFile                  │
├─────────────────────────────────────────┤
│ id (PK)                                 │
│ tool_id (FK)                            │
│ original_name                           │
│ stored_path                             │
│ file_size                               │
│ content_type                            │
│ status (NORMAL/DELETED)                 │
│ created_at                              │
└─────────────────────────────────────────┘
```
