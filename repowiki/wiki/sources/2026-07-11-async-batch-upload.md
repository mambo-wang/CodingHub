---
title: "2026-07-11-Async-Batch-Upload"
type: Source
description: "将 RAG 文档上传从「单文件同步阻塞」改为「批量异步 + 状态轮询」。支持单次最多 20 文件，上传请求立即返回 job_id 列表，后台 `asyncio + Semaphore(5)` 并行处理（转换→分块→向量化），引入本地 SQLite 记录 6 态处理状态（UPLOADING→CONVERTING→CHUNKING→EMBEDDING→READY/FAILED）。"
aliases: [异步批量上传设计, async-batch-upload-design]
origin: "openspec/changes/archive/2026-07-11-async-batch-upload/design.md"
source_type: "md"
tags: [rag, async, upload, openspec, design]
title: "异步批量上传设计"
version: "2026-07-11"
---
# 异步批量上传设计

## Summary
将 RAG 文档上传从「单文件同步阻塞」改为「批量异步 + 状态轮询」。支持单次最多 20 文件，上传请求立即返回 job_id 列表，后台 `asyncio + Semaphore(5)` 并行处理（转换→分块→向量化），引入本地 SQLite 记录 6 态处理状态（UPLOADING→CONVERTING→CHUNKING→EMBEDDING→READY/FAILED）。

## Key Points
- 元数据存储选 SQLite（RAG 本地、零配置、WAL 并发），不引入 MySQL/Redis/Celery；不修改分块策略。
- 批量端点 `POST /api/collections/{name}/documents/batch`；状态查询提供集合级 + 单文档级，前端每 3 秒轮询（非 WebSocket）。
- Embedding 批大小 `RAG_EMBEDDING_BATCH_SIZE`（默认 32）；Nginx `client_max_body_size` 调至 200M。
- 重启丢失内存任务：启动时扫描中间态标记为 FAILED，提供「重新处理」。

## Relevance
落地 [[异步批量上传]] 概念，是 [[知识库]] 文档管线的性能演进，依赖 [[RAG自适应分块]] 的分块阶段。

## Referenced By
- [[异步批量上传]]
- [[知识库]]