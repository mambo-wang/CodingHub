---
type: pitfall
title: Spring Boot 3 应使用 jakarta.servlet 命名空间，勿误用 jgit 包下的 HttpServletRequest
tags:
- fileresolver
- githttpconfig
- httpservletrequest
- pitfall
- pluginservice
metadata:
  date: 2026-08-26
  task_id: 日常维护
  related_modules:
  - config
  - security
  severity: medium
  source_ref: conversations/conv-manually_attached_skills-Please-use-the-use_skill-tool-to-in-4.md
  scene: 修复 GitHttpConfig 编译失败
status: stable
generated:
  by: codewiki/5.4.4
  at: 2026-08-26 13:54:21+00:00
stale_after: '2027-02-22'
origin: conversation
verified:
- by: human:wangbao
  at: '2026-08-26T13:58:37Z'
---

## Background

修复计数 bug 后运行 `gradlew compileJava` 失败，报错来自 `GitHttpConfig.java`：`org.eclipse.jgit.transport.HttpServletRequest` 包名错误。

## 根因

`FileResolver` 的泛型误用了 `org.eclipse.jgit.transport.HttpServletRequest`。项目是 Spring Boot 3，Web 层使用 **jakarta** 命名空间，应使用 `jakarta.servlet.http.HttpServletRequest`。

## 正确做法

Spring Boot 3.x 中所有 `javax.servlet.*` 已迁移为 `jakarta.servlet.*`，JGit 的 `org.eclipse.jgit.transport` 下没有 `HttpServletRequest` 类。误用会导致编译失败。

## 补充

`PluginService` 曾出现 jgit 相关 lint 报错，经确认 `build.gradle` 已含 jgit 7.3.0 依赖，属 IDE 索引假阳性；区分「真实 import 错误（编译失败）」与「IDE 索引假阳性（lint 报错但 `compileJava` 通过）」可避免误判。
