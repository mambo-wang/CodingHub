---
name: OPSX: API Test
description: "Execute curl-based API tests against spec scenarios after implementation is complete"
argument-hint: "[change-name]"
---

Execute curl-based API automation tests for an OpenSpec change after implementation is complete.

**Prerequisites**:
- All TDD tasks completed (RED/GREEN/REFACTOR)
- Backend implementation tasks completed
- Unit tests passing (`cd backend && ./gradlew test`)
- Backend server running on port 8082 (`make backend` or `./gradlew bootRun`)
- Database initialized with test data

**Input**: Optionally specify a change name after `/opsx:api-test` (e.g., `/opsx:api-test add-content-moderation`). If omitted, check if it can be inferred from conversation context. If vague or ambiguous you MUST prompt for available changes.

**Steps**:

1. **Determine what to test**

   If a change name is provided:
   ```bash
   openspec status --change "<name>" --json
   ```
   Check which spec files exist and what API changes need testing.

   If no change name provided, run `openspec list --json` to get available changes. Use the **AskUserQuestion tool** to let the user select.

   **IMPORTANT**: Do NOT guess or auto-select a change. Always let the user choose.

2. **Load the API test skill**
   ```
   /openspec-api-test
   ```

3. **Verify backend is running**
   ```bash
   curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/api/v1/tools
   ```
   If not reachable, start the backend:
   ```bash
   cd backend && ./gradlew bootRun &
   ```
   Wait for startup to complete before proceeding.

4. **Parse spec scenarios**

   Read all `specs/**/*.md` files in the change directory. Extract every `#### Scenario` block:
   - **WHEN**: HTTP method, path, calling role, request body
   - **THEN**: Expected status code, expected response fields

   Map spec roles to test accounts:
   | Spec role | Account | How to obtain token |
   |-----------|---------|---------------------|
   | SUPER_ADMIN | `admin` / `Cloud@1234` | Login via `/api/v1/auth/login` |
   | ADMIN | Dynamic test account | Register + super-admin approve + login |
   | USER / Creator | Dynamic test account | Register + login |
   | Guest | None | No Authorization header |

5. **Prepare test environment**

   ```bash
   # Super admin login
   SUPER_ADMIN_TOKEN=$(curl -s -X POST http://localhost:8082/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"Cloud@1234"}' \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

   # Create test users (owner, regular user, admin)
   # Approve admin via super admin
   # Login each role to get tokens
   ```

6. **Create test content**

   Use the creator token to create test resources (tools, posts, videos) needed by the scenarios. Record their IDs for use in subsequent tests.

   ```bash
   # Example: creator creates a test tool
   TOOL_ID=$(curl -s -X POST http://localhost:8082/api/v1/tools \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $OWNER_TOKEN" \
     -d '{"name":"API_TEST_tool","categoryId":1,"content":"test"}' \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
   ```

7. **Execute curl tests**

   For each parsed scenario, construct and run a curl command:

   ```bash
   run_test() {
     local tc_id=$1 tc_name=$2 method=$3 url=$4 token=$5 body=$6 expect_code=$7
     # Build auth header from token
     # Build body arg if provided
     # Execute curl, capture HTTP code + response
     # Compare actual code vs expect_code
     # Output ✅ PASS / ❌ FAIL
   }

   # Example: Admin deletes another user's tool (expect 200)
   run_test "TC-001" "Admin deletes others tool" \
     "DELETE" "http://localhost:8082/api/v1/tools/$TOOL_ID" \
     "$ADMIN_TOKEN" "" "200"

   # Example: Regular user cannot delete others tool (expect 403)
   run_test "TC-002" "User delete others tool denied" \
     "DELETE" "http://localhost:8082/api/v1/tools/$TOOL_ID" \
     "$USER_TOKEN" "" "403"
   ```

8. **Auto-fix on failure**

   If a test fails:
   - Read the response body to understand the error
   - Inspect the relevant backend Service/Controller code
   - Fix the permission check or API logic
   - Restart backend: `cd backend && ./gradlew bootRun &`
   - Re-run the failed test to verify the fix

   **Do not stop and ask** — fix and retry automatically.

9. **Clean up test data**

   After all tests pass, delete test resources and accounts created during the run to avoid polluting the database:
   ```bash
   # Delete test tools/posts/videos created
   # Delete test user accounts (via super admin)
   ```

10. **Record results**

    Generate a report and save to `openspec/changes/<change-name>/api-test-report.md`:

    ```markdown
    # API Test Report: <change-name>

    ## Test Environment
    - Backend: http://localhost:8082
    - Time: YYYY-MM-DD HH:MM
    - Accounts: test_owner(USER), test_user(USER), test_admin(ADMIN), admin(SUPER_ADMIN)

    ## Test Results

    | TC ID | Test Case | Status | Notes |
    |-------|-----------|--------|-------|
    | TC-001 | Admin deletes others tool | ✅ PASS | expect=200 actual=200 |
    | TC-002 | User delete others tool denied | ✅ PASS | expect=403 actual=403 |
    | TC-003 | Creator deletes own tool | ✅ PASS | expect=200 actual=200 |

    ## Failed Cases Detail
    (if any, with request/response/analysis)

    ## Summary
    **Overall: X/Y Passed**
    ```

**Output**:

Summarize test execution:
- Total scenarios tested (from spec)
- Pass/fail count
- Any failures with request details and response analysis
- Auto-fixes applied (if any)
- Recommendation: Ready for archive / Fix issues first
