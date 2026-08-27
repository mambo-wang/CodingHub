---
type: pitfall
title: JPA @PreUpdate 会在任意 save() 时刷新 updatedAt，读改写式计数更新会污染更新时间
tags:
- detailpage
- forumpost
- pitfall
- preupdate
- toolservice
metadata:
  date: 2026-08-26
  task_id: 日常维护
  related_modules:
  - tool
  - forum
  - video
  - plugin
  severity: high
  source_ref: conversations/conv-manually_attached_skills-Please-use-the-use_skill-tool-to-in-4.md
  scene: 工具市场更新时间显示为当前时间
status: stable
generated:
  by: codewiki/5.4.4
  at: 2026-08-26 13:53:45+00:00
stale_after: '2027-02-22'
origin: conversation
verified:
- by: human:wangbao
  at: '2026-08-26T13:58:36Z'
---

## Background

工具市场每个工具的「更新时间」始终显示为当前时间。用户报告该异常，诊断为实体生命周期回调与计数更新方式共同导致。

## 根因

`Tool` 实体通过 `@PreUpdate` 在**任何**实体保存时自动刷新 `updatedAt`。而工具的所有计数更新（浏览/点赞/收藏/评论/下载）都是「加载实体 → 内存改字段 → `save()`」的方式实现，每次 `save()` 都触发 `@PreUpdate` 把 `updatedAt` 刷新为当前时间。

最高频触发点是 `ToolService.getToolById`：**每次打开工具详情页就 `incrementViewCount()` + `save()`**，因此只要工具被浏览过一次，其 `updatedAt` 就被刷成浏览时刻，前端详情页「更新于 …」自然总是显示当前时间（`DetailPage.vue:255-259` 在 `tool.updatedAt !== tool.createdAt` 时展示「更新于」）。

同一问题在本项目扩展到 `ForumPost`、`Video`、`Plugin` 三个实体（均存在 `@PreUpdate` + 「改字段 → save()」维护计数）。

## 正确做法

计数更新应改到 repository 层做**原子 SQL**（`@Modifying` JPQL），绕过实体生命周期回调；这样 `updatedAt` 只在真实内容编辑（走实体保存）时才被刷新，不再被计数操作污染。

## 适用范围

任何用 JPA `@PreUpdate` 自动维护 `updatedAt`，但同时用「读-改-写 + save()」做高频计数字段更新的项目都会踩到同一坑。凡是「字段被高频非编辑性操作更新」的场景，都应考虑用原子更新避免污染审计时间戳。
