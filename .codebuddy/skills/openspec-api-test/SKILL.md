---
name: openspec-api-test
description: OpenSpec工作流完成后手动触发，使用curl命令对spec描述的接口变动做自动化测试
---
# openspec-api-test

> **重要**: 这是一个**手动触发**的 skill，用于在开发任务和单元测试全部完成后，基于 OpenSpec change 的 `specs/**/*.md` 中描述的接口变动，使用 curl 命令做接口自动化测试验证。

## 何时使用

在以下条件满足后执行：
- ✅ 所有 TDD 任务已完成（RED/GREEN/REFACTOR）
- ✅ 后端实现任务已完成
- ✅ 单元测试全部通过（`cd backend && ./gradlew test`）
- ✅ 后端服务已启动（端口 8082，`make backend` 或 `./gradlew bootRun`）
- ✅ 数据库已初始化且包含测试数据

## 核心思路

```
┌─────────────────────────────────────────────────────────────┐
│  specs/**/*.md  ──解析──▶  Scenario 列表                      │
│       │                          │                            │
│       │                     映射为 curl 命令                   │
│       │                          │                            │
│       ▼                          ▼                            │
│  WHEN: HTTP方法+路径+角色   ──▶  curl -X METHOD URL \         │
│  THEN: 预期状态码+字段           -H "Authorization: Bearer ..."│
│                                  -d '{...}'                   │
│                                       │                       │
│                                       ▼                       │
│                              对比实际响应 vs 预期              │
│                                       │                       │
│                                       ▼                       │
│                              ✅ PASS / ❌ FAIL 报告           │
└─────────────────────────────────────────────────────────────┘
```

## 第一步：解析 spec 场景

读取当前 change 目录下的 `specs/**/*.md`，提取所有 `#### Scenario` 块。

每个 Scenario 的结构：
```markdown
#### Scenario: <名称>
- **WHEN** 角色为 ADMIN 的用户对他人创建的工具调用 `DELETE /api/v1/tools/{id}`
- **THEN** 工具状态标记为 DELETED，返回 200 成功响应
```

解析出以下要素：

| 要素 | 提取规则 | 示例 |
|------|----------|------|
| HTTP 方法 | WHEN 中反引号内的 `METHOD /path` | `DELETE` |
| 请求路径 | 同上，`{id}` 替换为实际 ID | `/api/v1/tools/1` |
| 调用角色 | WHEN 中"角色为 X 的用户"或"创建者"或"游客" | `ADMIN` |
| 请求体 | WHEN 中"传入新的 title"等描述 | `{"title":"新标题"}` |
| 预期状态码 | THEN 中"返回 XXX" | `200` |
| 预期字段 | THEN 中"状态标记为 DELETED"等 | 响应含 `DELETED` 或查库验证 |

**角色映射表**（根据项目实际账号配置）：

| spec 中的角色描述 | 账号 | 密码 | 用途 |
|-------------------|------|------|------|
| 超级管理员 / SUPER_ADMIN | `admin` | `Cloud@1234` | 系统初始化账号，可操作任意内容 |
| 管理员 / ADMIN | 测试前动态创建并审批 | — | 需先注册 ADMIN 角色，再由超管审批 |
| 创建者 / 普通用户 / USER | 测试前动态创建 | — | 内容的原始创建者 |
| 游客 / 未登录 | 无需登录 | — | 不带 Authorization 头 |

## 第二步：准备测试账号与数据

### 2.1 确保超管账号可用

```bash
# 超管登录获取 token
SUPER_ADMIN_TOKEN=$(curl -s -X POST http://localhost:8082/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Cloud@1234"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

echo "Super Admin Token: ${SUPER_ADMIN_TOKEN:0:20}..."
```

### 2.2 动态创建测试账号

```bash
# 创建普通用户 A（内容创建者）
curl -s -X POST http://localhost:8082/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test_owner","nickname":"测试创建者","password":"test123456"}'

# 创建普通用户 B（无权限用户）
curl -s -X POST http://localhost:8082/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test_user","nickname":"测试用户","password":"test123456"}'

# 创建管理员账号（注册后状态为 PENDING，需超管审批）
curl -s -X POST http://localhost:8082/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test_admin","nickname":"测试管理员","password":"test123456","role":"ADMIN"}'

# 超管审批管理员账号
ADMIN_USER_ID=$(curl -s http://localhost:8082/api/v1/admin/pending-users \
  -H "Authorization: Bearer $SUPER_ADMIN_TOKEN" \
  | python3 -c "import sys,json; users=json.load(sys.stdin)['data']; print([u['id'] for u in users if u['username']=='test_admin'][0])")

curl -s -X PUT "http://localhost:8082/api/v1/admin/approve/$ADMIN_USER_ID" \
  -H "Authorization: Bearer $SUPER_ADMIN_TOKEN"
```

### 2.3 获取各角色 token

```bash
get_token() {
  local username=$1
  local password=$2
  curl -s -X POST http://localhost:8082/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$username\",\"password\":\"$password\"}" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])"
}

OWNER_TOKEN=$(get_token "test_owner" "test123456")
USER_TOKEN=$(get_token "test_user" "test123456")
ADMIN_TOKEN=$(get_token "test_admin" "test123456")
SUPER_ADMIN_TOKEN=$(get_token "admin" "Cloud@1234")
```

### 2.4 创建测试内容

```bash
# 创建者创建测试工具
TOOL_ID=$(curl -s -X POST http://localhost:8082/api/v1/tools \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -d '{"name":"测试工具_API_TEST","categoryId":1,"content":"测试内容"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

# 创建者创建测试帖子
POST_ID=$(curl -s -X POST http://localhost:8082/api/forum/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -d '{"title":"测试帖子_API_TEST","content":"测试内容","categoryId":1}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

# 创建者创建测试微课（需视频文件，可跳过或用占位）
# VIDEO_ID=...
```

## 第三步：生成并执行 curl 测试

### curl 命令模板

```bash
# 通用测试函数
run_test() {
  local tc_id=$1
  local tc_name=$2
  local method=$3
  local url=$4
  local token=$5
  local body=$6
  local expect_code=$7
  local expect_contains=$8

  local auth_header=""
  if [ -n "$token" ] && [ "$token" != "null" ]; then
    auth_header="-H \"Authorization: Bearer $token\""
  fi

  local body_arg=""
  if [ -n "$body" ]; then
    body_arg="-H \"Content-Type: application/json\" -d '$body'"
  fi

  echo "── $tc_id: $tc_name ──"
  echo "  $method $url  expect=$expect_code"

  # 执行请求
  local response
  response=$(eval curl -s -w '"\\n%{http_code}"' -X "$method" "$url" $auth_header $body_arg)

  local http_code=$(echo "$response" | tail -1)
  local body_text=$(echo "$response" | sed '$d')

  # 断言状态码
  if [ "$http_code" == "$expect_code" ]; then
    local status="✅ PASS"
  else
    local status="❌ FAIL"
  fi

  # 断言响应包含（可选）
  if [ -n "$expect_contains" ] && ! echo "$body_text" | grep -q "$expect_contains"; then
    status="❌ FAIL"
  fi

  echo "  actual=$http_code  $status"
  if [ "$status" == "❌ FAIL" ]; then
    echo "  response: ${body_text:0:200}"
  fi
  echo ""
}
```

### 从 spec 场景到 curl 的映射示例

以 `specs/content-moderation/spec.md` 为例：

```bash
BASE="http://localhost:8082"

# Scenario: 管理员删除他人创建的工具
# WHEN: ADMIN 调用 DELETE /api/v1/tools/{id}
# THEN: 返回 200
run_test "TC-001" "管理员删除他人工具" \
  "DELETE" "$BASE/api/v1/tools/$TOOL_ID" \
  "$ADMIN_TOKEN" "" "200" ""

# Scenario: 普通用户无法删除他人内容
# WHEN: USER 调用 DELETE /api/v1/tools/{id}
# THEN: 返回 403
run_test "TC-002" "普通用户删除他人工具被拒" \
  "DELETE" "$BASE/api/v1/tools/$TOOL_ID" \
  "$USER_TOKEN" "" "403" ""

# Scenario: 创建者仍可删除自己的内容
run_test "TC-003" "创建者删除自己工具" \
  "DELETE" "$BASE/api/v1/tools/$TOOL_ID" \
  "$OWNER_TOKEN" "" "200" ""

# Scenario: 管理员编辑他人帖子
# WHEN: ADMIN 调用 PUT /api/forum/posts/{id}
# THEN: 返回 200
run_test "TC-004" "管理员编辑他人帖子" \
  "PUT" "$BASE/api/forum/posts/$POST_ID" \
  "$ADMIN_TOKEN" \
  '{"title":"管理员修改的标题","content":"修改后内容","categoryId":1}' \
  "200" ""

# Scenario: 普通用户无法编辑他人帖子
run_test "TC-005" "普通用户编辑他人帖子被拒" \
  "PUT" "$BASE/api/forum/posts/$POST_ID" \
  "$USER_TOKEN" \
  '{"title":"无权修改","content":"内容","categoryId":1}' \
  "403" ""
```

### spec 场景到 curl 的完整映射规则

| spec WHEN 中的模式 | curl 构造 |
|--------------------|-----------|
| `GET /path` | `curl -X GET $BASE/path` |
| `POST /path` + "包含 title、content" | `curl -X POST $BASE/path -H "Content-Type: application/json" -d '{"title":"...","content":"..."}'` |
| `PUT /path` + "更新 title" | `curl -X PUT $BASE/path -H "Content-Type: application/json" -d '{"title":"新标题"}'` |
| `DELETE /path` | `curl -X DELETE $BASE/path` |
| "角色为 ADMIN" | `-H "Authorization: Bearer $ADMIN_TOKEN"` |
| "角色为 USER" | `-H "Authorization: Bearer $USER_TOKEN"` |
| "创建者" | `-H "Authorization: Bearer $OWNER_TOKEN"` |
| "游客" / "未登录" | 无 Authorization 头 |
| `{id}` 占位符 | 替换为第二步创建的测试内容 ID |

| spec THEN 中的模式 | 断言 |
|--------------------|------|
| "返回 200" / "返回 201" / "返回 204" | HTTP 状态码 == 预期值 |
| "返回 403 Forbidden" | HTTP 状态码 == 403 |
| "返回 404" | HTTP 状态码 == 404 |
| "状态标记为 DELETED" | 查库验证或 GET 请求返回 404 |
| "返回帖子列表" | 响应体包含 `content` 数组 |
| "返回 201，authorId 为当前用户" | 状态码 201 + 响应含 `authorId` |

## 第四步：执行测试并生成报告

将所有测试写入一个 shell 脚本执行，输出报告：

```bash
#!/bin/bash
# api-test.sh — 基于 spec 的接口自动化测试

BASE="http://localhost:8082"
PASS=0
FAIL=0
RESULTS=""

run_test() {
  # ... (见第三步的函数实现)
  if [ "$status" == "✅ PASS" ]; then
    PASS=$((PASS+1))
  else
    FAIL=$((FAIL+1))
  fi
  RESULTS="$RESULTS\n| $tc_id | $tc_name | $status | expect=$expect_code actual=$http_code |"
}

# === 测试用例（从 spec 自动映射）===
# ... 所有 run_test 调用 ...

# === 汇总 ===
echo ""
echo "=============================="
echo "  API Test Summary"
echo "  Passed: $PASS"
echo "  Failed: $FAIL"
echo "  Total:  $((PASS+FAIL))"
echo "=============================="
echo ""
echo "| TC ID | Test Case | Status | Notes |"
echo "|-------|-----------|--------|-------|"
echo -e "$RESULTS"
```

## 输出产物

测试完成后生成报告，保存到 `openspec/changes/<change-name>/api-test-report.md`：

```markdown
# API Test Report: <change-name>

## 测试环境
- 后端地址: http://localhost:8082
- 测试时间: YYYY-MM-DD HH:MM
- 测试账号: test_owner(USER), test_user(USER), test_admin(ADMIN), admin(SUPER_ADMIN)

## 测试结果

| TC ID | Test Case | Status | Notes |
|-------|-----------|--------|-------|
| TC-001 | 管理员删除他人工具 | ✅ PASS | expect=200 actual=200 |
| TC-002 | 普通用户删除他人工具被拒 | ✅ PASS | expect=403 actual=403 |
| TC-003 | 创建者删除自己工具 | ✅ PASS | expect=200 actual=200 |
| TC-004 | 管理员编辑他人帖子 | ✅ PASS | expect=200 actual=200 |
| TC-005 | 普通用户编辑他人帖子被拒 | ❌ FAIL | expect=403 actual=200 |

## 失败用例详情

### TC-005: 普通用户编辑他人帖子被拒
- 请求: PUT /api/forum/posts/3
- Token: test_user (USER)
- Body: {"title":"无权修改","content":"内容","categoryId":1}
- 预期: 403 Forbidden
- 实际: 200
- 响应体: {"code":200,"message":"更新成功","data":{...}}
- 分析: 后端权限校验未拦截普通用户，需检查 ForumPostService.updatePost 的 isAdmin 判断

## 总结

**Overall: 4/5 Passed**

失败用例表明后端权限校验存在缺陷，需修复后重新测试。
```

## 执行流程总结

```
1. 读取 specs/**/*.md
   └─ 提取所有 #### Scenario 块
   └─ 解析 WHEN(HTTP方法/路径/角色/请求体) + THEN(预期状态码/字段)

2. 准备测试环境
   └─ 超管登录 → 审批测试管理员 → 创建测试用户
   └─ 创建者创建测试内容(工具/帖子/微课) → 记录 ID

3. 生成 curl 测试脚本
   └─ 每个 Scenario → 一个 run_test 调用
   └─ 角色映射到对应 token
   └─ {id} 替换为测试内容 ID

4. 执行测试
   └─ 运行脚本，捕获每个请求的 HTTP 状态码和响应体
   └─ 对比预期，标记 PASS/FAIL

5. 生成报告
   └─ 保存到 openspec/changes/<change-name>/api-test-report.md
   └─ 失败用例附请求/响应详情和初步分析
```

## 特别的要求

- 遇到问题一定要自动修复，不要停下来问我
- 修复问题后自动重启服务验证问题是否修复
- 测试数据用完要清理（删除测试创建的工具/帖子/微课和测试账号），避免污染数据库
- 如果 spec 场景涉及"查库验证"（如"状态标记为 DELETED"），可通过再次 GET 该资源预期 404 来间接验证
- 微课测试如需视频文件，可用一个最小 mp4 占位文件；若上传复杂可跳过微课创建相关用例并在报告中注明
- token 过期（15分钟）时自动重新登录获取新 token
