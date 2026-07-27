---
title: Tool 工具
type: entity
---

# Tool 工具

## 定义

Tool 是 CodingHub 工具广场的核心实体，代表一个 AI 工具/应用的展示条目。

## 代码位置

- 实体: `backend/src/main/java/com/iaihub/toolbox/model/Tool.java`
- 仓库: `backend/src/main/java/com/iaihub/toolbox/repository/ToolRepository.java`
- 服务: `backend/src/main/java/com/iaihub/toolbox/service/ToolService.java`
- 控制器: `backend/src/main/java/com/iaihub/toolbox/controller/ToolController.java`
- 前端: `frontend/src/pages/ToolsPage.vue`, `frontend/src/pages/ToolDetailPage.vue`

## 关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 工具名称 |
| description | String(TEXT) | 详细描述 |
| logoUrl | String | Logo 图片 URL |
| coverUrl | String | 封面图 URL |
| websiteUrl | String | 官网链接 |
| category | Category | 分类（ManyToOne） |
| author | User | 发布者 |
| status | ToolStatus | ACTIVE / DELETED（[[soft-delete]]） |
| isPinned | Boolean | 置顶 |
| sortWeight | Integer | 排序权重 |
| viewCount | Integer | 浏览量 |
| likeCount | Integer | 点赞数 |
| commentCount | Integer | 评论数 |
| downloadCount | Integer | 下载/使用次数 |

## 核心行为

- **CRUD**: 标准增删改查 + [[content-moderation]]
- **排序**: 置顶优先 → sortWeight 降序 → 创建时间降序
- **标签筛选**: 通过 [[Tag]] 关联表 tool_tag 实现多对多
- **附件**: [[ToolFile]] 支持任意格式上传
- **互动**: [[unified-interaction]] TargetType=TOOL
- **MCP 暴露**: 通过 [[McpServer]] 提供搜索/获取工具
- **异步上传**: [[async-batch-upload]] 批量导入

## API 端点

- `GET /api/v1/tools` — 分页列表（搜索/分类/标签/排序）
- `POST /api/v1/tools` — 创建
- `GET /api/v1/tools/{id}` — 详情
- `PUT /api/v1/tools/{id}` — 编辑
- `DELETE /api/v1/tools/{id}` — 软删除
- `POST /api/v1/tools/{id}/like` — 点赞

## 关联实体

[[User]] · [[Tag]] · [[ToolFile]] · [[Notification]]

## 设计决策来源

- tool-square-optimization (2026-06-21)
- sort-and-pin (2026-06-15)
- tool-file-format-open (2026-06-07)
- tool-filter-by-tag (2026-07-21)
- tool-logo-and-stats (2026-07-26)
- cover-desc-tags (2026-07-05)
- async-batch-upload (2026-07-01)
