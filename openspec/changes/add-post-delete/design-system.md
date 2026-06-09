# Design System: 帖子删除

> 引用全局设计系统 `design-system/CodingHub/MASTER.md`，仅列出本次变更涉及的 UI 组件、交互状态与可访问性约束。

## 1. Applicability

本次变更涉及前端 UI，**生成**本 artifact。

涉及的核心场景：

- 帖子详情页新增"删除"按钮（**仅作者可见**）
- 通用 `ConfirmDialog` 组件（**新增**）
- 我的帖子页列表项右上角"删除"图标按钮
- 删除请求的 loading / error 反馈

## 2. Related Components

| 组件/页面 | 用途 | 状态覆盖 |
|-----------|------|----------|
| `PostDetailPage.vue`（修改） | 帖子详情页"删除"按钮 | normal / hover / focus / disabled（loading） |
| `MyPostsPage.vue`（修改） | 列表项"删除"图标按钮 | normal / hover / focus |
| `components/common/ConfirmDialog.vue`（新增） | 通用确认对话框 | normal / loading / hidden；模态遮罩态 |
| `Toast`（复用现有） | 成功/失败提示 | success / error |
| `PostCard.vue`（修改） | 我的帖子列表卡片 | 删除按钮 hover 透传 |

## 3. Visual Tokens

### Colors（取自全局设计系统）

| 用途 | Token | Hex | 说明 |
|------|-------|-----|------|
| 删除按钮（normal） | `--color-destructive` | `#EF4444` | 红色填充背景，文字白 |
| 删除按钮（hover） | `--color-destructive` + `--glow-cyan` | `#EF4444` + `rgba(239,68,68,0.3)` | 红色辉光 |
| 删除图标按钮（normal） | `--text-secondary` | `rgba(255,255,255,0.7)` | 与文字同色 |
| 删除图标按钮（hover） | `--color-destructive` | `#EF4444` | 变红 |
| 对话框背景 | `--color-card-bg` | `rgba(255,255,255,0.08)` | 毛玻璃 |
| 对话框遮罩 | — | `rgba(0,0,0,0.5)` | 半透明黑 |
| 确认按钮（危险态） | `--color-destructive` | `#EF4444` | 红色背景 |
| 取消按钮 | `--color-border` | `rgba(255,255,255,0.15)` | 描边按钮 |

### Typography

- 按钮文字：`font-family: 'Fira Sans', sans-serif; font-weight: 500; font-size: 14px;`
- 对话框标题：`font-family: 'Fira Code', monospace; font-weight: 600; font-size: 18px;`
- 对话框描述：`font-family: 'Fira Sans', sans-serif; font-weight: 400; font-size: 14px; color: var(--text-secondary);`

### Spacing & Radius

| 元素 | 间距/圆角 |
|------|-----------|
| 删除按钮 padding | `8px 16px` |
| 删除按钮 radius | `8px`（与 .btn-primary 一致） |
| 图标按钮 padding | `6px` |
| 对话框 padding | `24px` |
| 对话框 radius | `16px`（与 .glass-card 一致） |
| 按钮间距 | `12px` |

## 4. Interaction States

### 删除按钮（详情页）

| 状态 | 样式 |
|------|------|
| normal | `background: #EF4444; color: #FFFFFF;` |
| hover | `background: #EF4444; box-shadow: 0 0 20px rgba(239,68,68,0.3); transform: translateY(-1px);` |
| focus | `outline: 2px solid #00FFFF; outline-offset: 2px;`（与全局 focus-visible 一致） |
| disabled / loading | `background: rgba(239,68,68,0.5); cursor: not-allowed;` 内含 `<Loader2 />` 旋转图标 |

### 删除图标按钮（列表项）

| 状态 | 样式 |
|------|------|
| normal | `color: var(--text-secondary);` |
| hover | `color: #EF4444; background: rgba(239,68,68,0.1);` |
| focus | `outline: 2px solid #00FFFF; outline-offset: 2px;` |
| active | `transform: scale(0.95); transition: 150ms;` |

### 确认对话框

| 状态 | 样式 |
|------|------|
| hidden | `display: none`（无障碍：移除 `aria-modal`） |
| opening | `opacity 0 → 1; transform scale(0.95) → 1; duration 200ms ease-out;` |
| loading | "确认删除"按钮 disabled 显示 spinner；"取消"按钮 disabled |
| 遮罩点击 | 同"取消" |

## 5. Responsive Strategy

| 断点 | 行为 |
|------|------|
| `< 640px` (mobile) | 删除按钮与点赞/收藏按钮垂直堆叠；确认对话框占满 90% 宽度，最大 360px |
| `≥ 640px` (tablet) | 按钮水平排列；对话框固定宽度 400px |
| `≥ 1024px` (desktop) | 与现有帖子详情页布局保持一致 |

## 6. Accessibility Requirements

- [ ] 所有按钮有可读文字标签或 `aria-label`（如 `aria-label="删除帖子"`）
- [ ] 纯图标按钮必须有 `aria-label`，例如 `aria-label="删除此帖"`
- [ ] `ConfirmDialog` 必须有 `role="dialog"`、`aria-modal="true"`、`aria-labelledby` 指向标题、`aria-describedby` 指向描述
- [ ] 打开对话框时焦点自动移入对话框，关闭时焦点回到触发按钮
- [ ] 焦点环可见（`outline: 2px solid var(--color-primary)`，offset 2px）
- [ ] 颜色对比度：白字 on 红底（#EF4444）= 4.7:1 ≥ AA
- [ ] `prefers-reduced-motion: reduce` 媒体查询下关闭淡入/缩放动画
- [ ] 错误提示使用 `role="alert"` 或 `aria-live="assertive"`
- [ ] 键盘可达：Tab 循环、Esc 关闭、Enter 触发

## 7. Open Questions

无。本次 change 复用现有后端 + 现有设计系统 tokens，不引入新色/新字体/新组件库。
