---
title: ForumPost 论坛帖子
type: entity
---

# ForumPost 论坛帖子

## 定义

论坛帖子是 CodingHub 社区模块的核心实体，承载用户发布的技术讨论内容。

## 代码位置

- 实体: `backend/src/main/java/com/iaihub/toolbox/model/forum/ForumPost.java`
- 仓库: `backend/src/main/java/com/iaihub/toolbox/repository/forum/ForumPostRepository.java`
- 服务: `backend/src/main/java/com/iaihub/toolbox/service/forum/ForumPostService.java`
- 控制器: `backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumPostController.java`
- 前端页面: `frontend/src/pages/forum/`

## 关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| title | String | 标题（XSS 过滤） |
| content | String(TEXT) | 正文 |
| author | User | 作者（ManyToOne） |
| category | ForumCategory | 分类 |
| status | PostStatus | ACTIVE / DELETED（[[soft-delete]]） |
| viewCount | Integer | 浏览量 |
| likeCount | Integer | 点赞数 |
| commentCount | Integer | 评论数 |
| isPinned | Boolean | 置顶标记 |
| sortWeight | Integer | 排序权重 |

## 核心行为

- **发布**: 经 [[content-moderation]] 过滤后创建
- **删除**: [[soft-delete]]，级联标记评论
- **收藏**: PostFavorite 关联表，权限 isOwner||isAdmin
- **热度**: [[hotness-scoring]] 公式 `view×1 + like×3 + comment×5`
- **互动**: 通过 [[unified-interaction]] 多态表管理点赞
- **评论点赞**: 复用 [[unified-interaction]]，TargetType=FORUM_COMMENT

## API 端点

- `GET /api/forum/posts` — 分页列表（支持分类/标签/搜索/排序）
- `POST /api/forum/posts` — 创建帖子
- `GET /api/forum/posts/{id}` — 详情（+浏览量）
- `PUT /api/forum/posts/{id}` — 编辑
- `DELETE /api/forum/posts/{id}` — 软删除
- `POST /api/forum/posts/{id}/like` — 点赞/取消
- `GET /api/forum/posts/hot` — 热榜

## 关联实体

[[User]] · [[Tag]] · [[Tool]] · [[Notification]]

## 设计决策来源

- forum-module (2026-05-23)
- forum-post-deletion (2026-05-25)
- post-favorites (2026-05-27)
- hot-posts-ranking (2026-05-29)
- comment-like-unification (2026-07-11)
