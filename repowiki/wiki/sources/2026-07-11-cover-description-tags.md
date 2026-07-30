---
title: "2026-07-11-Cover-Description-Tags"
type: Source
description: "补齐三类内容的展示元数据：微课支持前端 Canvas 截帧封面、工具新增独立 `description` 短描述字段、跨模块统一标签体系（共享 `tag` 表 + 各模块独立关联表）。论坛标签打通现有 ForumTag 后端到前端。"
aliases: [封面描述标签设计, cover-description-tags-design]
origin: "openspec/changes/archive/2026-07-11-cover-description-tags/design.md"
source_type: "md"
tags: [tag, cover, description, openspec, design]
title: "封面/描述/标签设计"
version: "2026-07-11"
---
# 封面/描述/标签设计

## Summary
补齐三类内容的展示元数据：微课支持前端 Canvas 截帧封面、工具新增独立 `description` 短描述字段、跨模块统一标签体系（共享 `tag` 表 + 各模块独立关联表）。论坛标签打通现有 ForumTag 后端到前端。

## Key Points
- 封面用前端 `<video>+<canvas>` 截帧转 Blob 上传（零后端依赖），路径 `covers/{videoId}.jpg`。
- `Tool.description` 独立 VARCHAR(200) 纯文本，与 Markdown `content` 分离。
- 统一标签：`tag` 表带 `tag_type`(TOOL/FORUM/VIDEO)，全局唯一名 + 独立关联表（`tool_tag`/`video_tag`/新 `forum_post_tag`）；旧 `forum_tag` 保留不迁移。
- 任何登录用户可创建标签；不引入云存储、不做标签权限/层级树；MCP 标签支持保持现状。

## Relevance
对应 [[标签系统]] 概念/实体；与 [工具广场](../modules/工具广场.md)、[微课视频](../modules/微课视频.md)、[[论坛模块]] 展示增强相关。

## Referenced By
- [[标签系统]]
- [工具广场](../modules/工具广场.md)
- [微课视频](../modules/微课视频.md)