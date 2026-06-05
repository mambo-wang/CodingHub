# 快速开始：工具版本管理

**功能**：004-tool-version-management
**日期**：2026-06-04

## 开发前置准备

### 1. 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.x
- Maven/Gradle

### 2. 数据库设置

```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE IF NOT EXISTS iaihub_db;
USE iaihub_db;

# 执行迁移脚本（参考 data-model.md）
ALTER TABLE tool ADD COLUMN version VARCHAR(50) NOT NULL DEFAULT '1.0.0';
ALTER TABLE tool DROP INDEX uk_tool_uploader_name;
ALTER TABLE tool ADD CONSTRAINT uk_tool_uploader_name_category 
    UNIQUE (uploader_id, name, category_id, status);
CREATE INDEX idx_tool_version ON tool(version);
```

### 3. 启动服务

```bash
# 后端
cd backend
./gradlew bootRun

# 前端（新窗口）
cd frontend
npm install
npm run dev
```

---

## 实现步骤

### Step 1: 添加 Tool 实体 version 字段

**文件**：`backend/src/main/java/com/iaihub/toolbox/model/Tool.java`

```java
@Column(name = "version", nullable = false, length = 50)
@Builder.Default
private String version = "1.0.0";
```

### Step 2: 更新 CreateToolRequest

**文件**：`backend/src/main/java/com/iaihub/toolbox/dto/CreateToolRequest.java`

```java
@NotBlank(message = "版本号不能为空")
@Pattern(regexp = "^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9]+)?$",
         message = "版本号格式不正确，请使用标准格式（如 1.0.0）")
private String version;
```

### Step 3: 更新 ToolService 校验逻辑

**文件**：`backend/src/main/java/com/iaihub/toolbox/service/ToolService.java`

```java
// 1. 添加入参中设置版本号
Tool tool = Tool.builder()
    .name(request.getName())
    .category(category)
    .content(request.getContent())
    .uploader(user)
    .version(request.getVersion())  // 新增
    .build();

// 2. 添唯一性校验
private void validateVersionUniqueness(CreateToolRequest request, Long userId) {
    Optional<Tool> existing = toolRepository
        .findByUploaderIdAndNameAndCategoryIdAndStatus(
            userId, request.getName(), request.getCategoryId(), Tool.Status.NORMAL);
    if (existing.isPresent()) {
        throw new DuplicateVersionException("该分类下已存在同名工具", 
            existing.get().getId(), existing.get().getVersion());
    }
}
```

### Step 4: 增强 ToolFileService 同名替换

**文件**：`backend/src/main/java/com/iaihub/toolbox/service/ToolFileService.java`

```java
@Transactional
public FileUploadResponse uploadFiles(Long toolId, List<MultipartFile> files, String readme) {
    // ... 现有代码 ...

    for (MultipartFile file : files) {
        validateFile(file);
        String originalName = StringUtils.cleanPath(file.getOriginalFilename());

        // 新增：检测同名文件并删除
        Optional<ToolFile> existingFile = toolFileRepository
            .findByToolIdAndOriginalNameAndStatusNormal(toolId, originalName);
        if (existingFile.isPresent()) {
            deleteExistingFile(existingFile.get());
        }

        // 保存新文件
        // ...
    }
}

private void deleteExistingFile(ToolFile file) {
    // 删除物理文件
    Path filePath = Paths.get(uploadConfig.getBaseDir(), file.getStoredPath());
    Files.deleteIfExists(filePath);
    
    // 标记删除（软删除）
    file.setStatus(ToolFile.Status.DELETED);
    toolFileRepository.save(file);
}
```

### Step 5: 前端版本号输入组件

**文件**：`frontend/src/pages/CreateTool.vue`

```vue
<template>
  <form @submit.prevent="submitTool">
    <!-- 其他字段... -->
    
    <div class="form-group">
      <label>版本号</label>
      <input 
        v-model="form.version"
        :class="{ 'is-invalid': errors.version }"
        placeholder="如 1.0.0"
        @blur="validateVersion"
      />
      <span v-if="errors.version" class="error">
        {{ errors.version }}
      </span>
    </div>
    
    <button type="submit" :disabled="loading">
      创建工具
    </button>
  </form>
</template>

<script setup>
const versionPattern = /^\d+\.\d+\.\d+(-[a-zA-Z0-9]+)?$/

const validateVersion = () => {
  if (!form.value.version) {
    errors.value.version = '版本号不能为空'
  } else if (!versionPattern.test(form.value.version)) {
    errors.value.version = '版本号格式不正确（如 1.0.0）'
  } else {
    errors.value.version = null
  }
}
</script>
```

---

## 测试验证

### API 测试

```bash
# 1. 创建带版本号的工具
curl -X POST http://localhost:8081/api/v1/tools \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "name": "图像识别工具",
    "content": "这是一个图像识别工具",
    "categoryId": 1,
    "version": "1.0.0"
  }'

# 2. 验证版本号显示
curl http://localhost:8081/api/v1/tools/1
# 响应应包含 "version": "1.0.0"

# 3. 测试版本号唯一性
# 同一用户+分类下创建同名工具应返回 409

# 4. 测试文件上传同名替换
curl -X POST http://localhost:8081/api/v1/tools/1/files \
  -F "files=@new_file.pdf"
# 应替换同名文件
```

### UI 测试

1. 打开 http://localhost:5173/tools/create
2. 填写工具信息，包括版本号"1.0.0"
3. 提交后验证详情页显示版本号
4. 进入编辑页，验证版本号可修改
5. 上传同名文件验证替换功能
6. 删除文件验证权限控制

---

## 回滚方案

如果需要回滚：

```sql
-- 删除 version 字段
ALTER TABLE tool DROP COLUMN version;

-- 恢复唯一约束
ALTER TABLE tool ADD CONSTRAINT uk_tool_uploader_name 
    UNIQUE (uploader_id, name, status);
```
