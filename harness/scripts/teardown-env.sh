#!/bin/bash
# 清理脚本 - 停止所有服务

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$PROJECT_ROOT"

echo "=== 清理环境 ==="

# 停止后端服务
pkill -f "gradlew bootRun" 2>/dev/null || true
pkill -f "GradleDaemon" 2>/dev/null || true

# 停止前端服务
pkill -f "vite" 2>/dev/null || true

echo "✓ 所有服务已停止"