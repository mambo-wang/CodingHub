# Tasks

## A. Atomic TDD Task List

### Feature: Overview Stats API

- [x] RED: 编写失败测试——GET /api/overview/stats 返回统计数据 JSON，包含 userCount、postCount、toolCount
- [x] GREEN: 最小实现——创建 OverviewController 和 OverviewService，实现 stats 端点，查询数据库用户/帖子/工具数量
- [x] REFACTOR: 重构清理——提取 StatsDto，确保返回值不为 null

---

### Feature: Tool Ranks API

- [x] RED: 编写失败测试——GET /api/overview/tool-ranks 返回按类别分组的工具列表，每个类别最多 10 条
- [x] GREEN: 最小实现——创建 ToolRankDto，创建 OverviewService.toolRanks() 方法，按 category 分组并限制数量
- [x] REFACTOR: 重构清理——使用 Java Stream 的 for 循环替代，避免在循环中调用数据库

---

### Feature: Post Ranks API

- [x] RED: 编写失败测试——GET /api/overview/post-ranks 返回按类别分组的帖子列表，每个类别最多 10 条
- [x] GREEN: 最小实现——创建 PostRankDto，创建 OverviewService.postRanks() 方法，按 category 分组并限制数量
- [x] REFACTOR: 重构清理——使用 Java Stream 的 for 循环替代，避免在循环中调用数据库

---

## B. UI Implementation Tasks

### UI: OverviewPage

- [x] 实现 OverviewPage.vue 主页面组件——基于 design-system.md 规范，参考 ui-preview.html，响应式布局（桌面/平板/移动端）
- [x] 验证 OverviewPage.vue——视觉检查：暗色主题、霓虹强调色、玻璃态卡片、scanline 背景效果

---

### UI: StatsCard

- [x] 实现 StatsCard.vue 统计卡片组件——展示单个统计数据（标签/数值/图标），支持 loading 骨架屏状态
- [x] 验证 StatsCard.vue——检查：玻璃态效果、霓虹发光 hover、响应式布局

---

### UI: ToolRankList

- [x] 实现 ToolRankList.vue 工具热榜组件——双列布局（工具+帖子并排），每类别 TOP 5，Tab 切换，支持 loading/empty/error 状态
- [x] 验证 ToolRankList.vue——检查：双列布局、Tab 切换、TOP 5 显示

---

### UI: PostRankList

- [x] 实现 PostRankList.vue 帖子热榜组件——双列布局（工具+帖子并排），每类别 TOP 5，Tab 切换，支持 loading/empty/error 状态
- [x] 验证 PostRankList.vue——检查：双列布局、Tab 切换、TOP 5 显示

---

### UI: OverviewService

- [x] 实现 frontend/src/services/overview.ts——创建 API 调用服务，并行请求 stats、tool-ranks、post-ranks
- [x] 验证 overview.ts——检查：API 端点正确、错误处理完善、无循环调用接口

---

### UI: Overview Types

- [x] 实现 frontend/src/types/overview.ts——定义 TypeScript 类型：StatsDto、ToolRankDto、PostRankDto
- [x] 验证 overview.ts——检查：类型定义完整、无 any 类型

---

### UI: Router Integration

- [x] 实现路由配置——在 router 中添加 /overview 路由，指向 OverviewPage
- [x] 验证路由——检查页面可访问、URL 正确

---

## C. Browser Test

> 开发任务完成后，手动加载 `/openspec-browser-test` skill 执行浏览器测试。
> **参考**: `openspec-browser-test` skill

### Test: Overview Page E2E

- [ ] TC-001: 页面加载测试——访问 /overview，验证页面标题"数据概览"显示
- [ ] TC-002: 统计卡片显示测试——验证用户总数、帖子总数、工具总数三个卡片显示
- [ ] TC-003: 工具热榜测试——验证工具按类别分组，每组显示排名和热度
- [ ] TC-004: 帖子热榜测试——验证帖子按类别分组，每组显示排名和评论数
- [ ] TC-005: 响应式布局测试——验证在 375px、768px、1024px、1440px 断点下布局正常
- [ ] TC-006: 错误状态测试——模拟 API 失败，验证错误提示和重试按钮
- [ ] TC-007: 骨架屏测试——模拟加载延迟，验证骨架屏正确显示

---

## D. Backend API Verification

### Test: API Contracts

- [ ] TC-API-001: GET /api/overview/stats——验证返回 { userCount, postCount, toolCount } 格式
- [ ] TC-API-002: GET /api/overview/tool-ranks——验证返回按 category 分组的工具列表
- [ ] TC-API-003: GET /api/overview/post-ranks——验证返回按 category 分组的帖子列表
- [ ] TC-API-004: 空数据处理——验证无数据时返回空数组 [] 而非 null