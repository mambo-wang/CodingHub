#!/bin/bash
# 代码质量检查

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "=== 代码质量检查 ==="

ERRORS=0

# 1. 检查是否有空方法返回 null
echo "检查 null 返回..."
null_returns=$(grep -rn "return null;" backend/src/main/java 2>/dev/null || true)
if [ -n "$null_returns" ]; then
    echo "✗ 发现 null 返回:"
    echo "$null_returns" | head -5
    ((ERRORS++))
fi

# 2. 检查循环中是否有数据库/接口调用（禁止）
echo "检查循环中的数据库调用..."

# 检查 for/while 中是否有 repository 调用
for file in $(find backend/src/main/java -name "*.java"); do
    # 简单检查：方法内是否有 for/while 且其中有 .find / .save / .delete 等
    if grep -qE "(for|while|stream|iterator)" "$file"; then
        if grep -qE "\.(find|save|delete|getBy|create|update).*\(" "$file"; then
            # 进一步检查是否在循环中
            content=$(cat "$file")
            if echo "$content" | grep -A5 "for\s*(" | grep -qE "\.(find|save|delete)"; then
                echo "✗ $file: 循环中检测到数据库操作"
                ((ERRORS++))
            fi
        fi
    fi
done

# 3. 检查异常是否被正确抛出
echo "检查异常处理..."
# 检查 service 层是否在应该抛异常的地方使用了 null 返回
find backend/src/main/java/com/iaihub/toolbox/service -name "*.java" -type f | while read -r file; do
    if grep -qE "return null" "$file"; then
        # 检查是否有 Optional
        if ! grep -qE "Optional" "$file"; then
            echo "⚠ $file: 方法返回 null 但未使用 Optional"
        fi
    fi
done

if [ $ERRORS -eq 0 ]; then
    echo "✓ 代码质量检查通过"
    exit 0
else
    echo "✗ 发现 $ERRORS 个质量问题"
    exit 1
fi