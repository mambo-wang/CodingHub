#!/bin/bash

echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║           AI工具广场 - 论坛模块全功能测试报告                        ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""
echo "📅 测试时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

passed=0
failed=0
total=0

test_result() {
    local name="$1"
    local result="$2"
    local info="$3"
    ((total++))
    if [ "$result" = "PASS" ]; then
        ((passed++))
        echo -e "  ✅ $name"
    else
        ((failed++))
        echo -e "  ❌ $name: $info"
    fi
}

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📋 论坛帖子功能测试"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 1. Get forum posts (public)
echo "  [帖子列表]"
resp=$(curl -s "http://localhost:8080/api/forum/posts")
echo "$resp" | grep -q '"content"' && test_result "GET /api/forum/posts (帖子列表)" "PASS" || test_result "GET /api/forum/posts" "FAIL" "响应异常"
echo "$resp" | grep -q '"title"' && test_result "帖子包含标题" "PASS" || test_result "帖子包含标题" "FAIL" ""

echo ""
echo "  [帖子详情]"
post_id=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['content'][0]['id'])" 2>/dev/null)
if [ -n "$post_id" ]; then
    detail=$(curl -s "http://localhost:8080/api/forum/posts/$post_id")
    echo "$detail" | grep -q '"title"' && test_result "GET /api/forum/posts/$post_id (帖子详情)" "PASS" || test_result "帖子详情" "FAIL" ""
else
    test_result "帖子详情" "FAIL" "无帖子数据"
fi

echo ""
echo "  [帖子搜索]"
search=$(curl -s "http://localhost:8080/api/forum/posts?keyword=test")
echo "$search" | grep -q '"content"' && test_result "GET /api/forum/posts?keyword=test (搜索)" "PASS" || test_result "搜索功能" "FAIL" ""

echo ""
echo "  [按分类筛选]"
category=$(curl -s "http://localhost:8080/api/forum/posts?category=1")
echo "$category" | grep -q '"content"' && test_result "GET /api/forum/posts?category=1 (分类筛选)" "PASS" || test_result "分类筛选" "FAIL" ""

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📂 论坛分类测试"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "  [分类列表]"
resp=$(curl -s "http://localhost:8080/api/forum/categories")
echo "$resp" | grep -q '"id"' && test_result "GET /api/forum/categories (分类列表)" "PASS" || test_result "分类列表" "FAIL" ""
echo "$resp" | grep -q '"name"' && test_result "分类包含名称" "PASS" || test_result "分类名称" "FAIL" ""

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🏷️ 论坛标签测试"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "  [标签列表]"
resp=$(curl -s "http://localhost:8080/api/forum/tags")
echo "$resp" | grep -q '\[' && test_result "GET /api/forum/tags (标签列表)" "PASS" || test_result "标签列表" "FAIL" ""

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔐 认证操作测试 (需登录)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Register and login for authenticated tests
echo "  [注册测试用户]"
REG_RESP=$(curl -s -X POST "http://localhost:8080/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"forum_test_user","password":"Test123456","email":"forum@test.com"}')
echo "$REG_RESP" | grep -q '"accessToken"' && test_result "POST /auth/register (注册)" "PASS" || test_result "注册" "FAIL" ""

# Get token
TOKEN=$(echo "$REG_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null)

if [ -n "$TOKEN" ]; then
    echo ""
    echo "  [创建帖子]"
    CREATE_RESP=$(curl -s -X POST "http://localhost:8080/api/forum/posts" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -d '{"title":"自动化测试帖子","content":"这是通过自动化测试创建的帖子内容。\n\n- 测试点赞功能\n- 测试评论功能","categoryId":1}')
    echo "$CREATE_RESP" | grep -q '"id"' && test_result "POST /api/forum/posts (创建帖子)" "PASS" || test_result "创建帖子" "FAIL" ""
    
    # Get created post ID
    CREATED_ID=$(echo "$CREATE_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
    
    echo ""
    echo "  [点赞帖子]"
    LIKE_RESP=$(curl -s -X POST "http://localhost:8080/api/forum/likes" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -d "{\"postId\":$CREATED_ID}")
    echo "$LIKE_RESP" | grep -q '"code":200\|"code":201\|true" && test_result "POST /api/forum/likes (点赞)" "PASS" || test_result "点赞帖子" "FAIL" ""
    
    echo ""
    echo "  [取消点赞]"
    UNLIKE_RESP=$(curl -s -X DELETE "http://localhost:8080/api/forum/likes?postId=$CREATED_ID" \
      -H "Authorization: Bearer $TOKEN")
    echo "$UNLIKE_RESP" | grep -q '"code":200\|"code":204\|true" && test_result "DELETE /api/forum/likes (取消点赞)" "PASS" || test_result "取消点赞" "FAIL" ""
    
    echo ""
    echo "  [发表评论]"
    COMMENT_RESP=$(curl -s -X POST "http://localhost:8080/api/forum/posts/$CREATED_ID/comments" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -d '{"content":"这是自动化测试的评论内容。"}')
    echo "$COMMENT_RESP" | grep -q '"code":200\|"code":201\|"id"' && test_result "POST /posts/{id}/comments (评论)" "PASS" || test_result "发表评论" "FAIL" ""
    
    echo ""
    echo "  [获取我的帖子]"
    MY_POSTS=$(curl -s "http://localhost:8081/api/forum/posts/my" \
      -H "Authorization: Bearer $TOKEN")
    echo "$MY_POSTS" | grep -q '"content"' && test_result "GET /api/forum/posts/my (我的帖子)" "PASS" || test_result "我的帖子" "FAIL" ""
fi

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
    echo -e "  🎉 所有论坛功能测试通过!"
    exit 0
else
    echo -e "  ⚠️  有 $failed 项测试失败"
    exit 1
fi
