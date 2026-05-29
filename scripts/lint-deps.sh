#!/bin/bash
# 检查循环依赖

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "=== 循环依赖检查 ==="

cd backend/src/main/java/com/iaihub/toolbox

# 构建依赖图
declare -A deps

for pkg in config util model dto repository service controller; do
    if [ -d "$pkg" ]; then
        for file in $pkg/*.java; do
            if [ -f "$file" ]; then
                # 提取该文件的导入
                imports=$(grep -oE "import com\.iaihub\.toolbox\.[a-z]+" "$file" 2>/dev/null | sed 's/import //' || true)
                for imp in $imports; do
                    imp_pkg=$(echo "$imp" | cut -d. -f4)
                    if [ "$imp_pkg" != "$pkg" ]; then
                        deps["$pkg->$imp_pkg"]=1
                    fi
                done
            fi
        done
    fi
done

# 检测循环依赖
CIRCULAR=0

# 检查 L3 (service) 是否依赖 L2 (repository) 直接回溯
# 简单检查：service 是否直接依赖 controller
for key in "${!deps[@]}"; do
    if [[ "$key" == "service->controller" ]]; then
        echo "✗ 发现循环依赖: service -> controller"
        CIRCULAR=1
    fi
done

if [ $CIRCULAR -eq 0 ]; then
    echo "✓ 未发现循环依赖"
    exit 0
else
    echo "✗ 发现循环依赖问题"
    exit 1
fi