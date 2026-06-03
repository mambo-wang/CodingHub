---
name: build-and-package
description: Use when building and packaging the project for deployment, including frontend build, backend JAR, and creating distribution zip
---

# 项目打包与部署

## 概述

自动化项目的打包流程：前端构建、后端 JAR 打包、文件复制、压缩成 ZIP 包。

## 何时使用

- 需要部署项目到生产环境
- 需要打包项目进行分发
- 提交代码前需要生成部署包

## 快速流程

```
前端构建 (npm run build)
    ↓
后端打包 (./gradlew bootJar)
    ↓
复制到目标目录
    ↓
压缩成 ZIP
```

## 详细步骤

### 1. 前端构建

```bash
cd frontend
npm run build
```

产物：`frontend/dist/`

### 2. 后端打包

```bash
cd backend
./gradlew bootJar
```

产物：`backend/build/libs/ai-tool-square-*.jar`

### 3. 复制到部署目录

```bash
# 复制后端 JAR
cp backend/build/libs/ai-tool-square-*.jar /path/to/deploy/

# 复制前端构建产物
rm -rf /path/to/deploy/dist
cp -r frontend/dist /path/to/deploy/
```

### 4. 创建 ZIP 包

```bash
cd /path/to/parent
zip -r deploy.zip deploy/
```

## 部署目录结构

```
deploy/
├── ai-tool-square-*.jar   # 后端
├── dist/                   # 前端
│   ├── index.html
│   └── assets/
├── config/
│   └── application.yaml
├── nginx.conf
└── start.sh
```

## 常见问题

| 问题 | 解决方案 |
|------|----------|
| 前端 TypeScript 错误 | 使用 `npx vite build` 跳过类型检查临时构建 |
| JAR 文件名变更 | 检查 `backend/build/libs/` 确认实际文件名 |
| ZIP 包含不需要的文件 | 使用 `zip -r --exclude` 排除 |

## 快速命令汇总

```bash
# 一键打包
cd /Users/kirito/repos/iaihub/frontend && npm run build
cd /Users/kirito/repos/iaihub/backend && ./gradlew bootJar
cp /Users/kirito/repos/iaihub/backend/build/libs/ai-tool-square-*.jar /path/to/deploy/
rm -rf /path/to/deploy/dist && cp -r /Users/kirito/repos/iaihub/frontend/dist /path/to/deploy/
cd /path/to/parent && zip -r deploy.zip deploy/
```