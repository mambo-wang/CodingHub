---
title: Video 微课视频
type: entity
---

# Video 微课视频

## 定义

Video 是微课模块的核心实体，支持视频上传、流式播放、弹幕互动。

## 代码位置

- 实体: `backend/src/main/java/com/iaihub/toolbox/model/video/Video.java`
- 服务: `backend/src/main/java/com/iaihub/toolbox/service/video/VideoService.java`
- 控制器: `backend/src/main/java/com/iaihub/toolbox/controller/video/VideoController.java`
- 前端: `frontend/src/pages/video/`, `frontend/src/components/video/`

## 关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| title | String | 标题 |
| description | String(TEXT) | 描述 |
| videoUrl | String | 视频文件路径 |
| coverUrl | String | 封面图 |
| duration | Integer | 时长（秒） |
| author | User | 上传者 |
| status | VideoStatus | ACTIVE / DELETED |
| viewCount | Integer | 播放量 |
| likeCount | Integer | 点赞数 |
| commentCount | Integer | 评论数 |
| favoriteCount | Integer | 收藏数 |

## 核心行为

- **流式播放**: [[http-range-streaming]] 支持断点续传
- **弹幕**: Danmaku 实体，按时间轴存储
- **互动**: [[unified-interaction]] TargetType=VIDEO
- **收藏**: VideoFavorite 关联表
- **软删除**: [[soft-delete]] 策略

## API 端点

- `GET /api/v1/videos` — 列表
- `POST /api/v1/videos` — 上传（multipart）
- `GET /api/v1/videos/{id}` — 详情
- `GET /api/v1/videos/{id}/stream` — Range 流式播放
- `POST /api/v1/videos/{id}/danmaku` — 发送弹幕
- `GET /api/v1/videos/{id}/danmaku` — 获取弹幕列表

## 关联实体

[[User]] · [[Tag]] · [[Notification]]

## 设计决策来源

- video-course-module (2026-06-11)
