---
title: "2026-06-15-Allow-Any-Tool-Attachment-Format"
type: Source
description: "将工具附件上传的格式安全责任从「扩展名白名单」前移到「运行时校验 + 下载方自决」。`UploadConfig.allowedExtensions` 默认值改为空列表，`ToolFileService.validateFile()` 仅在白名单非空时才拦截，其余不变式（≤50MB/≤200MB、防路径穿越、归属校验、覆盖语义）全部保留。"
aliases: [任意附件格式设计, tool-attachment-format-design]
origin: "openspec/changes/archive/2026-06-15-allow-any-tool-attachment-format/design.md"
source_type: "md"
tags: [tool, upload, openspec, design]
title: "允许任意工具附件格式设计"
version: "2026-06-15"
---
# 允许任意工具附件格式设计

## Summary
将工具附件上传的格式安全责任从「扩展名白名单」前移到「运行时校验 + 下载方自决」。`UploadConfig.allowedExtensions` 默认值改为空列表，`ToolFileService.validateFile()` 仅在白名单非空时才拦截，其余不变式（≤50MB/≤200MB、防路径穿越、归属校验、覆盖语义）全部保留。

## Key Points
- `allowedExtensions` 默认值改为 `new ArrayList<>()`（非 null），`application.yml` 移除 `allowed-extensions` 段；空列表/null 等价于「无白名单」。
- 前端 `UploadPage/EditToolPage` 删除 `allowedExtensions` 数组与扩展名预过滤，仅保留大小预检，提示文案改为「支持任意格式」。
- 头像白名单（`avatarAllowedExtensions`）独立保留，不受本次影响（防 XSS/SVG 注入维度不同）。
- Non-Goals：不做恶意内容扫描、不动 `tool_file` 表结构、不修改 MCP 匿名上传分支。

## Relevance
对应 [工具广场](../modules/工具广场.md) 上传管线的约束放宽，体现「平台仅守大小/路径/归属」的分发理念。

## Referenced By
- [工具广场](../modules/工具广场.md)