---
title: 热度评分算法
type: concept
---

# 热度评分算法

## 定义

用于论坛热榜和工具广场排序的加权评分公式，反映内容的综合受欢迎程度。

## 公式

```
score = viewCount × 1 + likeCount × 3 + commentCount × 5
```

### 权重设计理由

| 行为 | 权重 | 理由 |
|------|------|------|
| 浏览 | 1 | 最低门槛，被动行为 |
| 点赞 | 3 | 主动认可，中等参与 |
| 评论 | 5 | 最高参与，创造内容 |

## 应用场景

1. **论坛热榜**: `GET /api/forum/posts/hot` 按 score 降序 Top N
2. **工具排序**: 综合排序模式使用相同公式
3. **概览页排行**: `GET /api/overview/rankings` 展示热门内容

## 实现方式

- **数据库计算**: JPQL `ORDER BY (t.viewCount + t.likeCount * 3 + t.commentCount * 5) DESC`
- **时间衰减**: 当前版本未实现（未来可加时间因子）
- **缓存**: 热榜结果可缓存 5 分钟（当前未实现）

## 关联页面

[ForumPost](../entities/ForumPost.md) · [Tool](../entities/Tool.md) · [Video](../entities/Video.md)

## 设计决策来源

- hot-posts-ranking (2026-05-29)
- overview-page (2026-05-31)
