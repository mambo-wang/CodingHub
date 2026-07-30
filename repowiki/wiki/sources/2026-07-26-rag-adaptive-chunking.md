---
title: "2026-07-26-Rag-Adaptive-Chunking"
type: Source
description: "基于 WeKnora 源码分析，为 RAG 分块器引入四机制：Protected Patterns（保护图片/链接/公式/表格行/代码块不被切断）、Validator 5 条规则自动降级到 recursive、ContextHeader（标题面包屑随 embedding 增强召回）、Auto Profiler（文档特征自动选策略），并提供分片预览 API（`/chunking/preview`）与"
aliases: [RAG自适应分块设计, rag-adaptive-chunking-design]
origin: "openspec/changes/archive/2026-07-26-rag-adaptive-chunking/design.md"
source_type: "md"
tags: [rag, chunking, bm25, embed, openspec, design]
title: "RAG 自适应分块设计"
version: "2026-07-26"
---
# RAG 自适应分块设计

## Summary
基于 WeKnora 源码分析，为 RAG 分块器引入四机制：Protected Patterns（保护图片/链接/公式/表格行/代码块不被切断）、Validator 5 条规则自动降级到 recursive、ContextHeader（标题面包屑随 embedding 增强召回）、Auto Profiler（文档特征自动选策略），并提供分片预览 API（`/chunking/preview`）与 BM25+向量 RRF 混合检索。

## Key Points
- Protected Patterns：6 组正则预扫描 byte offset 标记保护区，切分只在非保护区进行（O(N) 纯 CPU）。
- Validator 降级链：structural→recursive（semantic 为显式高成本模式不参与自动降级）；空输出/单 chunk 过大/碎片率>25%/超 2×/全远低于目标 触发。
- Auto Profiler 仅 4 指标（heading_count/code_ratio/has_tables/total_chars），默认 structural（最适 MD），极短/无结构降级 recursive。
- 混合检索：`zvec` 原生 FtsIndexParam + MultiQuery(ANN + BM25, fusion=rrf)，需 zvec≥0.5.0，旧 collection 无 FTS 时降级纯 ANN。

## Relevance
定义 [[RAG自适应分块]] 概念，是 [[知识库]] 文档管线的质量核心，与 [[异步批量上传]] 的处理流水线衔接。

## Referenced By
- [[RAG自适应分块]]
- [[知识库]]