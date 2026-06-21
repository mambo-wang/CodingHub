#!/bin/bash
# lint-quality.sh — 代码质量检查
set -euo pipefail

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SRC_DIR="$BASE_DIR/backend/src/main/java/com/iaihub/toolbox"
ERRORS=0

# Rule 1: Service 方法不应返回 null（应抛异常或返回 Optional）
echo "--- Rule 1: 禁止 Service 返回 null ---"
while IFS= read -r -d '' file; do
    if grep -n 'return null;' "$file" | grep -v '//.*return null' | grep -v 'test' > /dev/null 2>&1; then
        rel_file="${file#$BASE_DIR/}"
        matches=$(grep -n 'return null;' "$file" | grep -v '//.*return null')
        while IFS= read -r match; do
            echo "✗ $rel_file:$match"
            echo "  → Service 方法不应返回 null，应抛出异常或返回 Optional"
            ((ERRORS++))
        done <<< "$matches"
    fi
done < <(find "$SRC_DIR/service" -name "*.java" -print0 2>/dev/null)

# Rule 2: Controller 不应直接使用 System.out/System.err
echo "--- Rule 2: 禁止 System.out/err ---"
while IFS= read -r -d '' file; do
    if grep -n 'System\.out\.\|System\.err\.' "$file" > /dev/null 2>&1; then
        rel_file="${file#$BASE_DIR/}"
        matches=$(grep -n 'System\.out\.\|System\.err\.' "$file")
        while IFS= read -r match; do
            echo "✗ $rel_file:$match"
            echo "  → 使用 @Slf4j + log.info/error 代替 System.out/err"
            ((ERRORS++))
        done <<< "$matches"
    fi
done < <(find "$SRC_DIR" -name "*.java" -not -path "*/test/*" -print0)

# Rule 3: 空 catch 块
echo "--- Rule 3: 禁止空 catch 块 ---"
while IFS= read -r -d '' file; do
    if grep -Pn 'catch\s*\([^)]+\)\s*\{\s*\}' "$file" > /dev/null 2>&1; then
        rel_file="${file#$BASE_DIR/}"
        matches=$(grep -Pn 'catch\s*\([^)]+\)\s*\{\s*\}' "$file")
        while IFS= read -r match; do
            echo "✗ $rel_file:$match"
            echo "  → catch 块不应为空，至少添加 log.error 或注释说明"
            ((ERRORS++))
        done <<< "$matches"
    fi
done < <(find "$SRC_DIR" -name "*.java" -not -path "*/test/*" -print0)

if [ "$ERRORS" -gt 0 ]; then
    echo ""
    echo "=== 发现 $ERRORS 个代码质量问题 ==="
    exit 1
else
    echo ""
    echo "✓ 代码质量检查通过"
    exit 0
fi
