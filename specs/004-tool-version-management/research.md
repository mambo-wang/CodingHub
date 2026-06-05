# 研究发现：工具版本管理

**功能**：004-tool-version-management
**日期**：2026-06-04

## 1. 语义化版本号（SemVer）验证

### 决策
使用正则表达式验证版本号格式

### 理由
- SemVer 是行业标准格式（MAJOR.MINOR.PATCH）
- 支持预发布版本后缀（如 1.0.0-beta）
- 简单可靠，无需引入额外依赖

### 实现
```java
@Pattern(regexp = "^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9]+)?$",
         message = "版本号格式不正确，请使用标准格式（如 1.0.0）")
private String version;
```

### 替代方案
| 方案 | 优点 | 缺点 |
|------|------|------|
| 使用 semver 库 | 完整验证 | 增加依赖 |
| 正则表达式 | 简单高效 | 不验证数值范围 |
| **正则表达式（选中）** | **无需依赖** | **基础验证** |

---

## 2. 版本号唯一性验证

### 决策
同用户 + 同分类 + 同名称 唯一

### 理由
- 允许不同用户创建同名工具（如"图像识别工具"）
- 防止同一用户在同一分类下有多个同名工具造成混淆
- 符合规格澄清结果

### 实现
```java
// Repository 查询
Optional<Tool> findByUploaderIdAndNameAndCategoryIdAndStatus(
    Long uploaderId, String name, Long categoryId, Tool.Status status);
```

### SQL 约束变更
```sql
-- 变更前
ALTER TABLE tool DROP INDEX uk_tool_uploader_name;
ALTER TABLE tool ADD CONSTRAINT uk_tool_uploader_name_category 
    UNIQUE (uploader_id, name, category_id, status);
```

---

## 3. 文件同名检测策略

### 决策
后端检测同名文件并自动替换

### 理由
- 前端上传时无法预知服务器端文件名
- 同名替换是常见文件管理行为
- 简化前端逻辑

### 实现流程
```
1. 前端上传文件
2. 后端查询工具现有文件列表
3. 比对文件名（original_name）
4. 如存在同名：
   - 删除旧文件记录（status = DELETED）
   - 删除物理文件
5. 保存新文件
```

### 替代方案
| 方案 | 优点 | 缺点 |
|------|------|------|
| 前端检测同名 | 减少后端请求 | 无法检测其他用户上传 |
| **后端检测（选中）** | **统一处理** | **需额外查询** |
| 不支持同名替换 | 简单 | 用户体验差 |

---

## 4. 版本号更新策略

### 决策
修改工具时可直接更新版本号

### 理由
- 规格明确：一个工具只有一个版本，更新需修改工具
- 不保留版本历史（系统只记录最新版本）
- 简化实现复杂度

### 实现
```java
@Transactional
public ToolDetailDTO updateTool(Long id, UpdateToolRequest request, Long userId) {
    Tool tool = toolRepository.findByIdAndStatusNormal(id)
        .orElseThrow(() -> new ResourceNotFoundException("工具不存在"));
    
    // 版本号更新
    if (request.getVersion() != null) {
        tool.setVersion(request.getVersion());
    }
    
    return toolRepository.save(tool);
}
```

---

## 5. 文件删除权限验证

### 决策
仅工具创建者或管理员可删除文件

### 理由
- 规格安全需求：SR-002
- 遵循最小权限原则
- 复用现有权限校验逻辑

### 实现
```java
@Transactional
public void deleteToolFile(Long toolId, Long fileId, Long userId) {
    Tool tool = toolRepository.findByIdAndStatusNormal(toolId)
        .orElseThrow(() -> new ResourceNotFoundException("工具不存在"));
    
    // 权限校验
    if (!tool.getUploader().getId().equals(userId) && !isAdmin(userId)) {
        throw new AccessDeniedException("无权限删除此文件");
    }
    
    // 删除文件...
}
```

---

## 6. 前端版本号输入组件

### 决策
使用文本输入 + 实时格式校验

### 理由
- 简单直接
- 即时反馈格式错误
- 符合现有 UI 风格

### 实现
```vue
<template>
  <div class="version-input">
    <input 
      v-model="version" 
      placeholder="如 1.0.0"
      @blur="validateVersion"
    />
    <span v-if="!isValid" class="error">
      版本号格式不正确
    </span>
  </div>
</template>

<script setup>
const versionPattern = /^\d+\.\d+\.\d+(-[a-zA-Z0-9]+)?$/

const validateVersion = () => {
  isValid.value = versionPattern.test(version.value)
}
</script>
```
