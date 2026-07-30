---
title: "2026-07-26-Dual-Database-Config-Driven"
type: Source
description: "使后端同时兼容 MySQL 与 PostgreSQL，通过 Spring Profile 选择使用哪一种，业务代码零改动（仅 `User.java` 反引号注解微调）。Schema 由同一份实体 + `ddl-auto:update` 生成且两库均有效，对前端与调用方透明。"
aliases: [双数据库配置驱动设计, dual-database-design]
origin: "openspec/changes/archive/2026-07-26-dual-database-config-driven/design.md"
source_type: "md"
tags: [database, mysql, postgresql, profile, openspec, design]
title: "双数据库配置驱动设计"
version: "2026-07-26"
---
# 双数据库配置驱动设计

## Summary
使后端同时兼容 MySQL 与 PostgreSQL，通过 Spring Profile 选择使用哪一种，业务代码零改动（仅 `User.java` 反引号注解微调）。Schema 由同一份实体 + `ddl-auto:update` 生成且两库均有效，对前端与调用方透明。

## Key Points
- 双驱动共存：`mysql-connector-j` 与 `postgresql` 同时入 `build.gradle`；`application.yml` 用 `---` 多文档按 `spring.config.activate.on-profile` 切 mysql/postgresql。
- 方言自动探测：移除硬编码 `MySQLDialect`，Spring Boot 3.2/Hibernate 6 按 JDBC 元数据自动解析；主键 `IDENTITY`、枚举 `STRING` 两库天然可移植。
- 保留字 `user`：去反引号 `@Table(name="user")` + `hibernate.globally_quoted_identifiers=true` 全局引号，两库均视为分隔标识符。
- 初始化脚本按库分别提供（`Makefile db`/`init-db-windows.ps1`/`init-db.sql`）；同一时刻只连一个库，不引多数据源路由/双写。

## Relevance
对应 [[双数据库]] 实体/配置模块，定义 [[双库配置驱动]] 概念；影响全部业务模块的持久化层。

## Referenced By
- [[双数据库]]
- [[双库配置驱动]]