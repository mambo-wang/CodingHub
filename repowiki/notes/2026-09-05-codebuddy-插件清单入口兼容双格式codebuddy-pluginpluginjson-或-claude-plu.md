---
type: general
title: "CodeBuddy 插件清单入口兼容双格式：.codebuddy-plugin/plugin.json 或 .claude-plugin/plugin.json"
tags: ["codebuddy", "codinghub", "general"]
aliases: [".claude-plugin", "plugin-manifest", "插件清单入口", "codebuddy-plugin"]
metadata:
  date: 2026-09-05
  related_modules: ["plugin"]
status: stable
author: iamwangbao-163-com
generated: { by: codewiki/5.6.0, at: 2026-09-05T11:46:25Z }
stale_after: 2027-01-03
---

## 背景

此前认为 `.codebuddy-plugin/plugin.json` 是 CodeBuddy 客户端**唯一**的插件清单入口（2026-08-27 笔记，向 CodingHub 发布时的打包规范）。实测验证：CodeBuddy 客户端同样识别 `.claude-plugin/plugin.json`（Claude Code 插件格式），该目录任一存在即可触发组件注册。

## 结论

- CodeBuddy 客户端插件清单入口有两个合法位置：`.codebuddy-plugin/plugin.json` **或** `.claude-plugin/plugin.json`（已实测：.claude-plugin 形式可被识别并注册命令/技能）。
- 根目录 `plugin.json` 客户端**均不识别**（仅平台后端读取用于元数据展示），任何情况下都不能替代上述两种清单入口。
- 发布到 CodingHub 插件市场时仍按平台打包规范使用 `.codebuddy-plugin/plugin.json`（平台 marketplace 生成与规范文档均以此为准）；`.claude-plugin` 主要面向 Claude Code 生态插件包的兼容场景。

## 影响

修正此前"唯一入口"的绝对化认知——CodeBuddy 兼容 Claude Code 插件格式，`.claude-plugin/plugin.json` 目录结构的插件包无需改写即可被 CodeBuddy 客户端识别。
