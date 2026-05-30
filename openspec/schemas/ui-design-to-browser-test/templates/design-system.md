# Design System

> 本设计系统由 ui-ux-pro-max skill 生成，定义项目级视觉规范。

## 1. Design Style

| 属性 | 值 |
|------|------|
| **Style Name** | `<style_name>` |
| **Keywords** | `<keywords>` |
| **Best For** | `<best_for>` |

## 2. Color Palette

| Role | Hex | CSS Variable | Usage |
|------|-----|--------------|-------|
| Primary | `<hex>` | `--color-primary` | 主色调 |
| Secondary | `<hex>` | `--color-secondary` | 辅助色 |
| Background | `<hex>` | `--color-background` | 背景色 |
| Surface | `<hex>` | `--color-surface` | 卡片/容器背景 |
| Text | `<hex>` | `--color-text` | 主文字 |
| Text Muted | `<hex>` | `--color-text-muted` | 次要文字 |
| Border | `<hex>` | `--color-border` | 边框色 |
| Accent | `<hex>` | `--color-accent` | 强调/CTA |

## 3. Typography

| 用途 | 字体 | 字重 |
|------|------|------|
| 标题 | `<font_name>`, fallback | 600-700 |
| 正文 | `<font_name>`, fallback | 400-500 |
| 辅助 | `<font_name>`, fallback | 300-400 |

## 4. Spacing System

| Token | Value |
|-------|-------|
| `--space-xs` | `<value>` |
| `--space-sm` | `<value>` |
| `--space-md` | `<value>` |
| `--space-lg` | `<value>` |
| `--space-xl` | `<value>` |

## 5. Effects

| Token | Value | Usage |
|-------|-------|-------|
| `--shadow-sm` | `<value>` | 轻微阴影 |
| `--shadow-md` | `<value>` | 卡片/按钮 |
| `--radius-sm` | `<value>` | 小圆角 |
| `--radius-md` | `<value>` | 中圆角 |
| `--radius-lg` | `<value>` | 大圆角 |

## 6. Component Specs

### Button

```css
.btn {
  /* 主按钮样式 */
  background: var(--color-primary);
  color: var(--color-text);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-sm);
  font-weight: 600;
  transition: all 200ms ease;
  cursor: pointer;
}

.btn:hover {
  /* 悬停状态 */
}

.btn:disabled {
  /* 禁用状态 */
}
```

### Card

```css
.card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  transition: all 200ms ease;
  cursor: pointer;
}

.card:hover {
  /* 悬停状态 */
}
```

### Input

```css
.input {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: var(--space-sm) var(--space-md);
  color: var(--color-text);
  transition: border-color 200ms ease;
}

.input:focus {
  border-color: var(--color-primary);
  outline: none;
}
```

## 7. Icon Specification

| 用途 | Icon Name |
|------|-----------|
| `<usage>` | `<icon_name>` |

**规则**: 使用 SVG 图标库，禁止 emoji 作为图标。

## 8. Anti-Patterns

- ❌ 使用 emoji 作为 UI 图标
- ❌ 缺少 `cursor-pointer` 在可点击元素上
- ❌ 悬停状态使用 scale 变换导致布局位移
- ❌ 文字对比度低于 4.5:1
- ❌ 状态变化无过渡动画
- ❌ focus 状态不可见

## 9. Pre-Delivery Checklist

- [ ] 无 emoji 作为图标
- [ ] 可点击元素有 `cursor-pointer`
- [ ] 悬停状态有平滑过渡 (150-300ms)
- [ ] 文字对比度 ≥ 4.5:1
- [ ] focus 状态可见
- [ ] 响应式布局正常 (375px, 768px, 1024px, 1440px)