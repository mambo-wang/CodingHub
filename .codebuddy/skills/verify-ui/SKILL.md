---
name: verify-ui
description: 独立的 UI 验证 skill，使用 ui-ux-pro-max 检查 UI 实现是否完整、是否符合设计规范、是否适配不同皮肤。**自动修复模式**：发现问题时自动修复。
---

# verify-ui

独立的 UI 验证工具，使用 ui-ux-pro-max 设计系统检查 UI 实现质量，**发现问题时自动修复**。

## 自动修复模式

验证过程中发现 UI 问题会自动修复：
- CSS 变量未定义 → 自动添加到组件
- 颜色值 inline 而非 CSS 变量 → 自动转换为 CSS 变量
- 缺少必要组件 → 自动创建
- 组件结构不规范 → 自动规范化

## 功能

1. **设计系统验证** - 加载 design-system.md 或生成项目设计系统
2. **UI 完整性检查** - 验证所有设计的组件是否已实现
3. **设计规范检查** - 对比实现是否符合设计规范（颜色、间距、字体等）
4. **项目规范检查** - 检查 Vue 组件结构、props、emits、scoped styles
5. **皮肤/主题适配** - 检查暗色模式支持、CSS 变量定义
6. **视觉预览对比** - 打开 ui-preview.html 与实现对比
7. **自动修复** - 发现问题自动修复组件

## 前置条件

1. 加载 ui-ux-pro-max skill：
   ```
   /ui-ux-pro-max
   ```

2. 确保待验证的组件/页面已实现

## 使用方法

### 基本用法

```
/verify-ui
```

### 验证并自动修复

```
/verify-ui --fix
```

### 指定文件/目录

```
/verify-ui frontend/src/pages/OverviewPage.vue
/verify-ui frontend/src/components/
```

## 自动修复规则

### 1. CSS 变量检查

如果组件使用 inline 颜色值而非 CSS 变量，自动替换为变量：

| 发现问题 | 自动修复 |
|----------|----------|
| `color: #00FFFF` | `color: var(--color-accent-cyan)` |
| `background: #020617` | `background: var(--color-background)` |
| `border-radius: 16px` | `border-radius: var(--radius-lg)` |

### 2. 圆角值检查

对比 design-spec 中定义的圆角值，自动修复：

| 规范值 | 实际值 | 修复操作 |
|--------|--------|----------|
| radius-lg: 12px | 16px | 替换为 `var(--radius-lg)` |
| radius-sm: 4px | 8px | 替换为 `var(--radius-sm)` |

### 3. 字体检查

| 发现问题 | 自动修复 |
|----------|----------|
| 缺少 Fira Code 导入 | 添加 Google Fonts 导入 |
| 字体非 Fira Code | 替换为 Fira Code |

### 4. 组件结构检查

| 问题 | 修复 |
|------|------|
| 缺少 `<script setup lang="ts">` | 添加 |
| Props 无类型 | 添加 TypeScript 类型 |
| 缺少 scoped | 添加 `scoped` |
| 目录不正确 | 移动到正确目录 |

## 验证步骤

### Step 1: 确定验证范围

- 读取 design-system.md 获取设计规范
- 确定要验证的组件列表

### Step 2: 加载设计系统

```bash
cat design-system.md  # 读取项目设计规范
```

### Step 3: 逐个检查组件

对每个组件执行：

1. **读取组件源码**
2. **对比设计规范**
3. **发现问题自动修复**
4. **写回修复后的组件**

### Step 4: 验证修复结果

```bash
git diff --stat
```

## 输出报告

```
## UI Verification & Auto-Fix Report

### Components Verified: 5
### Issues Found: 3
### Issues Fixed: 3

| Component | Issues | Status |
|------------|---------|--------|
| StatsCard.vue | 2 CSS variables | ✅ Fixed |
| RankItem.vue | 1 radius value | ✅ Fixed |
| ToolRankList.vue | 0 | ✅ OK |

### Auto-Fixed Issues

1. **StatsCard.vue:32** - CSS variable added
   - Before: `color: #00FFFF`
   - After: `color: var(--color-accent-cyan)`

2. **StatsCard.vue:64** - CSS variable added
   - Before: `border-radius: 16px`
   - After: `border-radius: var(--radius-lg)`

3. **RankItem.vue:38** - CSS variable added
   - Before: `border-radius: 8px`
   - After: `border-radius: var(--radius-sm)`

### Summary

| Dimension | Status |
|-----------|--------|
| Components | 5/5 verified |
| Auto-Fixed | 3/3 fixed |
| Design Adherence | ✅ All issues resolved |

**All issues automatically fixed. Components are now design-compliant.**
```

## 命令行工具

```bash
# 验证并自动修复所有组件
verify-ui --fix

# 只验证不修复
verify-ui --check

# 验证指定组件
verify-ui src/components/StatsCard.vue
```

## 集成

此 skill 可以独立使用，也可以集成到 openspec-verify-change 工作流中作为 Step 8。

---
**Created**: 2026-05-31
**Version**: 1.1
**Auto-Fix**: Enabled