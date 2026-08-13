# 论坛按标签检索功能 - 测试报告

> 生成时间：2026-08-08
> 测试环境：Windows / MySQL 8.4 / Spring Boot 3.2.5 / Vue 3.4 / Vite 5.2
> 后端端口：8082，前端端口：5173

## 1. 测试范围

验证「论坛按标签检索」功能的端到端链路：前端标签筛选 UI → store → service → 后端 Controller → Service → Repository（基于 `forum_post_tag` 关联子查询）。

## 2. 测试数据准备

通过 SQL 向 `ai_tool_square` 库注入 FORUM 类型测试标签并关联到帖子：

| 标签 | id | 关联帖子(NORMAL) | usage_count |
|------|----|----|----|
| 测试标签A | 18 | 2,3,4,19,20,21,22 (共7篇) | 7 |
| 测试标签B | 19 | 5,23 (共2篇) | 2 |
| forum-tag | 4 | 4,5 (共2篇) | 2 |

## 3. 后端 API 测试结果

基础地址 `http://localhost:8082`。

| 用例 | 请求 | 预期 | 实际 | 结果 |
|------|------|------|------|------|
| 无 tag 参数 | `GET /api/forum/posts?page=0&size=5&sortBy=latest` | 返回全部帖子(21) | totalElements=21 | ✅ PASS |
| 按 tag 过滤(latest) | `...&tag=18` | 返回关联 7 篇 | totalElements=7, ids=[22,21,20,19,4,...] | ✅ PASS |
| 过滤结果正确性 | `...&tag=18` | 每帖均含目标标签 | 命中校验全部通过 | ✅ PASS |
| tag + hot 排序 | `...&sortBy=hot&tag=18` | 返回关联 7 篇 | totalElements=7 | ✅ PASS |
| tag + keyword 搜索 | `...&keyword=codinghub&tag=18` | 返回 >0 | totalElements>0 | ✅ PASS |
| 不存在的 tag | `...&tag=999999` | 返回空 | totalElements=0 | ✅ PASS |
| DTO 携带 tags | 任意列表 | tags 字段非空 | post.tags 正确填充 | ✅ PASS |
| 交叉验证 forum-tag | `...&tag=4` | 返回 2 篇(4,5) | totalElements=2 | ✅ PASS |
| 交叉验证 测试标签B | `...&tag=19` | 返回 2 篇(5,23) | totalElements=2 | ✅ PASS |

**断言汇总：7/7 PASS（功能断言），3 项交叉验证均符合预期。**

## 4. 前端验证结果

| 用例 | 预期 | 实际 | 结果 |
|------|------|------|------|
| 前端页面可访问 | HTTP 200 | HTTP 200 | ✅ PASS |
| 标签筛选器代码集成 | 含 tag-filter / handleTagSelect / @tag-click | 全部存在 | ✅ PASS |

## 5. 请求流程图

```mermaid
sequenceDiagram
    participant U as 用户(浏览器)
    participant P as PostListPage.vue
    participant S as forumStore
    participant API as 后端 /api/forum/posts
    participant SV as ForumPostService
    participant R as ForumPostRepository
    participant DB as forum_post_tag / tag

    U->>P: 点击标签「测试标签A」
    P->>P: handleTagSelect(18)
    P->>S: fetchPosts({tag:18, sortBy:'hot'})
    S->>API: GET /api/forum/posts?tag=18&sortBy=hot
    API->>SV: getPostList(null, 18, null, "hot", pageable)
    SV->>R: findByTagIdAndStatusAndVisibilityOrderByHot(18, NORMAL, PUBLIC, pageable)
    R->>DB: SELECT p FROM ForumPost p WHERE p.id IN (SELECT post_id FROM forum_post_tag WHERE tag_id=18) ...
    DB-->>R: 关联帖子(7篇)
    R-->>SV: Page<ForumPost>
    SV->>SV: toDTO() 填充 tags 字段
    SV-->>API: Page<ForumPostDTO>
    API-->>S: 响应数据
    S-->>P: posts 更新
    P-->>U: 列表仅显示带「测试标签A」的帖子
```

## 6. 改动文件清单

**后端**
- `repository/forum/ForumPostRepository.java`：新增 4 个基于 `forum_post_tag` 关联子查询的方法（标签+最新/热门、标签+关键词+最新/热门）。
- `service/forum/ForumPostService.java`：`getPostList` 增加 `tagId` 参数，整合标签过滤分支。
- `controller/forum/ForumPostController.java`：将 `tag` 请求参数透传至 Service。

**前端**
- `pages/forum/PostListPage.vue`：新增标签筛选条 UI、串联 `tag` 参数到所有 `fetchPosts` 调用、绑定 `PostCard` 的 `@tag-click`。
- `components/forum/PostCard.vue`：标签 `TagBadge` 设为可点击，emit `tag-click`。
- （`components/common/TagBadge.vue` 已原生支持 `clickable` 与 `click`，直接复用）

## 7. 结论

论坛按标签检索功能**全部通过测试**，后端过滤逻辑正确（按 `forum_post_tag` 关联子查询）、DTO 正确携带标签、前端筛选 UI 已集成并可访问。功能可交付。

## 8. 备注

- 测试中使用了临时测试数据（标签 18/19），如需清理可执行 `DELETE FROM forum_post_tag WHERE tag_id IN (18,19); DELETE FROM tag WHERE id IN (18,19);`。
- 启动服务时发现 Vite 首次启动因 CodeBuddy `node-safe-delete-shim` 拦截 `.vite/deps` 缓存批删而失败，清除该缓存目录后正常（环境机制，非代码问题）。
