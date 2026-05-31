---
name: ui-text-and-theme-fix
overview: 修改前端UI文本（Logo和标语）并为热榜组件添加浅色主题适配
todos:
  - id: modify-app-header
    content: 修改 AppHeader.vue 的 Logo 文字和导航按钮为 CodingHub
    status: completed
  - id: modify-home-page
    content: 修改 HomePage.vue 首页标语，去除"工具"二字
    status: completed
    dependencies:
      - modify-app-header
  - id: add-light-theme-tool-rank
    content: 为 ToolRankList.vue 添加浅色主题适配样式
    status: completed
    dependencies:
      - modify-home-page
  - id: add-light-theme-post-rank
    content: 为 PostRankList.vue 添加浅色主题适配样式
    status: completed
    dependencies:
      - add-light-theme-tool-rank
  - id: add-light-theme-overview
    content: 为 OverviewPage.vue 添加浅色主题适配样式
    status: completed
    dependencies:
      - add-light-theme-post-rank
---

## 用户需求

1. 将页面左上角的"AI工具广场"改为"CodingHub"
2. 将首页标语"发现AI 工具的无限可能"改为"发现AI的无限可能"
3. 热榜UI增加对浅色主题的适配，切换皮肤时配色要能正常变化

## 功能描述

- Logo文字修改：顶部导航栏的Logo从"AI 工具广场"改为"CodingHub"
- 首页标语修改：Hero区域的标语去除"工具"二字，保留"发现AI的无限可能"
- 热榜主题适配：热榜页面和组件需要支持浅色主题，切换主题时配色正确变化

## 技术方案

- 前端框架：Vue 3 + TypeScript
- 样式系统：Scoped CSS + CSS 变量主题切换
- 实现方式：在热榜组件中添加 `[data-theme="light"]` 样式覆盖，将硬编码颜色替换为浅色主题适配的颜色

## 修改文件清单

| 文件 | 修改内容 |
| --- | --- |
| `frontend/src/components/AppHeader.vue` | 第47行Logo文字、第52行导航按钮文字 |
| `frontend/src/pages/HomePage.vue` | 第101行标语文本 |
| `frontend/src/components/ToolRankList.vue` | 添加浅色主题样式覆盖 |
| `frontend/src/components/PostRankList.vue` | 添加浅色主题样式覆盖 |
| `frontend/src/pages/OverviewPage.vue` | 添加浅色主题样式覆盖 |