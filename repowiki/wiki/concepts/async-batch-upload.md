---
title: 异步批量上传
type: concept
---

# 异步批量上传

## 定义

工具广场支持批量导入工具附件，采用 Python asyncio + Semaphore 并发控制实现高效上传，避免串行等待。

## 架构

```mermaid
flowchart LR
    A[前端多文件选择] --> B[Python 上传脚本]
    B --> C[asyncio.Semaphore 5]
    C --> D[并发 HTTP POST]
    D --> E[Spring Boot 接收]
    E --> F[存储 + 记录]
```

## 并发控制

| 参数 | 值 | 说明 |
|------|------|------|
| 并发数 | 5 | Semaphore(5) 限制同时上传数 |
| 单文件大小 | 50MB | 超过拒绝 |
| 重试次数 | 3 | 失败自动重试 |
| 超时 | 60s | 单请求超时 |

## 6 态状态机

```
PENDING → UPLOADING → SUCCESS
                   → FAILED → RETRYING → SUCCESS
                                       → ABORTED
```

## 实现要点

- **前端**: 多文件选择 + 进度条展示
- **脚本**: Python aiohttp + asyncio，读取本地文件目录
- **后端**: 标准 multipart 接收，无特殊并发处理
- **幂等性**: 相同文件名 + 大小跳过已上传

## 关联页面

[ToolFile](../entities/ToolFile.md) · [Tool](../entities/Tool.md)

## 设计决策来源

- async-batch-upload (2026-07-01)
