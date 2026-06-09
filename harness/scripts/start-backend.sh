#!/bin/bash
# 启动后端服务

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$PROJECT_ROOT"

echo "=== 启动后端服务 ==="

# 检查数据库
echo "检查数据库连接..."
if ! mysql -u root -proot -e "USE ai_tool_square;" 2>/dev/null; then
    echo "⚠ 数据库不存在，正在创建..."
    make db
fi

# 启动后端
echo "启动 Spring Boot 后端 (端口 8082)..."
cd backend && ./gradlew bootRun