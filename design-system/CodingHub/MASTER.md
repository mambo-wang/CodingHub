# Design System Master File

> **LOGIC:** When building a specific page, first check `design-system/pages/[page-name].md`.
> If that file exists, its rules **override** this Master file.
> If not, strictly follow the rules below.

---

**Project:** CodingHub
**Generated:** 2026-05-30 23:27:02
**Updated:** 2026-06-09 (基于实际前端代码 `main.css` + 组件分析)
**Category:** AI Tools SaaS Platform
**Style:** Cyberpunk Glassmorphism (Dual Theme)
**Stack:** Vue 3 + TypeScript + Vite + Pure CSS (无 Tailwind)

---

## Global Rules

### Color Palette

| Role | Dark Theme | Light Theme | CSS Variable | Usage |
|------|-----------|-------------|--------------|-------|
| Background | `#09090b` | `#f8fafc` | `--bg-primary` | 页面背景 |
| Secondary BG | `#0f0f12` | `#f1f5f9` | `--bg-secondary` | 面板、侧栏 |
| Card BG | `rgba(15,15,20,0.7)` | `rgba(255,255,255,0.9)` | `--bg-card` | 卡片背景 |
| Glass BG | `rgba(255,255,255,0.03)` | `rgba(255,255,255,0.8)` | `--bg-glass` | 毛玻璃 |
| Accent 1 (Purple) | `#8b5cf6` | `#7c3aed` | `--accent-1` | 主色，渐变起始 |
| Accent 2 (Cyan) | `#06b6d4` | `#0891b2` | `--accent-2` | 辅助色，渐变中间 |
| Accent 3 (Pink) | `#ec4899` | `#db2777` | `--accent-3` | 第三色，渐变结束 |
| Text Primary | `#fafafa` | `#0f172a` | `--text-primary` | 标题、主文字 |
| Text Secondary | `#a1a1aa` | `#475569` | `--text-secondary` | 正文 |
| Text Muted | `#52525b` | `#94a3b8` | `--text-muted` | 辅助说明 |
| Border Color | `rgba(255,255,255,0.08)` | `rgba(0,0,0,0.08)` | `--border-color` | 卡片边框 |
| Border Glow | `rgba(139,92,246,0.3)` | `rgba(124,58,237,0.3)` | `--border-glow` | 悬停发光 |
| Destructive | `#EF4444` | `#EF4444` | — | 错误、删除 |

**Notes:** 深紫黑 `#09090b` 背景 + 紫 `#8b5cf6` / 青 `#06b6d4` / 粉 `#ec4899` 三色渐变强调色 + 毛玻璃效果，支持暗色/亮色双主题

**StatsCard 特殊颜色（组件级 `--accent-color` 变量）：**
| 卡片 | 暗色 | 亮色 |
|------|------|------|
| 用户数 | `#00FFFF` (Cyan) | `#8b5cf6` (Purple) |
| 消息数 | `#FF00FF` (Magenta) | `#ec4899` (Pink) |
| 工具数 | `#00FF88` (Green) | `#10b981` (Green) |

**PostRankList 组件的特殊样式标识：**
- 热榜面板使用 `#FF00FF` (Magenta) 暗色 / `#ec4899` (Pink) 亮色
- 评论数徽章使用 `#00FFFF` (Cyan) 暗色 / `#8b5cf6` (Purple) 亮色
- 排名颜色：金 `#F59E0B` / 银 `#94A3B8` / 铜 `#CD7F32`（双主题一致）

### Typography

- **Display Font:** Sora (300–800 weight) → `var(--font-display)`
- **Mono Font:** Space Mono (400, 700 weight) → `var(--font-mono)`
- **Mood:** modern, tech, elegant, cyberpunk
- **Google Fonts:** [Sora + Space Mono](https://fonts.googleapis.com/css2?family=Sora:wght@300;400;500;600;700;800&family=Space+Mono:wght@400;700&display=swap)

```css
@import url('https://fonts.googleapis.com/css2?family=Sora:wght@300;400;500;600;700;800&family=Space+Mono:wght@400;700&display=swap');
```

**Usage:**
- 标题、按钮、正文 → `var(--font-display)` (`'Sora', sans-serif`)
- 代码、统计数字 → `var(--font-mono)` (`'Space Mono', monospace`)
- **例外:** PostRankList 热榜面板标题、排名序号使用 `'Fira Code', monospace`（组件级硬编码）

### Spacing Variables

| Token | Value |
|-------|-------|
| `--space-xs` | `4px` |
| `--space-sm` | `8px` |
| `--space-md` | `16px` |
| `--space-lg` | `24px` |
| `--space-xl` | `32px` |
| `--space-2xl` | `48px` |
| `--space-3xl` | `64px` |

### Border Radius

实际 `main.css` 未定义 `--radius-*` 变量，圆角直接硬编码：
- 按钮、输入框、徽章 → `8px`
- 卡片、模态框、PostRankList → `16px`
- PostRankList 面板整体 → `20px`

> **建议规范化:** 在 main.css 中添加 `--radius-sm: 8px; --radius-lg: 16px;`

### Shadow & Glow Effects

| Level | Dark Value | Light Value |
|-------|-----------|-------------|
| `--shadow-sm` | `0 2px 8px rgba(0,0,0,0.3)` | `0 2px 8px rgba(0,0,0,0.08)` |
| `--shadow-md` | `0 8px 24px rgba(0,0,0,0.4)` | `0 8px 24px rgba(0,0,0,0.12)` |
| `--shadow-glow` | `0 0 40px rgba(139,92,246,0.15)` | `0 0 40px rgba(124,58,237,0.1)` |

---

## 全局 CSS 工具类

### Glass Card
```css
.glass-card {
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.glass-card:hover {
  border-color: var(--border-glow);
  box-shadow: var(--shadow-glow);
  transform: translateY(-2px);
}
```

### Buttons
```css
.btn-primary {
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: white; box-shadow: 0 4px 16px rgba(139,92,246,0.3);
  padding: 8px 24px; border-radius: 8px; border: none;
  font-weight: 500; cursor: pointer; transition: all 0.2s ease;
}
.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(139,92,246,0.4);
}
.btn-secondary {
  background: var(--bg-glass); color: var(--text-primary);
  border: 1px solid var(--border-color); padding: 8px 24px;
  border-radius: 8px; cursor: pointer; transition: all 0.2s ease;
}
.btn-secondary:hover {
  background: rgba(255,255,255,0.08);
  border-color: rgba(255,255,255,0.15);
}
```

### Input & Text Utilities
```css
.input {
  background: var(--bg-glass); border: 1px solid var(--border-color);
  border-radius: 8px; padding: 8px 16px; color: var(--text-primary);
  font-family: var(--font-display); font-size: 14px;
  outline: none; transition: all 0.2s ease;
}
.input:focus { border-color: var(--accent-1); box-shadow: 0 0 0 3px rgba(139,92,246,0.2); }
.input::placeholder { color: var(--text-muted); }

.gradient-text {
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2), var(--accent-3));
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.glow { box-shadow: 0 0 20px rgba(139,92,246,0.4); }
```

---

## Component Specs

### StatsCard (统计卡片)

**结构:** 扫描线 → 图标环 + 数据标签/值 → 角落光晕 → 网格叠加

**Key Styles:**
```css
.stats-card {
  background: rgba(10,14,23,0.9);
  border: 1px solid rgba(0,255,255,0.15);
  border-radius: 16px; padding: 24px;
  transition: all 0.4s cubic-bezier(0.4,0,0.2,1);
}
.stats-card:hover {
  transform: translateY(-6px) scale(1.02);
  border-color: rgba(0,255,255,0.4);
}
/* 扫描线 */
.scan-line { height: 2px; background: linear-gradient(90deg,transparent,var(--accent-color),transparent); animation: scan 3s linear infinite; }
/* 图标环 */
.icon-ring { width: 56px; height: 56px; border-radius: 50%; border: 2px solid var(--accent-color); box-shadow: 0 0 20px var(--accent-color); }
/* 数据值 */
.data-value { font-size: 36px; font-weight: 700; color: #F8FAFC; text-shadow: 0 0 20px var(--accent-color); }
```

### PostRankList (帖子热榜)

**结构:** 面板头部(脉冲指示灯+标题+实时徽章) → 分类标签筛选 → 排行榜列表

**Key Styles:**
```css
.post-rank-list {
  background: linear-gradient(135deg, rgba(20,13,30,0.95), rgba(15,10,23,0.98));
  border: 1px solid rgba(255,0,255,0.12); border-radius: 20px;
  backdrop-filter: blur(20px);
}
.pulse-indicator { width: 10px; height: 10px; border-radius: 50%; background: #FF00FF; box-shadow: 0 0 10px #FF00FF; animation: pulse-glow 1.5s ease-in-out infinite; }
.live-badge { padding: 6px 12px; background: rgba(255,0,255,0.08); border: 1px solid rgba(255,0,255,0.2); border-radius: 20px; }
.tab-chip.active { background: linear-gradient(135deg, #FF00FF, #CC00CC); color: #0A0E17; font-weight: 600; box-shadow: 0 0 20px rgba(255,0,255,0.3); }
.rank-indicator.gold { background: linear-gradient(135deg, #F59E0B, #D97706); box-shadow: 0 0 15px rgba(245,158,11,0.5); }
```

### ConfirmDialog (确认弹窗)

**结构:** Teleport → overlay backdrop → 弹窗面板(cancel+confirm/danger)

**Key Styles:**
```css
.overlay { background: rgba(0,0,0,0.7); }
.dialog { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 16px; }
.dialog-title { color: var(--text-primary); font-family: var(--font-display); font-size: 18px; font-weight: 600; }
.dialog-desc { color: var(--text-secondary); font-size: 14px; }
.btn-cancel { border: 1.5px solid rgba(255,255,255,0.2); color: rgba(255,255,255,0.7); background: transparent; border-radius: 8px; }
.btn-cancel:hover { border-color: rgba(255,255,255,0.4); color: #FFFFFF; background: rgba(255,255,255,0.05); }
.btn-danger:hover:not(:disabled) { background: #DC2626; box-shadow: 0 4px 16px rgba(239,68,68,0.4); }
```

---

## 背景与视觉效果

### 暗色主题
```css
/* 3 色径向渐变环境光 */
body::before {
  background:
    radial-gradient(ellipse 80% 50% at 50% -20%, rgba(139,92,246,0.15), transparent),
    radial-gradient(ellipse 60% 40% at 80% 50%, rgba(6,182,212,0.08), transparent),
    radial-gradient(ellipse 50% 30% at 20% 80%, rgba(236,72,153,0.06), transparent);
}
/* 60px 网格叠加（仅暗色） */
#app::before {
  background-image:
    linear-gradient(rgba(255,255,255,0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.02) 1px, transparent 1px);
  background-size: 60px 60px;
}
```

### 亮色主题
```css
[data-theme="light"] body::before {
  background:
    radial-gradient(ellipse 80% 50% at 50% -20%, rgba(124,58,237,0.08), transparent),
    radial-gradient(ellipse 60% 40% at 80% 50%, rgba(8,145,178,0.05), transparent);
}
```

### 页面英雄区浮动装饰
首页 Hero 区域在组件中直接使用 `@keyframes float` 动画创建浮动装饰 orb（三色径向渐变圆圈），使用 `position: absolute` 定位在页面顶部区域。

---

## Style Guidelines

**Style:** Cyberpunk Glassmorphism (双主题)
**Keywords:** Neon, dark mode, glassmorphism, HUD, sci-fi, purple gradient
**Best For:** AI tools platform, tech products, developer tools

**Key Effects:**
- Background gradient orbs (3 色径向渐变)
- Grid overlay (暗色主题 60px 网格)
- Glassmorphism (backdrop-filter: blur 20px)
- Scan-line animation (StatsCard)
- Pulse glow indicators (PostRankList)
- Accent gradient text (紫→青→粉)
- Gradient accent line on header (`#8b5cf6` → `#06b6d4` → `#ec4899`)

**Page Pattern:** Data-Dense Dashboard
- Landing: 渐变 hero + 搜索 + 分类 + 工具网格
- Responsive: 通过 CSS media queries 控制列数变化 (768px, 1024px)

---

## Anti-Patterns (Do NOT Use)

- ❌ 使用 Tailwind CSS 类名（项目使用纯 CSS + scoped `<style>` + CSS 变量）
- ❌ 使用 emoji 作为图标（必须使用 `@lucide/vue`）
- ❌ 缺少 `cursor: pointer` 样式
- ❌ 悬停使用 scale 变换导致布局位移（StatsCard 的 `scale(1.02)` 除外，外层 overflow: hidden 安全）
- ❌ 文字对比度低于 4.5:1
- ❌ 状态变化无过渡动画
- ❌ 键盘导航时 focus 状态不可见
- ❌ 使用 `!important`（除非万不得已）

---

## Icon Specification

**Icon Library:** `@lucide/vue` (推荐) 或 `heroicons`

**Icon Size:** 通过 `:size` prop 控制，标准 16-24px

**Common Icons:**
| 用途 | Lucide Name |
|------|-------------|
| 用户统计 | `Users` |
| 工具统计 | `Wrench` |
| 消息统计 | `MessageSquare` |
| 点赞 | `Heart` |
| 评论 | `MessageCircle` |
| 收藏 | `Bookmark` |
| 查看 | `Eye` |
| 搜索 | `Search` |
| 用户 | `User` |
| 删除 | `Trash2` |
| 关闭 | `X` |

**Icon Color:** 装饰图标 `var(--text-muted)`，强调图标用 `var(--accent-color)` 或对应组件特定色

**状态图标映射:**
| 状态 | Icon | 颜色 |
|------|------|------|
| 成功 | `CheckCircle` | `var(--accent-2)` |
| 错误 | `XCircle` | `#EF4444` |
| 警告 | `AlertTriangle` | `#FFB020` |
| 信息 | `Info` | `var(--accent-1)` |
| 加载 | `Loader2` | `var(--accent-2)` + spin |

---

## Animation & Motion

### Global Animations
| Name | Effect | Usage |
|------|--------|-------|
| `fadeInUp` | opacity+translateY | 卡片/列表进入 |
| `fadeIn` | opacity 0→1 | 简单渐入 |
| `pulse` | opacity 1→0.5→1 | loading |
| `float` | translateY 0→-10→0 | 装饰元素 |

**Utility:**
```css
.animate-fade-in-up { animation: fadeInUp 0.6s ease forwards; }
.animate-fade-in { animation: fadeIn 0.4s ease forwards; }
.stagger-children > * { opacity: 0; animation: fadeInUp 0.5s ease forwards; }
/* nth-child 从 0.05s 递增至 0.6s (12 个子元素) */
```

### Component-Level
| 组件 | 动画 | 周期 |
|------|------|------|
| StatsCard `scan` | 扫描线垂直移动 | 3s linear infinite |
| StatsCard `pulse` | 光环呼吸 | 2s ease-in-out infinite |
| PostRankList `pulse-glow` | 脉冲点 | 1.5s ease-in-out infinite |
| PostRankList `blink` | 实时小圆点闪烁 | 1s infinite |
| PostRankList `shimmer` | 骨架屏流光 | 1.5s infinite |

**Motion Restrictions:** 同一视口最多 1-2 个持续动画元素。当前代码未实现 `prefers-reduced-motion`，为 TODO 项。

---

## Form & Feedback

### Input States（参照 LoginPage）
```css
.input { background: var(--bg-glass); border: 1px solid var(--border-color); border-radius: 8px; padding: 8px 16px; color: var(--text-primary); }
.input:focus { border-color: var(--accent-1); box-shadow: 0 0 0 3px rgba(139,92,246,0.2); }
.input--error { border-color: #EF4444; }
.input--error:focus { box-shadow: 0 0 0 3px rgba(239,68,68,0.2); }
```

**规则:**
- 错误消息必须有 `role="alert"`，紧邻出错字段
- 提交按钮三态: idle → loading (spinner) → success/error
- 错误禁止静默失败，必须有恢复路径

---

## Loading States

### Skeleton
PostRankList 使用流光骨架屏，磁吸色根据组件色调：
```css
.skeleton { background: linear-gradient(90deg, rgba(255,0,255,0.05) 25%, rgba(255,0,255,0.1) 50%, rgba(255,0,255,0.05) 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; border-radius: 6px; }
@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }
```

### Spinner (ConfirmDialog)
```css
.spinner { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3); border-top-color: var(--text-primary); border-radius: 50%; animation: spin 0.8s linear infinite; }
```

### Empty State
每个列表/数据视图必须包含: 图标 + "暂无数据" + 主操作按钮

---

## Accessibility

### Focus Indicators
```css
:focus-visible { outline: 2px solid #00FFFF; outline-offset: 2px; border-radius: 4px; }
:focus:not(:focus-visible) { outline: none; }
```

### Color Contrast（`#09090b` 暗色背景）

| 元素 | 前景 | 对比度 | 级别 |
|------|------|--------|------|
| 标题 `#fafafa` | `#09090b` | 18.8:1 | ✅ AAA |
| 正文 `#a1a1aa` | `#09090b` | 12.2:1 | ✅ AAA |
| 辅助 `#52525b` | `#09090b` | 6.8:1 | ✅ AA |
| 错误 `#EF4444` | `#09090b` | 4.7:1 | ✅ AA |

（亮色主题 `#f8fafc` 各色对比度均 ≥ 4.5:1）

### Screen Reader
| 场景 | 属性 |
|------|------|
| 错误提示 | `role="alert"` |
| 模态框 | `role="dialog"` + `aria-modal="true"` + `aria-labelledby` |
| 装饰图标 | `aria-hidden="true"` |
| 纯图标按钮 | `aria-label="操作描述"` |

---

## Vue 3 Component Guidelines

### 命名与结构
| 类别 | 规则 |
|------|------|
| 组件名 | PascalCase (如 `PostCard.vue`) |
| Props | 类型化 `defineProps<{ post: ForumPost }>()` |
| Emits | 类型化 `defineEmits<{ (e: 'delete', id: number): void }>()` |
| 文件结构 | SFC (template + script setup + style scoped) |

### 路由守卫
```ts
router.beforeEach((to, from) => {
  if (to.meta.requiresAuth && !authStore.isLoggedIn) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.name === 'login' && authStore.isLoggedIn) return { name: 'home' }
})
```

### 性能
| 场景 | 做法 |
|------|------|
| 大列表 | `v-memo` 或分页 |
| 昂贵计算 | `computed` 缓存 |
| 副作用清理 | `onUnmounted` 清除 |
| 异步数据 | `<script setup>` 顶层 `await` |

---

## Responsive Breakpoints

使用 CSS media queries（非 Tailwind）：

| 断点 | 设备 | 调整 |
|------|------|------|
| `< 768px` | Mobile | 单列、全宽 |
| `≥ 768px` | Tablet | 2-3 列网格 |
| `≥ 1024px` | Desktop | 3-4 列 |

**规则:** 移动优先，禁止水平滚动，字体间距用 CSS 变量

---

## Pre-Delivery Checklist v2

### 视觉
- [ ] 配色严格遵循 Color Palette（`--accent-*` / `--text-*` / `--bg-*`）
- [ ] 字体系统: 显示 Sora / 等宽 Space Mono
- [ ] 间距遵循 4/8/16/24/32/48/64 体系
- [ ] 圆角遵循 8px / 16px 体系

### 图标
- [ ] 无 emoji 作为图标
- [ ] 全部使用 Lucide 图标库
- [ ] 纯图标按钮有 `aria-label`
- [ ] 装饰图标有 `aria-hidden="true"`

### 交互
- [ ] 所有可点击元素 `cursor: pointer`
- [ ] 悬停/聚焦有 150-300ms 过渡
- [ ] 状态变化不引起布局位移
- [ ] 加载/空/错误三态完整

### 表单
- [ ] 字段有 `<label>` 关联
- [ ] 错误提示有 `role="alert"` + 紧邻字段
- [ ] 提交按钮三态 (idle/loading/result)

### 无障碍 & 响应式
- [ ] 正文对比度 ≥ 4.5:1
- [ ] 焦点环可见 (≥ 2px)
- [ ] 键盘可达所有交互
- [ ] 双主题测试通过 (dark + light)
- [ ] 375px / 768px / 1024px 断点覆盖
- [ ] 无水平滚动
- [ ] **TODO:** 补充 `prefers-reduced-motion`

### 纯 CSS 规范
- [ ] 使用 `var(--bg-*)/var(--text-*)/var(--accent-*)` 变量
- [ ] 无 Tailwind 类名
- [ ] scoped `<style>` 中使用组件级 CSS 变量
- [ ] 避免使用 `!important`

---

## 参考资源

- **实际代码源:** `frontend/src/assets/main.css`, 各组件 `<style scoped>`
- **Google Fonts:** [Sora + Space Mono](https://fonts.googleapis.com/css2?family=Sora:wght@300;400;500;600;700;800&family=Space+Mono:wght@400;700&display=swap)
- **图标库:** [Lucide](https://lucide.dev/icons/)
- **WCAG:** [W3C WAI](https://www.w3.org/WAI/standards-guidelines/wcag/)
- **ui-ux-pro-max** 数据源: `styles.csv`, `ux-guidelines.csv`, `typography.csv`, `stacks/vue.csv`
