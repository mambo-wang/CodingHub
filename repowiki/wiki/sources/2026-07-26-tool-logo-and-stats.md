---
title: "2026-07-26-Tool-Logo-And-Stats"
type: Source
description: "补齐工具广场视觉与统计：工具支持自定义 logo（三级回退：工具 logo→分类默认 logo→系统占位图），分类可配置默认 logo；卡片底部展示浏览/点赞/收藏/下载四项统计（格式化 1.2k / 16.5 万）。收藏量复用 unified_favorite 聚合，下载量用 tool_file.download_count 原子自增。"
aliases: [工具Logo与统计设计, tool-logo-stats-design]
origin: "openspec/changes/archive/2026-07-26-tool-logo-and-stats/design.md"
source_type: "md"
tags: [tool, logo, stats, openspec, design]
title: "工具 Logo 与统计设计"
version: "2026-07-26"
---
# 工具 Logo 与统计设计

## Summary
补齐工具广场视觉与统计：工具支持自定义 logo（三级回退：工具 logo→分类默认 logo→系统占位图），分类可配置默认 logo；卡片底部展示浏览/点赞/收藏/下载四项统计（格式化 1.2k / 16.5 万）。收藏量复用 unified_favorite 聚合，下载量用 tool_file.download_count 原子自增。

## Key Points
- logo 复用通用图片端点 POST /api/v1/uploads/images 上传 + 轻量绑定端点 POST /api/v1/tools/{id}/logo；回退链在后端 toSummaryDTO 解析，前端只拿最终 URL。
- 收藏量 UnifiedFavoriteRepository.countByTargetTypeAndTargetIdIn(TOOL, ids)；下载量 tool_file.download_count 用 @Modifying 原子 UPDATE 自增，sumDownloadCountGroupByToolId 聚合。
- 批量加载：每页 2 次 IN 查询组装 Map 避免 N+1；数字格式化放前端 formatCount(n)。
- DDL 由 ddl-auto:update 自动加 tool.logo_url / category.logo_url / tool_file.download_count 三列，无数据回填。

## Relevance
对应 [工具广场](../modules/工具广场.md) 的展示增强，依赖 [[统一互动架构]]（收藏权威源为 unified_favorite）与 [[标签系统]] 一致「复用不重造」。

## Referenced By
- [工具广场](../modules/工具广场.md)
- [[统一互动架构]]