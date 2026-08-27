---
type: decision
title: 内容实体计数更新应改用 repository 层原子 SQL（@Modifying）而非读改写 + save()
tags:
- decision
- forumpost
- preupdate
- toolrepository
metadata:
  date: 2026-08-26
  task_id: 日常维护
  related_modules:
  - tool
  - forum
  - video
  - plugin
  severity: medium
  source_ref: conversations/conv-manually_attached_skills-Please-use-the-use_skill-tool-to-in-4.md
  scene: 修复计数更新污染 updatedAt
status: stable
generated:
  by: codewiki/5.4.4
  at: 2026-08-26 13:54:21+00:00
stale_after: '2027-08-26'
origin: conversation
verified:
- by: human:wangbao
  at: '2026-08-26T13:58:36Z'
---

## 决策

把内容实体（[Tool](../backend/src/main/java/com/iaihub/toolbox/model/Tool.java)、[ForumPost](../backend/src/main/java/com/iaihub/toolbox/model/forum/ForumPost.java)、[Video](../backend/src/main/java/com/iaihub/toolbox/model/video/Video.java)、Plugin）的计数更新从「加载实体 → 内存改字段 → `save()`」改为 repository 层的**原子 SQL**（`@Modifying` JPQL），绕过实体生命周期回调。

## Rationale

1. 消除 `@PreUpdate` 对 `updatedAt` 的污染——计数更新不再刷新审计时间戳。
2. 附带消除了原来「读-改-写」计数更新在并发下的**丢更新隐患**（原子 SQL 由数据库保证）。
3. `decrement` 用 `CASE WHEN` 保下限，与实体原逻辑保持一致。

## 实现要点

- `ToolRepository` 新增 8 个原子方法（view/like/comment/download/favorite 增减），同步维护热度分 `score`，权重公式 `score = view×1 + download×2 + like×3 + favorite×4 + comment×5`。
- `ForumPost`、`Video`、`Plugin` 无 `favoriteCount` 字段（`UnifiedFavoriteService` 只对 TOOL 更新计数），score 权重为 `view×1 + like×3 + comment×5`；`Video` 原子方法带 `status='NORMAL'` 过滤。
- 调用点扩展：`ForumPostService.getPostById`、`VideoService.getVideoDetail`、`PluginService.getDetail` 改为原子 +1 + 重查（私有帖权限检查不变）；`UnifiedLikeService`/`UnifiedCommentService` 的相应分支改用原子增减。
- `updateLikeCount` 前保留 `validateTargetExists` 校验。

## 结果

四类实体的计数更新均已改为原子 SQL，`updatedAt` 只反映真正的内容编辑；全局复查后端已无「调用实体计数方法 + save()」的遗留路径（唯一剩余匹配为 `PluginService.clonePlugin` 的字段拷贝，仅复制内存值、非持久化，不影响 `updatedAt`）。
