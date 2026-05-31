---
name: OPSX: Verify-UI
description: "Independent UI verification using ui-ux-pro-max to check if UI implementation is complete, matches design, and adapts to themes. Auto-fixes issues when --fix is specified."
argument-hint: "[component-path or --fix or --all]"
---

# /opsx:verify-ui

独立的 UI 验证命令，使用 ui-ux-pro-max 设计系统检查 UI 实现质量，**发现问题时自动修复**。

**Input**: Optionally specify a component path or `--fix` flag.

## Quick Start

```
/verify-ui --fix          # 验证并自动修复所有组件
/verify-ui --all         # 验证所有 UI 组件
/verify-ui --check       # 只验证不修复
/verify-ui <path>        # 验证指定组件
```

## Auto-Fix 规则

### CSS 变量检查
- 发现 inline 颜色值 → 自动替换为 CSS 变量
- 对比 design-spec 中的 radius-lg/sm/md 值 → 替换为 `var(--radius-*)` 格式

### 组件结构
- 缺少 TypeScript 类型 → 自动添加
- 缺少 scoped styles → 自动添加
- 缺少 Props 定义 → 自动规范化

## 使用示例

```bash
# 验证并自动修复
/verify-ui --fix

# 验证概览页面所有组件
/verify-ui --fix frontend/src/pages/OverviewPage.vue
/verify-ui --fix frontend/src/components/StatsCard.vue
/verify-ui --fix frontend/src/components/RankItem.vue
```

## 验证流程

1. 读取 design-system.md 获取设计规范
2. 读取组件源码
3. 对比设计规范与实现
4. 发现问题自动修复并写回
5. 生成验证报告

## Prerequisites

1. Load ui-ux-pro-max skill:
   ```
   /ui-ux-pro-max
   ```

2. Ensure components exist:
   ```
   ls frontend/src/components/
   ```

## Integration

```
/verify-ui --fix    # 验证并自动修复
    ↓
/verify-ui --check  # 验证下一个组件
    ↓
/opsx:archive       # 归档变更
```

---
**Version**: 1.1 - Auto-Fix Enabled