---
title: "2026-07-11-Tool-Ui-And-Mcp-Fixes"
type: Source
description: "修复分散的小问题：管理员可进任意工具编辑页（前端权限判断对齐后端 `isOwner||isAdmin`）；HomePage 快捷上传弹窗补 `description` 字段；工具卡片 inline 展示版本号 badge；MCP `tool_create/tool_modify` 新增 `description` 与 `tags`（标签名列表）参数，后端 `TagService.resolveOr"
aliases: [工具UI与MCP修复设计, tool-ui-mcp-fixes-design]
origin: "openspec/changes/archive/2026-07-11-tool-ui-and-mcp-fixes/design.md"
source_type: "md"
tags: [tool, mcp, fix, openspec, design]
title: "工具 UI 与 MCP 修复设计"
version: "2026-07-11"
---
# 工具 UI 与 MCP 修复设计

## Summary
修复分散的小问题：管理员可进任意工具编辑页（前端权限判断对齐后端 `isOwner||isAdmin`）；HomePage 快捷上传弹窗补 `description` 字段；工具卡片 inline 展示版本号 badge；MCP `tool_create/tool_modify` 新增 `description` 与 `tags`（标签名列表）参数，后端 `TagService.resolveOrCreateTags` 自动匹配或创建标签。

## Key Points
- `EditToolPage.fetchTool()` 条件由 `uploaderId !== userId` 改为 `uploaderId !== userId && !isAdmin`，与 DetailPage `canModify` 对齐。
- MCP `tags` 接受标签名字符串列表（非 ID），更语义化、契合 AI 自动化；`tag` 表 `UNIQUE(name,type)` 防并发重复创建（捕获 `DataIntegrityViolationException` 回退）。
- 版本号 `v{version}` 青色 mono badge 内联在 `.tool-name` 后。
- 纯前端 + MCP 小幅改动，无 DB schema 变更、无新路由。

## Relevance
对齐 [工具广场](../modules/工具广场.md) 编辑权限与展示、[[标签系统]] 自动创建、[[MCP工具集]] 写工具参数。

## Referenced By
- [工具广场](../modules/工具广场.md)
- [[标签系统]]
- [[MCP工具集]]