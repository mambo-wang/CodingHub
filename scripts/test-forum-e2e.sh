#!/bin/bash
# CodingHub E2E 测试脚本
# 使用 opencli 浏览器自动化测试论坛功能

SESSION="iaihub-e2e"
BASE_URL="http://localhost:5173"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

cleanup() {
    opencli browser ${SESSION} close 2>/dev/null
}
trap cleanup EXIT

# ============================================
# 测试用例 1: 首页工具广场
# ============================================
test_homepage() {
    log_info "测试 1: 首页工具广场..."
    opencli browser ${SESSION} open "${BASE_URL}/" >/dev/null 2>&1
    opencli browser ${SESSION} wait time 2 >/dev/null 2>&1

    local result=$(opencli browser ${SESSION} find --css "button" --limit 10 2>/dev/null | grep "工具广场" | wc -l)
    if [ "$result" -gt 0 ]; then
        log_info "  ✅ 导航栏正常"
        return 0
    else
        log_error "  ❌ 导航栏异常"
        return 1
    fi
}

# ============================================
# 测试用例 2: 论坛帖子列表
# ============================================
test_forum_list() {
    log_info "测试 2: 论坛帖子列表..."
    opencli browser ${SESSION} open "${BASE_URL}/forum" >/dev/null 2>&1
    opencli browser ${SESSION} wait time 2 >/dev/null 2>&1

    local result=$(opencli browser ${SESSION} find --css ".sidebar-nav" --limit 1 2>/dev/null | grep "sidebar-nav" | wc -l)
    if [ "$result" -gt 0 ]; then
        log_info "  ✅ 左侧导航栏存在"
    else
        log_error "  ❌ 左侧导航栏不存在"
        return 1
    fi
    return 0
}

# ============================================
# 测试用例 3: 帖子详情页
# ============================================
test_post_detail() {
    log_info "测试 3: 帖子详情页..."
    opencli browser ${SESSION} open "${BASE_URL}/forum/posts/6" >/dev/null 2>&1
    opencli browser ${SESSION} wait time 2 >/dev/null 2>&1

    local like_result=$(opencli browser ${SESSION} find --css ".like-btn" --limit 1 2>/dev/null | grep "ref" | wc -l)
    if [ "$like_result" -gt 0 ]; then
        log_info "  ✅ 点赞按钮存在"
    else
        log_error "  ❌ 点赞按钮不存在"
        return 1
    fi
    return 0
}

# ============================================
# 测试用例 4: 未登录访问需认证页面
# ============================================
test_auth_guard() {
    log_info "测试 4: 未登录访问需认证页面跳转..."
    opencli browser ${SESSION} open "${BASE_URL}/forum/my-posts" >/dev/null 2>&1
    opencli browser ${SESSION} wait time 2 >/dev/null 2>&1

    local url=$(opencli browser ${SESSION} get url 2>/dev/null)
    if echo "$url" | grep -q "/login"; then
        log_info "  ✅ 正确跳转到登录页"
    else
        log_error "  ❌ 未正确跳转，当前 URL: $url"
        return 1
    fi
    return 0
}

# ============================================
# 测试用例 5: 我的帖子页面
# ============================================
test_my_posts() {
    log_info "测试 5: 我的帖子页面..."
    opencli browser ${SESSION} open "${BASE_URL}/forum/my-posts" >/dev/null 2>&1
    opencli browser ${SESSION} wait time 2 >/dev/null 2>&1

    local result=$(opencli browser ${SESSION} find --css ".sidebar-nav" --limit 1 2>/dev/null | grep "sidebar-nav" | wc -l)
    if [ "$result" -gt 0 ]; then
        log_info "  ✅ 左侧导航栏存在"
    else
        log_error "  ❌ 左侧导航栏不存在"
        return 1
    fi
    return 0
}

# ============================================
# 测试用例 6: 我的收藏页面
# ============================================
test_my_favorites() {
    log_info "测试 6: 我的收藏页面..."
    opencli browser ${SESSION} open "${BASE_URL}/forum/my-favorites" >/dev/null 2>&1
    opencli browser ${SESSION} wait time 2 >/dev/null 2>&1

    local result=$(opencli browser ${SESSION} find --css ".sidebar-nav" --limit 1 2>/dev/null | grep "sidebar-nav" | wc -l)
    if [ "$result" -gt 0 ]; then
        log_info "  ✅ 左侧导航栏存在"
    else
        log_error "  ❌ 左侧导航栏不存在"
        return 1
    fi
    return 0
}

# ============================================
# 测试用例 7: 发布帖子（需登录）
# ============================================
test_create_post() {
    log_info "测试 7: 发布帖子（需登录）..."
    opencli browser ${SESSION} open "${BASE_URL}/forum/editor" >/dev/null 2>&1
    opencli browser ${SESSION} wait time 2 >/dev/null 2>&1

    local url=$(opencli browser ${SESSION} get url 2>/dev/null)
    # 如果跳转到登录页，说明需要登录，测试通过但提示需登录
    if echo "$url" | grep -q "/login"; then
        log_warn "  ⚠️ 发布帖子需要登录，跳转到登录页"
        return 0
    fi

    local result=$(opencli browser ${SESSION} state 2>/dev/null | grep "发布帖子" | wc -l)
    if [ "$result" -gt 0 ]; then
        log_info "  ✅ 编辑器页面正常"
    else
        log_error "  ❌ 编辑器页面异常"
        return 1
    fi
    return 0
}

# ============================================
# 主函数
# ============================================
main() {
    echo "=========================================="
    echo "CodingHub E2E 测试"
    echo "=========================================="
    echo ""

    local failed=0

    test_homepage; [ $? -ne 0 ] && ((failed++))
    test_forum_list; [ $? -ne 0 ] && ((failed++))
    test_post_detail; [ $? -ne 0 ] && ((failed++))
    test_auth_guard; [ $? -ne 0 ] && ((failed++))
    test_my_posts; [ $? -ne 0 ] && ((failed++))
    test_my_favorites; [ $? -ne 0 ] && ((failed++))
    test_create_post; [ $? -ne 0 ] && ((failed++))

    echo "=========================================="
    if [ $failed -eq 0 ]; then
        log_info "所有测试通过! ✅"
    else
        log_error "$failed 个测试失败 ❌"
    fi
    echo "=========================================="

    return $failed
}

main "$@"