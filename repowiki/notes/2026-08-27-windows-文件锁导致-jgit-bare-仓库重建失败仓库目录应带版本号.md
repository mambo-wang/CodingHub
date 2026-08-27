---
type: pitfall
title: "Windows 文件锁导致 JGit bare 仓库重建失败，仓库目录应带版本号"
tags: ["accessdeniedexception", "filerepository", "pitfall", "repositorycache"]
aliases: ["JGit", "AccessDeniedException", "initGitRepo", "RepositoryCache", "文件锁"]
metadata:
  date: 2026-08-27
  related_modules: ["plugin", "backend"]
  severity: high
status: stable
generated: { by: codewiki/5.4.4, at: 2026-08-27T09:35:02Z }
stale_after: 2027-02-23
---

## 背景

后端 `initGitRepo` 对插件仓库目录先 `deleteQuietly` 再重建。Windows 下 git 服务（JGit `RepositoryCache`）缓存了已打开的 bare 仓库，删除时抛 `AccessDeniedException`（文件被进程占用），旧目录残留、新仓库残缺，HTTP clone 404/空仓库。

## 结论

- 插件 git 仓库目录改为带版本号：`<name>-<version>.git`（如 `openspec-pack-1.0.1.git`）。
- 版本更新时生成新路径，**不删除旧仓库**，彻底绕开 Windows 文件锁。
- 排查仓库残缺：检查 `backend` 日志 `initGitRepo` 是否 `complete`；Windows 上 `deleteQuietly` 失败是无声的（返回 false 不抛异常），务必校验返回值或检查日志。

## 根因

JGit `FileRepository` 打开后由 `RepositoryCache` 持有文件句柄，Windows 不允许删除被占用文件；`deleteQuietly` 静默失败导致覆盖重建不完整。

## 适用范围

Windows 环境下的插件 git 仓库管理逻辑（`backend` 插件模块）；Linux/macOS 无此文件锁问题。
