---
name: forum-sidebar-and-header-nav
overview: 修复论坛左侧导航栏在"我的帖子"和"我的收藏"页面消失的问题，以及调整顶部导航栏菜单位置显示逻辑
todos:
  - id: create-sidebar-nav-component
    content: 新建 SidebarNav.vue 共享组件
    status: completed
  - id: modify-post-list-page
    content: 修改 PostListPage.vue 使用 SidebarNav
    status: completed
    dependencies:
      - create-sidebar-nav-component
  - id: modify-my-posts-page
    content: 修改 MyPostsPage.vue 添加 SidebarNav
    status: completed
    dependencies:
      - create-sidebar-nav-component
  - id: modify-my-favorites-page
    content: 修改 MyFavoritesPage.vue 添加 SidebarNav
    status: completed
    dependencies:
      - create-sidebar-nav-component
  - id: modify-app-header
    content: 修改 AppHeader.vue 顶部菜单逻辑
    status: completed
---

## 用户需求

1. **左侧边栏固定问题**：点击论坛左侧"我的帖子"和"我的收藏"时，左侧边栏要保留，一直在左侧位置不动
2. **顶部导航栏菜单问题**：未登录状态下，顶部导航栏应该有"工具广场"和"论坛"两个菜单；登录后增加"我的工具"菜单

## 产品概述

AI 工具广场论坛模块的前端 UI 优化，涉及左侧导航栏复用和顶部菜单条件显示。

## 核心功能

- 提取左侧导航栏为共享组件，在帖子列表、我的帖子、我的收藏页面复用
- 调整顶部导航栏菜单项的显示逻辑，根据登录状态动态切换

## 技术栈

- 前端框架：Vue 3 + TypeScript
- 状态管理：Pinia (useAuthStore)
- 路由：Vue Router
- 图标：@lucide/vue

## 实施方案

### 问题1：左侧导航栏固定

提取 `PostListPage.vue` 中的 `sidebar-nav` 为独立的共享组件 `SidebarNav.vue`，在以下页面复用：

- `PostListPage.vue` - 帖子列表页
- `MyPostsPage.vue` - 我的帖子页
- `MyFavoritesPage.vue` - 我的收藏页

组件接收 `currentPath` prop，根据当前路由高亮对应菜单项。

### 问题2：顶部导航栏菜单

修改 `AppHeader.vue` 的导航逻辑：

- **未登录状态**：显示"工具广场"（首页）和"论坛"菜单
- **登录状态**：显示"工具广场"、"论坛"、"我的工具"菜单

## 目录结构

```
frontend/src/
├── components/
│   └── forum/
│       └── SidebarNav.vue    # [NEW] 左侧导航栏共享组件
├── pages/forum/
│   ├── PostListPage.vue     # [MODIFY] 使用 SidebarNav 组件
│   ├── MyPostsPage.vue      # [MODIFY] 添加 SidebarNav
│   └── MyFavoritesPage.vue  # [MODIFY] 添加 SidebarNav
└── components/
    └── AppHeader.vue        # [MODIFY] 顶部菜单条件显示
```