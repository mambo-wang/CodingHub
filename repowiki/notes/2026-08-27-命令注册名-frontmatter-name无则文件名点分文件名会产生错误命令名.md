---
type: pitfall
title: "命令注册名 = frontmatter name（无则文件名），点分文件名会产生错误命令名"
tags: ["codebuddy", "pitfall"]
aliases: ["命令命名", "slash command", "wbnb", "frontmatter name"]
metadata:
  date: 2026-08-27
  related_modules: ["plugin"]
  severity: medium
status: stable
generated: { by: codewiki/5.4.4, at: 2026-08-27T09:34:59Z }
stale_after: 2027-02-23
---

## 背景

发布 verify-ui-pack 插件时命令文件命名为 `commands/verify-ui-pack.wbnb.md`（点分命名，无 frontmatter name）。客户端注册命令名=文件名，实际注册为 `/verify-ui-pack.wbnb` 而非 `/wbnb`，用户敲 `/wbnb` 无反应。

## 结论

- 命令注册名优先级：frontmatter `name` > 文件名。
- 正确做法：`commands/<命令名>.md` 平铺 + frontmatter 写 `name`，如 `commands/wbnb.md` + `name: "wbnb"` → `/wbnb`。
- 点分文件名仅在没有 `name` 时用作命令名，产生 `verify-ui-pack.wbnb` 这类无法预期的命令。命令正文里的斜杠标题只是说明，不参与注册。

## 根因

CodeBuddy 客户端按文件名（或 frontmatter name）注册命令；旧文档约定的"点分命名"是平台后端摘要展示行为（`listDirs` 只枚举子目录）的误读，与客户端实际规则冲突。

## 适用范围

所有包含 `commands/` 的插件包命名。
