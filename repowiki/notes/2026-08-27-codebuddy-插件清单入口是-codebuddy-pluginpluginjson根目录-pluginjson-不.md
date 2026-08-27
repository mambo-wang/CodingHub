---
type: pitfall
title: "CodeBuddy 插件清单入口是 .codebuddy-plugin/plugin.json，根目录 plugin.json 不能替代"
tags: ["codebuddy", "pitfall"]
aliases: ["configPath", "plugin-manifest", "codebuddy-plugin", "插件清单"]
metadata:
  date: 2026-08-27
  related_modules: ["plugin"]
  severity: high
status: stable
generated: { by: codewiki/5.4.4, at: 2026-08-27T09:34:57Z }
stale_after: 2027-02-23
---

## 背景

发布插件包时只在 zip 根目录放了 `plugin.json`（平台元数据），缺少 `.codebuddy-plugin/plugin.json`。用户安装后命令/技能全部不注册，客户端缓存的 `configPath` 指向 `.codebuddy-plugin/plugin.json`，找不到该文件时客户端跳过组件发现。

## 结论

- `.codebuddy-plugin/plugin.json` 是 CodeBuddy 客户端**唯一**的插件清单入口，打包时**必须**存在。
- 根目录 `plugin.json` 仅平台后端读取用于元数据展示（平台有回退逻辑），客户端不识别，两者不是等价的。
- 规范细节见 `.codebuddy/skills/codinghub/references/plugin-packaging.md`。

## 根因

CodeBuddy 客户端按 `configPath`（指向 `.codebuddy-plugin/plugin.json`）定位插件清单，缺文件即中止组件注册；平台与客户端对 plugin.json 的读取入口设计不一致（平台宽松回退、客户端严格）。

## 适用范围

所有向 CodingHub 插件市场发布插件包、由 CodeBuddy 客户端安装的场景。
