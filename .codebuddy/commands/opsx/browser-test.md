---
name: OPSX: Browser Test
description: "Execute browser E2E tests using opencli-browser after implementation is complete"
argument-hint: "[change-name]"
---

Execute browser-based E2E tests for an OpenSpec change after implementation is complete.

**Prerequisites**:
- All TDD tasks completed (RED/GREEN/REFACTOR)
- UI implementation tasks completed (if any)
- Unit tests passing
- Dev server running

**Steps**:

1. **Check opencli environment**
   ```bash
   opencli doctor
   ```
   If doctor fails, fix the environment before proceeding.

2. **Load the browser test skill**
   ```
   /openspec-browser-test
   ```

3. **Determine what to test**

   If a change name is provided:
   ```bash
   openspec status --change "<name>" --json
   ```
   Check which page(s)/feature(s) need testing.

   If no change name provided, ask user to select or specify the target URL.

4. **Execute tests**

   Open the page and perform verification:
   ```bash
   # Open the page
   opencli browser test open "<url>"

   # Wait for content to load
   opencli browser test wait selector "<selector>" --timeout 15000

   # Get page state
   opencli browser test state

   # Verify key elements exist
   opencli browser test find --css "<selector>"

   # Take screenshot if needed
   opencli browser test screenshot [<path>]

   # Close session when done
   opencli browser test close
   ```

5. **Record results**

   Document test outcomes:
   ```
   ## Browser Test Results: <change-name>

   | Test Case | Status | Notes |
   |----------|--------|-------|
   | TC-001   | ✅ PASS / ❌ FAIL | |
   | TC-002   | ✅ PASS / ❌ FAIL | |

   **Overall:** X/Y Passed
   ```

**Output**:

Summarize test execution:
- Tests executed
- Pass/fail count
- Any failures with evidence (screenshots, error messages)
- Recommendation: Ready for archive / Fix issues first