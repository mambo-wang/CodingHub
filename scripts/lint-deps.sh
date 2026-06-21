#!/bin/bash
# lint-deps.sh — 检查循环依赖
# 兼容 macOS bash 3.2（无关联数组，使用临时文件）
set -eo pipefail

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SRC_DIR="$BASE_DIR/backend/src/main/java/com/iaihub/toolbox"
ERRORS=0

# 临时文件存储依赖关系（每行：source_package target_package）
DEPS_FILE=$(mktemp)
trap 'rm -f "$DEPS_FILE"' EXIT

# 构建依赖图
while IFS= read -r -d '' file; do
    rel_path="${file#$SRC_DIR/}"
    pkg_name=$(dirname "$rel_path" | cut -d'/' -f1)

    while IFS= read -r import_line; do
        import_pkg=$(echo "$import_line" | sed 's/^import com\.iaihub\.toolbox\.//' | sed 's/;.*//' | cut -d'.' -f1)
        [ -z "$import_pkg" ] && continue
        [ "$import_pkg" = "$pkg_name" ] && continue  # 跳过自引用

        echo "$pkg_name $import_pkg" >> "$DEPS_FILE"
    done < <(grep '^import com\.iaihub\.toolbox\.' "$file" 2>/dev/null || true)
done < <(find "$SRC_DIR" -name "*.java" -print0)

# 去重
sort -u "$DEPS_FILE" -o "$DEPS_FILE"

# 检查直接循环依赖：A→B 且 B→A
while IFS=' ' read -r src tgt; do
    # 检查反向依赖
    if grep -q "^${tgt} ${src}$" "$DEPS_FILE"; then
        echo "✗ 发现循环依赖: $src → $tgt → $src"
        echo "  → 打破循环的方式："
        echo "    1. 提取共享接口到独立包"
        echo "    2. 使用事件/回调解耦"
        echo "    3. 合并为一个包"
        echo ""
        ERRORS=$((ERRORS + 1))
    fi
done < "$DEPS_FILE"

# 检查三角循环：A→B→C→A
PACKAGES=$(awk '{print $1}' "$DEPS_FILE" | sort -u)
for a in $PACKAGES; do
    # A 的直接依赖
    b_list=$(awk -v src="$a" '$1==src {print $2}' "$DEPS_FILE")
    for b in $b_list; do
        # B 的直接依赖
        c_list=$(awk -v src="$b" '$1==src {print $2}' "$DEPS_FILE")
        for c in $c_list; do
            [ "$c" = "$a" ] && continue  # 直接循环已检测
            # 检查 C→A
            if grep -q "^${c} ${a}$" "$DEPS_FILE"; then
                echo "✗ 发现三角循环依赖: $a → $b → $c → $a"
                echo "  → 打破循环的方式："
                echo "    1. 提取共享接口到独立包"
                echo "    2. 使用事件/回调解耦"
                echo ""
                ERRORS=$((ERRORS + 1))
            fi
        done
    done
done

# 去重错误计数（直接循环会被报两次）
if [ "$ERRORS" -gt 0 ]; then
    echo "=== 发现循环依赖（共 $ERRORS 条） ==="
    exit 1
else
    echo "✓ 无循环依赖"
    exit 0
fi
