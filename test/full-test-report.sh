#!/bin/bash

echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║           CodingHub - 全功能测试报告                                ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""
echo "测试时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "后端服务: http://localhost:8082"
echo "前端服务: http://localhost:5174"
echo ""

BASE_URL="http://localhost:8082/api/v1"
passed=0
failed=0
total=0

test_item() {
    local name="$1"
    local result="$2"
    local reason="$3"
    ((total++))
    if [ "$result" = "PASS" ]; then
        ((passed++))
        echo -e "  ✅ $name"
    else
        ((failed++))
        echo -e "  ❌ $name: $reason"
    fi
}

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📡 1. 公开API接口测试"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "  [工具列表]"
resp=$(curl -s "$BASE_URL/tools")
echo "$resp" | grep -q '"code":200' && test_item "GET /tools" "PASS" || test_item "GET /tools" "FAIL" "返回: $resp"
echo "$resp" | grep -q '"content"' && test_item "工具列表有数据" "PASS" || test_item "工具列表有数据" "FAIL" "无工具数据"

echo ""
echo "  [工具详情]"
resp=$(curl -s "$BASE_URL/tools/1")
echo "$resp" | grep -q '"code":200' && test_item "GET /tools/1" "PASS" || test_item "GET /tools/1" "FAIL" ""
echo "$resp" | grep -q '"name"' && test_item "工具详情包含名称" "PASS" || test_item "工具详情包含名称" "FAIL" ""

echo ""
echo "  [分类]"
resp=$(curl -s "$BASE_URL/categories")
echo "$resp" | grep -q '"code":200' && test_item "GET /categories" "PASS" || test_item "GET /categories" "FAIL" ""

echo ""
echo "  [点赞状态-公开接口]"
resp=$(curl -s "$BASE_URL/tools/1/like-status")
echo "$resp" | grep -q '"code":200' && test_item "GET /tools/{id}/like-status" "PASS" || test_item "GET /tools/{id}/like-status" "FAIL" ""

echo ""
echo "  [评论列表-公开接口]"
resp=$(curl -s "$BASE_URL/tools/1/comments")
echo "$resp" | grep -q '"code":200' && test_item "GET /tools/{id}/comments" "PASS" || test_item "GET /tools/{id}/comments" "FAIL" ""

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔐 2. 认证接口测试 (需登录)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Register a test user
echo "  [注册新用户]"
REG_RESP=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"test_report_user","password":"Test123456","email":"report@test.com"}')
echo "$REG_RESP" | grep -q '"code":201' && test_item "POST /auth/register" "PASS" || test_item "POST /auth/register" "FAIL" ""

# Extract token
TOKEN=$(echo "$REG_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null)

echo ""
echo "  [登录]"
LOGIN_RESP=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"test_report_user","password":"Test123456"}')
echo "$LOGIN_RESP" | grep -q '"accessToken"' && test_item "POST /auth/login" "PASS" || test_item "POST /auth/login" "FAIL" ""

if [ -n "$TOKEN" ]; then
    echo ""
    echo "  [已登录用户操作]"
    
    echo "  - 点赞工具"
    resp=$(curl -s -X POST "$BASE_URL/tools/1/like" -H "Authorization: Bearer $TOKEN")
    echo "$resp" | grep -q '"code":200' && test_item "POST /tools/{id}/like" "PASS" || test_item "POST /tools/{id}/like" "FAIL" ""
    
    echo "  - 取消点赞"
    resp=$(curl -s -X DELETE "$BASE_URL/tools/1/like" -H "Authorization: Bearer $TOKEN")
    echo "$resp" | grep -q '"code":200' && test_item "DELETE /tools/{id}/like" "PASS" || test_item "DELETE /tools/{id}/like" "FAIL" ""
    
    echo "  - 发表评论"
    resp=$(curl -s -X POST "$BASE_URL/tools/1/comments" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -d '{"content":"测试报告评论 - 验证功能正常"}')
    echo "$resp" | grep -q '"code":201' && test_item "POST /tools/{id}/comments" "PASS" || test_item "POST /tools/{id}/comments" "FAIL" ""
    
    echo "  - 获取当前用户信息"
    resp=$(curl -s "$BASE_URL/users/me" -H "Authorization: Bearer $TOKEN")
    echo "$resp" | grep -q '"code":200' && test_item "GET /users/me" "PASS" || test_item "GET /users/me" "FAIL" ""
    
    echo "  - 获取我的工具列表"
    resp=$(curl -s "$BASE_URL/users/me/tools" -H "Authorization: Bearer $TOKEN")
    echo "$resp" | grep -q '"code":200' && test_item "GET /users/me/tools" "PASS" || test_item "GET /users/me/tools" "FAIL" ""
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🎨 3. 前端UI测试"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

cd ~/repos/iaihub/test
node -e "
const { chromium } = require('playwright');
(async () => {
    const browser = await chromium.launch({ headless: true });
    const page = await browser.newPage();
    
    // Test home page
    await page.goto('http://localhost:5174/', { timeout: 30000 });
    await page.waitForLoadState('networkidle');
    const homeText = await page.textContent('body');
    
    if (homeText.includes('工具广场') || homeText.includes('AI')) {
        console.log('  ✅ 首页加载正常');
        ((passed++));
    } else {
        console.log('  ❌ 首页加载失败');
        ((failed++));
    }
    ((total++));
    
    // Test tool detail page
    await page.goto('http://localhost:5174/tools/1', { timeout: 30000 });
    await page.waitForLoadState('networkidle');
    const detailText = await page.textContent('body');
    
    if (detailText.includes('ssh-mcp')) {
        console.log('  ✅ 工具详情页 - 工具名称显示');
        ((passed++));
    } else {
        console.log('  ❌ 工具详情页 - 工具名称未显示');
        ((failed++));
    }
    ((total++));
    
    if (detailText.includes('MCP') || detailText.includes('类别')) {
        console.log('  ✅ 工具详情页 - 分类显示');
        ((passed++));
    } else {
        console.log('  ❌ 工具详情页 - 分类未显示');
        ((failed++));
    }
    ((total++));
    
    const likeBtn = await page.locator('.like-btn').count();
    if (likeBtn > 0) {
        console.log('  ✅ 工具详情页 - 点赞按钮存在');
        ((passed++));
    } else {
        console.log('  ❌ 工具详情页 - 点赞按钮不存在');
        ((failed++));
    }
    ((total++));
    
    const textarea = await page.locator('.comment-editor textarea, textarea').count();
    if (textarea > 0) {
        console.log('  ✅ 工具详情页 - 评论输入框存在');
        ((passed++));
    } else {
        console.log('  ❌ 工具详情页 - 评论输入框不存在');
        ((failed++));
    }
    ((total++));
    
    // Test login page
    await page.goto('http://localhost:5174/login', { timeout: 30000 });
    await page.waitForLoadState('networkidle');
    const loginText = await page.textContent('body');
    
    if (loginText.includes('登录') || loginText.includes('login') || loginText.includes('登录')) {
        console.log('  ✅ 登录页面加载正常');
        ((passed++));
    } else {
        console.log('  ❌ 登录页面加载失败');
        ((failed++));
    }
    ((total++));
    
    // Test 404 page
    await page.goto('http://localhost:5174/nonexistent-page', { timeout: 30000 });
    await page.waitForTimeout(2000);
    const errorText = await page.textContent('body');
    
    if (errorText.includes('404') || errorText.includes('不存在')) {
        console.log('  ✅ 404错误页面正常');
        ((passed++));
    } else {
        console.log('  ❌ 404错误页面异常');
        ((failed++));
    }
    ((total++));
    
    await browser.close();
})();
" 2>&1

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 测试结果汇总"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "  📊 总测试项: $total"
echo "  ✅ 通过: $passed"
echo "  ❌ 失败: $failed"
echo ""

if [ $failed -eq 0 ]; then
    echo -e "  🎉 所有测试通过!"
    exit 0
else
    echo -e "  ⚠️  有 $failed 项测试失败"
    exit 1
fi
