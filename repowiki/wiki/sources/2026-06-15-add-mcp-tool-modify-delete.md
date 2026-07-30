---
title: "2026-06-15-Add-Mcp-Tool-Modify-Delete"
type: Source
description: "为后端 MCP 层新增 `h3_coding_hub_tool_modify` 与 `h3_coding_hub_tool_file_delete` 两个工具，分别复用 `ToolService.updateTool()` 与 `ToolFileService.deleteToolFile()`。不涉及前端或数据库变更，仅扩展 `IaihubToolHandler` 与 `McpSdkServer"
aliases: [MCP工具增改删设计, mcp-tool-modify-delete-design]
origin: "openspec/changes/archive/2026-06-15-add-mcp-tool-modify-delete/design.md"
source_type: "md"
tags: [mcp, openspec, design]
title: "MCP 工具增改删设计"
version: "2026-06-15"
---
# MCP 工具增改删设计

## Summary
为后端 MCP 层新增 `h3_coding_hub_tool_modify` 与 `h3_coding_hub_tool_file_delete` 两个工具，分别复用 `ToolService.updateTool()` 与 `ToolFileService.deleteToolFile()`。不涉及前端或数据库变更，仅扩展 `IaihubToolHandler` 与 `McpSdkServerConfig`。

## Key Points
- 调用方传入 `username/password`，内部 `userService.login()` 鉴权后再校验归属权（`uploader.getId().equals(userId)`）。
- `tool_modify` 支持 `incrementVersion()` 自动递增版本号（解析 `x.y.z`，末位 +1，保留后缀如 `-beta`）。
- 写类工具通过登录态鉴权，可用 `admin / Cloud@1234`。
- 对应 [[MCP工具集]] 的工具清单扩展；与 [[统一互动架构]] 中“MCP 不绕经 InteractionController”一致。

## Relevance
扩展 [[MCP工具集]] 的写能力（此前仅查），是 AI Agent 直接运维工具的基础。

## Referenced By
- [[MCP工具集]]