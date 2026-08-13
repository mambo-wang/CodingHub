---
type: Conversation
title: "conversation 20260813T044040Z"
status: pending
stale_after: 2026-11-11
generated: { by: codewiki/5.2.2, at: 2026-08-13T04:40:40Z }
tags: ["codinghub", "ai-tool-square"]
captured_at: "2026-08-13T04:40:40Z"
content_hash: "sha256:592403694b2500d5abeb2ecf0bd10095d332b67d8c0ef16f53cc30cde1522b4d"
keep_raw: false
link_to: ""
source_session: "3230e1fa12b04d2caa47ebe8a418bdff"
turn_count: 13
---

# Conversation Transcript

user: 我上午使用codinghub mcp做skill上传的时候，发现没有打成压缩包就上传了，请修改mcp的相关prompt描述，如果skill包含多个文件，一定要压缩后上传
assistant: 我来先了解一下 codinghub skill 和 mcp prompt 的配置位置。
assistant: 让我查看 codinghub skill 的关键文件和 MCP 工具定义。
assistant: 让我查看工具参考文档和后端 MCP 工具定义。
assistant: 让我查看 MCP 工具定义文件，特别是上传工具的描述。
assistant: 让我查看 Prompt 模板定义文件 `McpPromptProvider.java`。
assistant: 让我查看 codinghub skill 文档中关于上传的现有描述。
assistant: 让我查看 MCP 工具定义中文件上传工具的当前描述。
assistant: 我已经找到问题所在。MCP 工具描述和 prompt 模板中关于"多文件压缩"的描述不够强硬（用了"建议"等措辞），导致 Agent 上传时直接传了多个文件。现在修改 MCP 相关的 prompt 描述：
assistant: 现在修改 `McpPromptProvider.java` 中发布和更新工具的 prompt 描述：
assistant: 现在同步更新 codinghub skill 文档中对应的描述（`SKILL.md` 和 `gotchas.md`），保持行为一致：
assistant: 再检查一下 `tool-reference.md` 中是否有相关描述，同时检查 lint 错误：
assistant: 已完成修改。lint 检查无错误。修改内容如下：

## 修改汇总

**核心问题**：MCP 上传工具描述和 prompt 模板中"多文件压缩"的要求不够强硬（原措辞是"建议先压缩"），导致 Agent 上传 Skill 时直接逐个上传了多个文件。

### 1. MCP 工具描述 — `McpSdkServerConfig.java`
`h3_coding_hub_tool_file_upload` 的描述从"多文件**建议**先压缩再上传"改为强制规则：
- 多文件（SKILL.md + references/ + scripts/ 等）**必须先压缩整个目录为 zip** 再上传，**禁止**逐个直接上传
- 仅当目录中只有一个文件（如只有 SKILL.md）时才可直接上传

### 2. MCP Prompt 模板 — `McpPromptProvider.java`
- **publish-tool**：第 6 步"准备上传文件"改为先检查文件数量，多文件必须压 zip（保留目录结构），仅单 SKILL.md 才直接上传，明确"严禁逐个上传多个文件"
- **update-tool**：第 5 步"准备新版本文件"同样强化为强制压缩规则

### 3. CodingHub skill 文档（保持一致）
- `SKILL.md`：第 8 条改为"上传前必须检查文件数量……严禁将多个文件逐个直接上传"
- `gotchas.md`：第 7 条从"多文件**才需**压缩"改为"多文件**必须**压缩……禁止逐个上传多个文件"

后端修改生效需要**重新编译/重启后端服务**（`make backend`）才能让 MCP 客户端拉到新的工具描述和 prompt。

另外，这次"多文件 Skill 未压缩就上传"的问题值得沉淀为 Wiki 经验笔记（pitfall 类型），要记录吗？
