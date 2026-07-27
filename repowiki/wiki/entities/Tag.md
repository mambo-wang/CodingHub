---
title: Tag 统一标签
type: entity
---

# Tag 统一标签

## 定义

Tag 是跨模块的统一标签实体，支持 TOOL / FORUM / VIDEO 三种目标类型的多对多关联。

## 代码位置

- 实体: `backend/src/main/java/com/iaihub/toolbox/model/Tag.java`
- 服务: `backend/src/main/java/com/iaihub/toolbox/service/tag/TagService.java`
- 控制器: `backend/src/main/java/com/iaihub/toolbox/controller/tag/TagController.java`
- 关联表: tool_tag, forum_post_tag, video_tag

## 关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 标签名（唯一） |
| targetType | TagTargetType | TOOL / FORUM / VIDEO |
| usageCount | Integer | 使用次数 |

## 核心行为

- **多态关联**: 通过中间表分别关联 [Tool](Tool.md)、[ForumPost](ForumPost.md)、[Video](Video.md)
- **筛选**: 工具广场按标签筛选（JOIN tool_tag）
- **自动创建**: 发布内容时自动创建不存在的标签
- **计数维护**: 关联/取消时更新 usageCount

## API 端点

- `GET /api/v1/tags` — 标签列表（按 targetType 过滤）
- `GET /api/v1/tags/popular` — 热门标签

## 关联实体

[Tool](Tool.md) · [ForumPost](ForumPost.md) · [Video](Video.md)

## 设计决策来源

- tool-filter-by-tag (2026-07-21)
- cover-desc-tags (2026-07-05)
