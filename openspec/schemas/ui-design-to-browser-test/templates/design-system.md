# Design System

> 本文档是本次变更的 UI 约束摘要。优先引用全局设计系统 `design-system/CodingHubs/MASTER.md`，仅补充本次变更相关的组件、样式和交互规范。

## 1. Applicability

- 是否涉及前端 UI：是 / 否
- 如不涉及 UI：说明跳过原因
- 参考来源：
  - `design-system/CodingHub/MASTER.md`
  - ui-ux-pro-max 检索结果（如有）

## 2. Related Components

| 组件/页面 | 用途 | 状态覆盖 |
|----------|------|----------|
| `<component/page>` | `<purpose>` | normal / hover / focus / disabled / loading / empty / error |

## 3. Visual Tokens

### Colors

| Token | Value | Usage |
|-------|-------|-------|
| `<token>` | `<value>` | `<usage>` |

### Typography

| 层级 | 字体/字号/行高 | 用途 |
|------|----------------|------|
| `<level>` | `<font-size/line-height>` | `<usage>` |

### Spacing & Radius

| Token | Value | Usage |
|-------|-------|-------|
| `<token>` | `<value>` | `<usage>` |

## 4. Interaction States

| 元素 | 状态 | 视觉/行为要求 |
|------|------|----------------|
| `<element>` | hover / focus / disabled / loading / error | `<requirement>` |

## 5. Responsive Strategy

| 断点 | 布局策略 |
|------|----------|
| Desktop ≥1024px | `<layout>` |
| Tablet 768px-1023px | `<layout>` |
| Mobile <768px | `<layout>` |

## 6. Accessibility Requirements

- [ ] 关键交互元素支持键盘访问
- [ ] focus-visible 状态清晰可见
- [ ] 功能性图标提供 `aria-label` 或可读文本
- [ ] 表单错误提示使用 `aria-live` 或等效机制
- [ ] 文本与背景对比度满足 WCAG AA

## 7. Open Questions

- `<需要用户或设计确认的问题>`
