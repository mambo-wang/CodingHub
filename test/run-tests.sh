#!/bin/bash

echo "=========================================="
echo "AI工具广场 - 自动化测试"
echo "=========================================="

BASE_URL="http://localhost:8081/api/v1"
FRONTEND_URL="http://localhost:5174"

echo -e "\n[1/6] 测试工具详情页API (未登录状态)..."
echo "----------------------------------------"

# Test 1: Get like status (public endpoint)
echo "Testing: GET /tools/{id}/like-status"
RESPONSE=$(curl -s "$BASE_URL/tools/1/like-status")
echo "Response: $RESPONSE"
if echo "$RESPONSE" | grep -q '"code":200'; then
    echo "✅ PASS - like-status API正常工作"
else
    echo "❌ FAIL - like-status API返回错误"
fi

# Test 2: Get comments (public endpoint)
echo -e "\nTesting: GET /tools/{id}/comments"
RESPONSE=$(curl -s "$BASE_URL/tools/1/comments")
echo "Response: $RESPONSE"
if echo "$RESPONSE" | grep -q '"code":200'; then
    echo "✅ PASS - comments API正常工作"
else
    echo "❌ FAIL - comments API返回错误"
fi

# Test 3: Get tool detail
echo -e "\nTesting: GET /tools/{id}"
RESPONSE=$(curl -s "$BASE_URL/tools/1")
echo "Response: $RESPONSE"
if echo "$RESPONSE" | grep -q '"code":200'; then
    echo "✅ PASS - tool detail API正常工作"
else
    echo "❌ FAIL - tool detail API返回错误"
fi

echo -e "\n\n[2/6] 检查测试用户..."
echo "----------------------------------------"
# Check if there's a user we can use for authenticated tests
echo "尝试登录获取token..."
LOGIN_RESP=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}')
echo "Login response: $LOGIN_RESP"

if echo "$LOGIN_RESP" | grep -q '"accessToken"'; then
    TOKEN=$(echo "$LOGIN_RESP" | grep -o '"accessToken":"[^"]*"' | sed 's/"accessToken":"//;s/"//')
    echo "✅ 获得测试token: ${TOKEN:0:20}..."
    
    # Test 4: Like a tool (authenticated)
    echo -e "\nTesting: POST /tools/{id}/like (authenticated)"
    LIKE_RESP=$(curl -s -X POST "$BASE_URL/tools/1/like" \
      -H "Authorization: Bearer $TOKEN")
    echo "Response: $LIKE_RESP"
    if echo "$LIKE_RESP" | grep -q '"code":200'; then
        echo "✅ PASS - like API (authenticated) 正常工作"
    else
        echo "❌ FAIL - like API 返回错误"
    fi
    
    # Test 5: Add comment (authenticated)
    echo -e "\nTesting: POST /tools/{id}/comments (authenticated)"
    COMMENT_RESP=$(curl -s -X POST "$BASE_URL/tools/1/comments" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -d '{"content":"自动化测试评论 - 测试点赞和评论功能"}')
    echo "Response: $COMMENT_RESP"
    if echo "$COMMENT_RESP" | grep -q '"code":201'; then
        echo "✅ PASS - comment API (authenticated) 正常工作"
    else
        echo "❌ FAIL - comment API 返回错误"
    fi
else
    echo "⚠️  无法获取测试token，跳过认证测试"
fi

echo -e "\n\n[3/6] 验证评论和点赞数据更新..."
echo "----------------------------------------"
RESPONSE=$(curl -s "$BASE_URL/tools/1/comments")
echo "Comments: $RESPONSE"
COMMENT_COUNT=$(echo "$RESPONSE" | grep -o '"id"' | wc -l | tr -d ' ')
echo "评论数量: $COMMENT_COUNT"
if [ "$COMMENT_COUNT" -gt 0 ]; then
    echo "✅ PASS - 可以获取评论列表"
else
    echo "⚠️  暂无评论数据（可能是未登录或用户无评论）"
fi

echo -e "\n\n=========================================="
echo "API测试完成!"
echo "=========================================="
