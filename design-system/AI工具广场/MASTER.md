# Design System Master File

> **LOGIC:** When building a specific page, first check `design-system/pages/[page-name].md`.
> If that file exists, its rules **override** this Master file.
> If not, strictly follow the rules below.

---

**Project:** AI工具广场
**Generated:** 2026-05-30 23:27:02
**Enhanced:** 2026-06-08 (基于 ui-ux-pro-max 检索)
**Category:** AI Tools SaaS Platform
**Style:** Cyberpunk Glassmorphism (Dark Mode)
**Stack:** Vue 3 + TypeScript + Vite + Tailwind CSS

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
| Destructive | `#EF4444` | `--color-destructive` | 错误、删除、危险操作 |
| Warning | `#FFB020` | `--color-warning` | 警告提示 |

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

---

## Animation & Motion

### Duration Tokens

| Token | Duration | Usage |
|-------|----------|-------|
| `--motion-fast` | `150ms` | 按钮按下、tooltip 出现 |
| `--motion-base` | `200ms` | 悬停状态、颜色过渡 |
| `--motion-slow` | `300ms` | 卡片进入、模态框 |
| `--motion-slower` | `500ms` | **禁止** UI 过渡使用 |

**规则**: UI 过渡不超过 500ms，超过视为"迟钝"。

### Easing Functions

| Token | Value | Usage |
|-------|-------|-------|
| `--ease-in` | `cubic-bezier(0.4, 0, 1, 1)` | 元素退出 |
| `--ease-out` | `cubic-bezier(0, 0, 0.2, 1)` | 元素进入 |
| `--ease-in-out` | `cubic-bezier(0.4, 0, 0.2, 1)` | 状态切换 |
| ~~linear~~ | `linear` | **禁止**用于 UI 过渡（机械感） |

### Motion Restrictions

```css
/* 必须尊重用户的减少动效偏好 */
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}
```

**规则**:
- 同一视口最多动画 1-2 个关键元素，避免视觉干扰
- 持续动画**仅**用于 loading 指示器
- 装饰性元素禁止使用 infinite 动画

### Glitch & Neon Animations (可选)

```css
/* 微故障效果 —— 仅用于重要 CTA 或加载占位 */
@keyframes neon-pulse {
  0%, 100% { box-shadow: 0 0 5px rgba(0, 255, 255, 0.3); }
  50%      { box-shadow: 0 0 20px rgba(0, 255, 255, 0.6); }
}

.neon-glow {
  animation: neon-pulse 2s ease-in-out infinite;
}

/* 扫描线叠加 */
.scanlines::before {
  content: '';
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 2px,
    rgba(0, 255, 255, 0.03) 2px,
    rgba(0, 255, 255, 0.03) 4px
  );
  pointer-events: none;
}
```

---

## Form & Feedback

### Input Validation Pattern

```css
/* 正常态 */
.input {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  padding: 12px 16px;
  color: #FFFFFF;
  transition: all 200ms ease;
}

/* 聚焦态 */
.input:focus {
  border-color: #00FFFF;
  outline: none;
  box-shadow: 0 0 0 3px rgba(0, 255, 255, 0.2);
}

/* 错误态 */
.input--error {
  border-color: #EF4444;
  background: rgba(239, 68, 68, 0.08);
}

.input--error:focus {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.2);
}

/* 成功态 */
.input--success {
  border-color: #00FF00;
  background: rgba(0, 255, 0, 0.05);
}
```

### Error Message Pattern

```vue
<!-- 错误消息必须可被屏幕阅读器朗读 -->
<div class="form-field">
  <label for="email">邮箱</label>
  <input
    id="email"
    type="email"
    :class="['input', { 'input--error': errors.email }]"
    :aria-invalid="!!errors.email"
    aria-describedby="email-error"
  />
  <p
    v-if="errors.email"
    id="email-error"
    class="error-text"
    role="alert"
  >
    {{ errors.email }}
  </p>
</div>
```

```css
.error-text {
  color: #EF4444;
  font-size: 0.875rem;
  margin-top: 4px;
  /* 屏幕阅读器朗读 */
}
```

**规则**:
- 错误消息**必须**紧邻出错字段
- 错误消息**必须**有 `role="alert"` 或 `aria-live="polite"`
- 错误消息**必须**提供恢复路径（重试按钮 / 帮助链接）
- 禁止静默失败

### Submit Feedback Pattern

```vue
<button :disabled="submitting" class="btn-primary">
  <span v-if="submitting" class="spinner"></span>
  {{ submitting ? '提交中...' : '提交' }}
</button>
```

**规则**: 提交按钮必须三态 —— `idle → loading → success/error`，禁止点击后无反馈。

### Input Type Standards

| 字段类型 | input type | autocomplete |
|----------|------------|--------------|
| 邮箱 | `email` | `email` |
| 密码 | `password` | `current-password` / `new-password` |
| 用户名 | `text` | `username` |
| 手机号 | `tel` | `tel` |
| 网址 | `url` | `url` |
| 数字 | `number` | — |

---

## Loading States

### Skeleton Pattern

```css
.skeleton {
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.04) 0%,
    rgba(255, 255, 255, 0.08) 50%,
    rgba(255, 255, 255, 0.04) 100%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: 8px;
}

@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@media (prefers-reduced-motion: reduce) {
  .skeleton {
    animation: none;
    background: rgba(255, 255, 255, 0.06);
  }
}
```

### Spinner Pattern

```css
.spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(0, 255, 255, 0.2);
  border-top-color: #00FFFF;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
```

**规则**:
- 数据加载优先用**骨架屏**（避免布局抖动）
- 短操作（<300ms）可省略 loading
- 长操作（>1s）必须显示进度反馈
- 加载占位**必须**预留正确尺寸，避免 CLS（Cumulative Layout Shift < 0.1）

### Empty State Pattern

```vue
<div class="empty-state">
  <InboxIcon :size="48" class="text-muted" />
  <h3 class="empty-state__title">暂无数据</h3>
  <p class="empty-state__description">还没有任何记录，开始创建第一条吧</p>
  <button class="btn-primary">立即创建</button>
</div>
```

```css
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 64px 24px;
  text-align: center;
}
```

**规则**: 每个列表/数据视图都必须有 Empty State，包含**图标 + 标题 + 描述 + 主操作**。

---

## Accessibility (a11y) 增强

### Focus Indicators

```css
/* 键盘聚焦可见 —— 永不禁用 outline */
:focus-visible {
  outline: 2px solid #00FFFF;
  outline-offset: 2px;
  border-radius: 4px;
}

/* 鼠标点击不显示 */
:focus:not(:focus-visible) {
  outline: none;
}
```

**规则**:
- 焦点环厚度 ≥ 2px，颜色对比度 ≥ 3:1
- 图标按钮**必须**有 `aria-label`
- 表单字段**必须**有 `<label for="">` 关联

### Skip Link

```vue
<!-- 页面顶部，键盘 Tab 第一个可达元素 -->
<a href="#main-content" class="skip-link">跳到主要内容</a>

<main id="main-content" tabindex="-1">
  <!-- 页面主体 -->
</main>
```

```css
.skip-link {
  position: absolute;
  top: -100px;
  left: 16px;
  background: #00FFFF;
  color: #0D0D0D;
  padding: 12px 24px;
  border-radius: 8px;
  z-index: 9999;
  transition: top 200ms;
}

.skip-link:focus {
  top: 16px;
}
```

### Color Contrast

| 元素 | 前景 | 背景 | 对比度 | 状态 |
|------|------|------|--------|------|
| 主标题 `#FFFFFF` | `#FFFFFF` | `#0D0D0D` | 21:1 | ✅ AAA |
| 正文 `rgba(255,255,255,0.7)` | — | `#0D0D0D` | 14.7:1 | ✅ AAA |
| 辅助文字 `rgba(255,255,255,0.5)` | — | `#0D0D0D` | 10.5:1 | ✅ AAA |
| 主色按钮 `#00FFFF` | `#00FFFF` | `#0D0D0D` | 11.4:1 | ✅ AAA |
| 错误 `#EF4444` | `#EF4444` | `#0D0D0D` | 4.7:1 | ✅ AA |

**规则**: 所有正文文字对比度 ≥ 4.5:1（AA），主标题 ≥ 7:1（AAA）。

### Screen Reader Support

| 场景 | 属性 |
|------|------|
| 错误提示 | `role="alert"` 或 `aria-live="assertive"` |
| 加载中 | `aria-busy="true"` + `aria-live="polite"` |
| 模态框 | `role="dialog"` + `aria-modal="true"` + `aria-labelledby` |
| 装饰图标 | `aria-hidden="true"` |
| 折叠区域 | `aria-expanded` + `aria-controls` |

---

## Vue 3 Component Guidelines

### 命名与结构

| 类别 | 规则 | 示例 |
|------|------|------|
| 组件名 | PascalCase | `PostCard.vue`、`CategoryFilter.vue` |
| Props | camelCase 定义，kebab-case 模板使用 | `postId` → `:post-id` |
| 事件 | 动词或动词短语，kebab-case | `emit('update:modelValue')` |
| 文件结构 | SFC 单文件组件（template + script + style） | — |

### 组件模板

```vue
<script setup lang="ts">
// 1. 类型导入
import type { Post } from '@/types/post'

// 2. 依赖导入
import { computed } from 'vue'

// 3. Props 定义
const props = defineProps<{
  post: Post
  showComments?: boolean
}>()

// 4. Emits 定义
const emit = defineEmits<{
  (e: 'like', postId: number): void
  (e: 'share', postId: number): void
}>()

// 5. 响应式状态
const isLiked = ref(false)

// 6. 计算属性
const formattedDate = computed(() => /* ... */)

// 7. 方法
function handleLike() {
  isLiked.value = !isLiked.value
  emit('like', props.post.id)
}
</script>

<template>
  <article class="post-card glass-card">
    <!-- 内容 -->
  </article>
</template>

<style scoped>
/* 组件作用域样式 */
</style>
```

### 路由守卫

```ts
// router/index.ts
router.beforeEach((to, from) => {
  // 认证检查
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  // 已在登录态访问 /login，重定向到首页
  if (to.name === 'login' && authStore.isLoggedIn) {
    return { name: 'home' }
  }
})
```

**规则**: 路由级认证放 `router.beforeEach`，不要散落在各组件的 `onMounted`。

### 性能实践

| 场景 | 做法 |
|------|------|
| 大列表 | `<VirtualList>` 或 `v-memo` |
| 昂贵计算 | `computed` 缓存 |
| 副作用清理 | `onUnmounted` 中清除 listener/timer |
| 异步数据 | `Suspense` + `await` 或 `<script setup>` 顶层 |
| 重渲染隔离 | `defineComponent` + `markRaw` 标记不可变对象 |

### 组件测试

```ts
import { mount } from '@vue/test-utils'
import PostCard from './PostCard.vue'

test('emits like event on button click', async () => {
  const wrapper = mount(PostCard, {
    props: { post: mockPost }
  })
  await wrapper.find('[data-testid="like-btn"]').trigger('click')
  expect(wrapper.emitted('like')).toBeTruthy()
  expect(wrapper.emitted('like')[0]).toEqual([mockPost.id])
})
```

**规则**: 测试组件的**输入/输出**（props/emit/render），不测试内部状态。

---

## Iconography — 增强规范

### 图标选择决策树

```
需要装饰性图标？── 是 ─→ aria-hidden="true"，不参与交互
       │
       否（功能图标）
       │
   有可见文字标签？── 是 ─→ 仅作辅助，可省略 aria-label
       │
       否（纯图标按钮）
       │
   → 必须有 aria-label
```

### 状态图标映射

| 状态 | Lucide Icon | 颜色 | 用法 |
|------|-------------|------|------|
| 成功 | `CheckCircle` | `#00FF00` | 操作成功 |
| 错误 | `XCircle` | `#EF4444` | 操作失败 |
| 警告 | `AlertTriangle` | `#FFB020` | 警告提示 |
| 信息 | `Info` | `#00FFFF` | 一般信息 |
| 加载 | `Loader2` | `#00FFFF` + spin | 加载中 |

---

## Responsive Breakpoints

```ts
// tailwind.config.ts
export default {
  theme: {
    screens: {
      sm: '640px',   // Mobile landscape
      md: '768px',   // Tablet
      lg: '1024px',  // Desktop
      xl: '1280px',  // Large desktop
      '2xl': '1536px' // 4K
    }
  }
}
```

### 断点策略

| 断点 | 设备 | 布局调整 |
|------|------|----------|
| `< 640px` | Mobile portrait | 单列、汉堡菜单、底部导航 |
| `≥ 768px` | Tablet | 2 列、侧边栏可折叠 |
| `≥ 1024px` | Desktop | 3-4 列、完整导航 |
| `≥ 1280px` | Large desktop | 内容居中、最大宽度 1440px |

**规则**: 移动优先，**禁止**出现水平滚动条；字体/间距用 rem 不用 px。

---

## Pre-Delivery Checklist v2

### 视觉
- [ ] 配色严格遵循 Color Palette（无新色）
- [ ] 字体系统：标题 Fira Code / 正文 Fira Sans
- [ ] 间距遵循 4/8/12/16/24/32/48/64 体系
- [ ] 圆角遵循 8/12/16/24 体系

### 图标
- [ ] 无 emoji 作为图标
- [ ] 全部使用 Lucide 图标库
- [ ] 纯图标按钮有 `aria-label`
- [ ] 装饰图标有 `aria-hidden="true"`

### 交互
- [ ] 所有可点击元素 `cursor: pointer`
- [ ] 悬停/聚焦状态有 150-300ms 过渡
- [ ] 状态变化不引起布局位移（无 scale 变换）
- [ ] 加载/空/错误三态完整

### 表单
- [ ] 字段有 `<label>` 关联
- [ ] 错误提示有 `role="alert"` + 紧邻字段
- [ ] 提交按钮三态（idle/loading/result）
- [ ] 错误有恢复路径

### 无障碍
- [ ] 正文对比度 ≥ 4.5:1
- [ ] 焦点环可见（≥ 2px）
- [ ] 键盘可达所有交互
- [ ] 尊重 `prefers-reduced-motion`
- [ ] 装饰元素 `aria-hidden`

### 响应式
- [ ] 375px / 768px / 1024px / 1440px 四档断点
- [ ] 无水平滚动
- [ ] 移动端汉堡菜单或底部导航

### Vue 3 规范
- [ ] 组件 PascalCase
- [ ] Props/Emits 类型化
- [ ] 路由守卫在 `router.beforeEach`
- [ ] 副作用在 `onUnmounted` 清理

### 性能
- [ ] 图片 `loading="lazy"` + WebP/AVIF
- [ ] 字体 `font-display: swap`
- [ ] 路由级代码分割
- [ ] CLS < 0.1

---

## 参考资源

- **ui-ux-pro-max** 数据源：`styles.csv`、`ux-guidelines.csv`、`typography.csv`、`stacks/vue.csv`
- **Google Fonts**: [Fira Code + Fira Sans](https://fonts.google.com/share?selection.family=Fira+Code:wght@400;500;600;700|Fira+Sans:wght@300;400;500;600;700)
- **图标库**: [Lucide](https://lucide.dev/) / [Heroicons](https://heroicons.com/)
- **WCAG**: [W3C WAI](https://www.w3.org/WAI/standards-guidelines/wcag/)
- [ ] No content hidden behind fixed navbars
- [ ] No horizontal scroll on mobile
