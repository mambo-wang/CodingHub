#!/bin/bash
# 环境启动脚本 - 启动依赖服务

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$PROJECT_ROOT"

echo "=== 启动依赖服务 ==="

# 检查 MySQL 是否运行
if command -v mysqladmin &> /dev/null; then
    if mysqladmin ping -h localhost -u root -proot 2>/dev/null; then
        echo "✓ MySQL 已运行"
    else
        echo "⚠ MySQL 未运行，请确保 MySQL 服务已启动"
        echo "  macOS: brew services start mysql"
        echo "  Linux: sudo systemctl start mysql"
    fi
else
    echo "⚠ 未找到 mysqladmin，请确保 MySQL 已安装"
fi

echo ""
echo "依赖服务检查完成"