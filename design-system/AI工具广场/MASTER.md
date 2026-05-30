# Design System Master File

> **LOGIC:** When building a specific page, first check `design-system/pages/[page-name].md`.
> If that file exists, its rules **override** this Master file.
> If not, strictly follow the rules below.

---

**Project:** AI工具广场
**Generated:** 2026-05-30 23:27:02
**Category:** AI Tools SaaS Platform
**Style:** Cyberpunk Glassmorphism (Dark Mode)

---

## Global Rules

### Color Palette

| Role | Hex | CSS Variable | Usage |
|------|-----|--------------|-------|
| Background | `#0D0D0D` | `--color-background` | 页面背景，深黑底 |
| Card BG | `rgba(255,255,255,0.08)` | `--color-card-bg` | Glassmorphism 毛玻璃卡片 |
| Border | `rgba(255,255,255,0.15)` | `--color-border` | 卡片边框 |
| Primary | `#00FFFF` | `--color-primary` | 主色调 Cyan，科技感 |
| Secondary | `#FF00FF` | `--color-secondary` | 辅助色 Magenta |
| Success/Like | `#00FF00` | `--color-success` | 点赞数、Matrix Green |
| Text Primary | `#FFFFFF` | `--color-text` | 标题、主文字 |
| Text Secondary | `rgba(255,255,255,0.7)` | `--color-text-secondary` | 正文 |
| Text Muted | `rgba(255,255,255,0.5)` | `--color-text-muted` | 辅助说明 |

**Color Notes:** Cyberpunk Glassmorphism 风格 — 深黑背景 + 霓虹强调色（青/品红/矩阵绿）+ 毛玻璃效果

### Typography

- **Heading Font:** Fira Code (monospace)
- **Body Font:** Fira Sans
- **Mood:** dashboard, data, analytics, cyberpunk, tech, futuristic
- **Google Fonts:** [Fira Code + Fira Sans](https://fonts.googleapis.com/share?selection.family=Fira+Code:wght@400;500;600;700|Fira+Sans:wght@300;400;500;600;700)

**CSS Import:**
```css
@import url('https://fonts.googleapis.com/css2?family=Fira+Code:wght@400;500;600;700&family=Fira+Sans:wght@300;400;500;600;700&display=swap');
```

**Usage:**
- 标题、标签、排名数字 → `font-family: 'Fira Code', monospace`
- 正文、说明文字 → `font-family: 'Fira Sans', sans-serif`

### Spacing Variables

| Token | Value | Usage |
|-------|-------|-------|
| `--space-xs` | `4px` / `0.25rem` | Tight gaps |
| `--space-sm` | `8px` / `0.5rem` | Icon gaps, inline spacing |
| `--space-md` | `16px` / `1rem` | Standard padding |
| `--space-lg` | `24px` / `1.5rem` | Section padding |
| `--space-xl` | `32px` / `2rem` | Large gaps |
| `--space-2xl` | `48px` / `3rem` | Section margins |
| `--space-3xl` | `64px` / `4rem` | Hero padding |

### Border Radius

| Token | Value | Usage |
|-------|-------|-------|
| `--radius-sm` | `8px` | 小按钮、输入框 |
| `--radius-md` | `12px` | 卡片、列表项 |
| `--radius-lg` | `16px` | 大卡片 |
| `--radius-xl` | `24px` | 模态框、特大卡片 |

### Shadow & Glow Effects

| Level | Value | Usage |
|-------|-------|-------|
| `--shadow-sm` | `0 1px 2px rgba(0,0,0,0.3)` | Subtle lift |
| `--shadow-md` | `0 4px 6px rgba(0,0,0,0.4)` | Cards, buttons |
| `--shadow-lg` | `0 10px 15px rgba(0,0,0,0.5)` | Modals, dropdowns |
| `--glow-cyan` | `0 0 20px rgba(0,255,255,0.3)` | 悬停发光效果 |
| `--glow-magenta` | `0 0 20px rgba(255,0,255,0.3)` | 悬停发光效果 |

---

## Component Specs

### Glass Card (毛玻璃卡片)

```css
/* 基础毛玻璃卡片 */
.glass-card {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 16px;
  transition: all 200ms ease;
}

.glass-card:hover {
  box-shadow: var(--glow-cyan);
  border-color: rgba(0, 255, 255, 0.3);
}

/* 次级毛玻璃卡片 */
.glass-card-sub {
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
}
```

### Buttons

```css
/* Primary Button */
.btn-primary {
  background: linear-gradient(135deg, #00FFFF, #00CCCC);
  color: #0D0D0D;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  transition: all 200ms ease;
  cursor: pointer;
  border: none;
}

.btn-primary:hover {
  box-shadow: var(--glow-cyan);
  transform: translateY(-1px);
}

/* Secondary Button */
.btn-secondary {
  background: transparent;
  color: #00FFFF;
  border: 2px solid #00FFFF;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  transition: all 200ms ease;
  cursor: pointer;
}

.btn-secondary:hover {
  background: rgba(0, 255, 255, 0.1);
  box-shadow: var(--glow-cyan);
}
```

### Stat Card (统计卡片)

```css
.stat-card {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 16px;
  padding: 20px;
  min-height: 140px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  cursor: pointer;
  transition: all 150ms ease;
}

.stat-card:hover {
  box-shadow: var(--glow-cyan);
  transform: translateY(-2px);
}

.stat-card .stat-value {
  font-family: 'Fira Code', monospace;
  font-size: 2.5rem;
  font-weight: 700;
  color: #FFFFFF;
}

.stat-card .stat-label {
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.875rem;
}
```

### Hot List Item (热榜条目)

```css
.hot-list-item {
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 150ms ease;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hot-list-item:hover {
  background: rgba(255, 255, 255, 0.12);
}

.hot-list-item.rank-1 {
  border-left: 3px solid #00FFFF;
}

/* 排名数字 */
.rank-badge {
  font-family: 'Fira Code', monospace;
  font-weight: 700;
  font-size: 0.875rem;
}

.rank-badge.top { color: #00FFFF; }
.rank-badge.normal { color: rgba(255, 255, 255, 0.4); }

/* 点赞数 */
.like-count {
  color: #FF00FF;
  display: flex;
  align-items: center;
  gap: 4px;
}

.like-count svg {
  width: 16px;
  height: 16px;
  fill: currentColor;
}
```

### Category Tabs (分类标签)

```css
.tab {
  padding: 6px 16px;
  border-radius: 8px 8px 0 0;
  font-size: 0.875rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.5);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 200ms ease;
}

.tab:hover {
  color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.05);
}

.tab.active {
  background: rgba(0, 255, 255, 0.15);
  color: #00FFFF;
  border-bottom: 2px solid #00FFFF;
}
```

### Input Fields

```css
.input {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 16px;
  color: #FFFFFF;
  transition: all 200ms ease;
}

.input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.input:focus {
  border-color: #00FFFF;
  outline: none;
  box-shadow: 0 0 0 3px rgba(0, 255, 255, 0.2);
}
```

### Loading Skeleton (骨架屏)

```css
.skeleton {
  background: #1A1A1A;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { opacity: 0.3; }
  50% { opacity: 0.7; }
  100% { opacity: 0.3; }
}
```

---

## Style Guidelines

**Style:** Cyberpunk Glassmorphism (暗色主题)

**Keywords:** Neon, dark mode, glassmorphism, HUD, sci-fi, futuristic, cyber, matrix, terminal

**Best For:** AI tools platform, tech products, developer tools, gaming platforms, crypto apps

**Key Effects:**
- Neon glow (text-shadow, box-shadow)
- Glassmorphism (backdrop-filter: blur)
- Scanlines overlay (optional)
- Terminal fonts (Fira Code)
- Subtle glitch animations (optional)

### Page Pattern

**Pattern Name:** Data-Dense Dashboard

- **Layout:** KPI cards + hot lists + charts
- **Information Density:** High — maximum data visibility
- **Section Order:** 1. Header, 2. Stats cards row, 3. Hot lists grid, 4. Footer
- **Responsive:** 1→2→3→4 columns based on breakpoints

---

## Anti-Patterns (Do NOT Use)

- ❌ Light mode default (本项目仅支持暗色主题)
- ❌ 使用 emoji 作为图标
- ❌ 缺少 `cursor-pointer` 样式
- ❌ 悬停状态使用 scale 变换导致布局位移
- ❌ 文字对比度低于 4.5:1
- ❌ 状态变化无过渡动画
- ❌ 键盘导航时 focus 状态不可见

### Additional Forbidden Patterns

- ❌ **Emojis as icons** — Use SVG icons (Heroicons, Lucide, Simple Icons)
- ❌ **Missing cursor:pointer** — All clickable elements must have cursor:pointer
- ❌ **Layout-shifting hovers** — Avoid scale transforms that shift layout
- ❌ **Low contrast text** — Maintain 4.5:1 minimum contrast ratio
- ❌ **Instant state changes** — Always use transitions (150-300ms)
- ❌ **Invisible focus states** — Focus states must be visible for a11y

---

## Icon Specification (图标规范)

**Icon Library:** `@lucide/vue-next` (推荐) 或 `heroicons`

**Icon Size Standard:** 24x24 viewBox，配套 `w-6 h-6` Tailwind 类

**Common Icons Mapping:**

| 用途 | Lucide Icon Name | 说明 |
|------|------------------|------|
| 用户统计 | `Users` | 用户数卡片 |
| 工具统计 | `Wrench` | 工具数卡片 |
| 帖子统计 | `FileText` | 帖子数卡片 |
| 点赞/喜欢 | `Heart` | 点赞数 (fill for filled) |
| 热榜 | `Flame` | 热榜标题图标 |
| 排名 | `Trophy` | 排名标题图标 |
| 分类 | `Folder` | 分类标签 |
| 搜索 | `Search` | 搜索框 |
| 导航菜单 | `Menu` | 移动端菜单 |
| 关闭 | `X` | 关闭按钮 |
| 箭头上升 | `ArrowUp` | 增长趋势 |
| 箭头下降 | `ArrowDown` | 下降趋势 |

**Icon Color Variables:**

```css
--icon-primary: #00FFFF;   /* Cyan */
--icon-secondary: #FF00FF; /* Magenta */
--icon-like: #FF00FF;      /* 点赞粉色 */
--icon-muted: rgba(255,255,255,0.5); /* 次要图标 */
```

---

## Pre-Delivery Checklist

Before delivering any UI code, verify:

- [ ] No emojis used as icons (use SVG instead)
- [ ] All icons from consistent icon set (Heroicons/Lucide)
- [ ] `cursor-pointer` on all clickable elements
- [ ] Hover states with smooth transitions (150-300ms)
- [ ] Light mode: text contrast 4.5:1 minimum
- [ ] Focus states visible for keyboard navigation
- [ ] `prefers-reduced-motion` respected
- [ ] Responsive: 375px, 768px, 1024px, 1440px
- [ ] No content hidden behind fixed navbars
- [ ] No horizontal scroll on mobile
