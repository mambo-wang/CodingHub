#!/bin/bash
# lint-arch.sh — 检查后端架构层级依赖
# 兼容 macOS bash 3.2（无关联数组）
set -eo pipefail

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SRC_DIR="$BASE_DIR/backend/src/main/java/com/iaihub/toolbox"
ERRORS=0

# 包前缀 → 层级号（case 语句兼容 bash 3.2）
get_layer() {
    case "$1" in
        config|util|exception) echo 0 ;;
        model|dto)             echo 1 ;;
        repository)            echo 2 ;;
        service)               echo 3 ;;
        controller|mcp)        echo 4 ;;
        *)                     echo "" ;;
    esac
}

# 获取文件所属层级（取顶层目录名）
get_file_layer() {
    local rel="${1#$SRC_DIR/}"
    local top
    top=$(echo "$rel" | cut -d'/' -f1)
    get_layer "$top"
}

# 获取文件顶层包名
get_file_top_pkg() {
    local rel="${1#$SRC_DIR/}"
    echo "$rel" | cut -d'/' -f1
}

# 从 import 行提取目标顶层包名
get_import_top_pkg() {
    echo "$1" | sed 's/^import com\.iaihub\.toolbox\.//' | sed 's/;.*//' | cut -d'.' -f1
}

# 从 import 行提取完整导入路径
get_import_path() {
    echo "$1" | sed 's/^import com\.iaihub\.toolbox\.//' | sed 's/;$//'
}

# 检查每个 Java 文件
while IFS= read -r -d '' file; do
    src_layer=$(get_file_layer "$file")
    [ -z "$src_layer" ] && continue

    src_top=$(get_file_top_pkg "$file")

    # 提取所有 import 行
    while IFS= read -r import_line; do
        target_top=$(get_import_top_pkg "$import_line")
        target_layer=$(get_layer "$target_top")
        [ -z "$target_layer" ] && continue

        # 例外规则：
        # 1. config (L0) 允许导入 repository (L2) — Spring 标准用法
        # 2. L0 基础设施包可导入 L1 数据类型（model/dto 是通用数据结构）
        if [ "$src_top" = "config" ] && [ "$target_top" = "repository" ]; then
            continue
        fi
        if [ "$target_layer" = "1" ]; then
            continue
        fi

        # 层级检查：不能依赖更高层级
        if [ "$target_layer" -gt "$src_layer" ]; then
            import_path=$(get_import_path "$import_line")
            line_num=$(grep -nF "$import_line" "$file" | head -1 | cut -d: -f1)
            src_name=$(basename "$file" .java)
            rel_file="${file#$BASE_DIR/}"
            echo "✗ ${rel_file}:${line_num} — ${src_name} (L${src_layer}/${src_top}) imports ${import_path} (L${target_layer})"
            echo "  → L${src_layer} 不得依赖 L${target_layer}。修复方式："
            echo "    1. 将依赖逻辑移到中间层（如 service）"
            echo "    2. 通过接口抽象解耦"
            echo "    3. 使用依赖注入"
            echo ""
            ERRORS=$((ERRORS + 1))
        fi
    done < <(grep '^import com\.iaihub\.toolbox\.' "$file" 2>/dev/null || true)
done < <(find "$SRC_DIR" -name "*.java" -print0)

if [ "$ERRORS" -gt 0 ]; then
    echo ""
    echo "=== 发现 $ERRORS 个架构层级违规 ==="
    exit 1
else
    echo "✓ 架构层级检查通过"
    exit 0
fi
