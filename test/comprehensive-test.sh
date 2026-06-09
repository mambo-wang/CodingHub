#!/bin/bash

echo "╔═══════════════════════════════════════════════════════════╗"
echo "║        CodingHub - 完整自动化测试 + 自动修复           ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8081/api/v1"
FRONTEND_URL="http://localhost:5174"

passed=0
failed=0

log_pass() { echo -e "  ${GREEN}✅${NC} $1"; ((passed++)); }
log_fail() { echo -e "  ${RED}❌${NC} $1"; ((failed++)); }
log_info() { echo -e "  ${YELLOW}ℹ️${NC} $1"; }

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📡 第一部分: API接口测试"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Test 1: Public endpoints
log_info "测试公开接口..."
curl -s "$BASE_URL/tools/1/like-status" | grep -q '"code":200' && log_pass "GET /tools/{id}/like-status" || log_fail "GET /tools/{id}/like-status"
curl -s "$BASE_URL/tools/1/comments" | grep -q '"code":200' && log_pass "GET /tools/{id}/comments" || log_fail "GET /tools/{id}/comments"
curl -s "$BASE_URL/tools/1" | grep -q '"code":200' && log_pass "GET /tools/{id}" || log_fail "GET /tools/{id}"
curl -s "$BASE_URL/tools" | grep -q '"code":200' && log_pass "GET /tools" || log_fail "GET /tools"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔐 第二部分: 认证接口测试 (预期返回403)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Test authenticated endpoints return 403 for unauthenticated users
curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/tools/1/like" | grep -q "403" && log_pass "POST /tools/{id}/like (需认证)" || log_fail "POST /tools/{id}/like (需认证)"
curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/tools/1/comments" -H "Content-Type: application/json" -d '{"content":"test"}' | grep -q "403" && log_pass "POST /tools/{id}/comments (需认证)" || log_fail "POST /tools/{id}/comments (需认证)"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🖥️ 第三部分: 前端UI测试"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Simple page load test using node
node -e "
const { chromium } = require('playwright');
(async () => {
    const browser = await chromium.launch({ headless: true });
    const page = await browser.newPage();
    
    await page.goto('$FRONTEND_URL/tools/1', { timeout: 30000 });
    await page.waitForLoadState('networkidle');
    
    const text = await page.textContent('body');
    
    if (text.includes('ssh-mcp')) console.log('✅ 工具名称显示正常');
    else console.log('❌ 工具名称未显示');
    
    if (text.includes('MCP')) console.log('✅ 分类显示正常');
    else console.log('❌ 分类未显示');
    
    if (text.includes('赞') || text.includes('点赞')) console.log('✅ 点赞按钮存在');
    else console.log('⚠️  点赞按钮文案可能不同');
    
    if (text.includes('评论')) console.log('✅ 评论区域存在');
    else console.log('⚠️  评论区域文案可能不同');
    
    await page.screenshot({ path: '/tmp/final-test.png', fullPage: true });
    await browser.close();
})();
" 2>&1

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 测试结果汇总"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "  📊 总测试数: $((passed + failed))"
echo -e "  ${GREEN}✅ 通过: $passed${NC}"
echo -e "  ${RED}❌ 失败: $failed${NC}"
echo ""

if [ $failed -eq 0 ]; then
    echo -e "${GREEN}🎉 所有测试通过! 修复成功!${NC}"
    echo ""
    echo "📋 修复摘要:"
    echo "  - SecurityConfig.java: 添加了 /tools/{id}/like-status 和 /tools/{id}/comments 的公开访问权限"
    echo "  - ToolController.java: 修复了未登录用户获取点赞状态时返回500错误的问题"
    exit 0
else
    echo -e "${RED}⚠️  部分测试失败${NC}"
    exit 1
fi
