---
title: "Rag自适应分块"
type: Concept
description: "RAG 分块器的质量核心机制集合：保护结构性内容不被切断、自动校验降级、上下文面包屑增强召回、按文档特征自动选策略，并提供分片预览与 BM25+向量混合检索。"
aliases: [AdaptiveChunking, 自适应分块, adaptive-chunking]
domain: "rag"
source_refs: [openspec/changes/archive/2026-07-26-rag-adaptive-chunking/design.md]
tags: [rag, chunking, bm25, openspec]
title: "RAG 自适应分块"
---
# RAG 自适应分块

## Definition
RAG 分块器的质量核心机制集合：保护结构性内容不被切断、自动校验降级、上下文面包屑增强召回、按文档特征自动选策略，并提供分片预览与 BM25+向量混合检索。

## Key Points
- Protected Patterns：6 组正则预扫描标记保护区（图片/链接/公式/表格行/代码块），切分只在非保护区（O(N) 纯 CPU）。
- Validator 降级链：structural→recursive（semantic 为显式高成本不参与自动降级）；空输出/单 chunk 过大/碎片率>25%/超 2×/全远低于目标 触发。
- ContextHeader：标题面包屑随 embedding 增强召回。
- Auto Profiler：仅 4 指标（heading_count/code_ratio/has_tables/total_chars），默认 structural，极短/无结构降级 recursive。
- 混合检索：BM25 + 向量 ANN 经 RRF 融合（需 zvec≥0.5.0，旧 collection 降级纯 ANN）。

## Related
- 是 [知识库](../entities/知识库.md) 文档管线的质量核心；衔接 [[异步批量上传]] 的处理流水线。
- 分片预览 API `/chunking/preview` 支撑调试。

## Source References
- [2026-07-26-rag-adaptive-chunking](../sources/2026-07-26-rag-adaptive-chunking.md)