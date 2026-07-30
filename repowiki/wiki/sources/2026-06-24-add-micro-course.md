---
title: "2026-06-24-Add-Micro-Course"
type: Source
description: "新增独立的视频共享模块（MVP）：参考论坛互动模式构建视频上传/列表/详情/流式播放，以及点赞/评论/收藏。后端新建 `model/video`、`repository/video`、`service/video`、`controller/video` 与 `VideoStorageConfig`；前端新增 video 页面/组件/store/service/types。"
aliases: [微课模块设计, micro-course-design]
origin: "openspec/changes/archive/2026-06-24-add-micro-course/design.md"
source_type: "md"
tags: [video, module, openspec, design]
title: "微课视频模块设计"
version: "2026-06-24"
---
# 微课视频模块设计

## Summary
新增独立的视频共享模块（MVP）：参考论坛互动模式构建视频上传/列表/详情/流式播放，以及点赞/评论/收藏。后端新建 `model/video`、`repository/video`、`service/video`、`controller/video` 与 `VideoStorageConfig`；前端新增 video 页面/组件/store/service/types。

## Key Points
- MVP 范围：本地磁盘存储 `uploads/videos/`、`HTTP Range` 边下边播（Spring `ResourceRegion`）、单文件 ≤1GB、仅 MP4、不转码；不含弹幕/分类/分片/OSS。
- 4 张表：`video`/`video_comment`/`video_like`/`video_favorite`，`video.status` 枚举 `NORMAL/DELETED` 软删除。
- 互动 API：`POST /api/videos/{id}/like|favorite`（toggle）、`/comments`（分页+发表）、`GET /api/videos/{id}/stream`（免登录 Range）。
- 免登录白名单：`GET /api/videos`、`/{id}`、`/{id}/stream`、`/{id}/comments`。
- SecurityConfig 扩展；`VideoService.upload()` 预留 `VideoChunkController` 以便后续分片上传。

## Relevance
对应 [微课视频](../modules/微课视频.md) 实体模块；复用 [[统一互动架构]] 前的独立互动实现（后被统一互动取代）。

## Referenced By
- [微课视频](../modules/微课视频.md)
- [[统一互动架构]]