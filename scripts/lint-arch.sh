#!/bin/bash
# Java/Gradle 项目架构检查脚本
# 检查分层依赖是否违规

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "=== Java 架构层级检查 ==="

# 定义层级
L0="config,util"
L1="model,dto"
L2="repository"
L3="service"
L4="controller"

# 允许的依赖关系
ALLOWED_DEPS="L0->外部库,L1->L0,L2->L1,L3->L0:L1:L2,L4->L1:L3"

ERRORS=0

# 检查每个 Java 文件的 import
check_layer_dependency() {
    local file="$1"
    local file_dir=$(dirname "$file" | sed "s|.*/src/main/java/com/iaihub/toolbox/||")

    # 确定当前文件所在层级
    local current_layer=""
    case "$file_dir" in
        config) current_layer="L0" ;;
        util) current_layer="L0" ;;
        model) current_layer="L1" ;;
        dto) current_layer="L1" ;;
        repository) current_layer="L2" ;;
        service) current_layer="L3" ;;
        controller) current_layer="L4" ;;
        *) return ;;
    esac

    # 提取当前文件的导入
    imports=$(grep -oE "import com\.iaihub\.toolbox\.[a-z]+" "$file" 2>/dev/null || true)

    for imp in $imports; do
        imp_pkg=$(echo "$imp" | sed 's/import //')

        # 确定被导入包所在的层级
        target_pkg=$(echo "$imp_pkg" | sed 's/import //' | cut -d. -f4-)

        case "$target_pkg" in
            config|util) target_layer="L0" ;;
            model|dto) target_layer="L1" ;;
            repository) target_layer="L2" ;;
            service) target_layer="L3" ;;
            controller) target_layer="L4" ;;
            *) continue ;;
        esac

        # 检查是否允许依赖
        case "$current_layer" in
            L0) # L0 (config) 允许依赖 L1 (model) 和 L2 (repository) —— Spring 配置标准用法
                # 但是不能依赖 L3 (service) 和 L4 (controller)
                if [[ "$target_layer" == "L3" || "$target_layer" == "L4" ]]; then
                    echo "✗ $file 违规: L0 (config/util) 不得依赖 L3 (service) 或 L4 (controller)"
                    ((ERRORS++))
                fi
                ;;
            L1) # L1 只能依赖 L0
                if [[ "$target_layer" == "L2" || "$target_layer" == "L3" || "$target_layer" == "L4" ]]; then
                    echo "✗ $file 违规: L1 (model/dto) 不得依赖 $target_layer"
                    ((ERRORS++))
                fi
                ;;
            L2) # L2 只能依赖 L1
                if [[ "$target_layer" == "L3" || "$target_layer" == "L4" ]]; then
                    echo "✗ $file 违规: L2 (repository) 不得依赖 $target_layer"
                    ((ERRORS++))
                fi
                ;;
            L3) # L3 可以依赖 L0, L1, L2
                if [[ "$target_layer" == "L4" ]]; then
                    echo "✗ $file 违规: L3 (service) 不得依赖 L4 (controller)"
                    ((ERRORS++))
                fi
                ;;
            L4) # L4 可以依赖 L1, L3
                if [[ "$target_layer" == "L0" || "$target_layer" == "L2" ]]; then
                    echo "✗ $file 违规: L4 (controller) 不得依赖 L0 (config/util) 或 L2 (repository)"
                    ((ERRORS++))
                fi
                ;;
        esac
    done
}

# 遍历所有 Java 文件
echo "检查中..."
find backend/src/main/java -name "*.java" -type f | while read -r file; do
    check_layer_dependency "$file"
done

if [[ $ERRORS -eq 0 ]]; then
    echo "✓ 架构层级检查通过"
    exit 0
else
    echo ""
    echo "✗ 发现 $ERRORS 个架构违规"
    exit 1
fi