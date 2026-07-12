# CodingHub Full-Feature E2E Report

> Frontend: http://localhost:5173 (Vite dev)  |  Backend: http://localhost:8082
> Generated: 2026-07-12  |  Harness: opencli browser (real Chromium via Browser Bridge)
> Account: wangbao (logged in, role USER)

## Summary

| Metric | Count |
|--------|-------|
| Total cases | 34 |
| PASS | 29 |
| FAIL | 0 |
| WARN | 5 |

WARN items are expected / non-blocking:
- TC-110 / TC-201 / TC-202: detail pages for Knowledge / Forum-post / Video render an empty state because the dev database has no seed data for those modules (0 KB, 0 posts, 0 videos).
- TC-401 / TC-402: admin pages redirect to Home because the test account (wangbao) is not SUPER_ADMIN / ADMIN. This is correct RBAC behavior, not a defect.

Recommendation: **Ready for archive** — no functional regressions detected. One real defect was found and fixed during this run (see Notes).

## Results

| Result | Case | Status | Description | Note |
|--------|------|--------|-------------|------|
| PASS | TC-101 | PASS | Home / Tool Plaza |  |
| PASS | TC-102 | PASS | Tool detail page |  |
| PASS | TC-103 | PASS | Forum list page |  |
| PASS | TC-104 | PASS | Video list page |  |
| PASS | TC-105 | PASS | Overview / Hot ranking |  |
| PASS | TC-106 | PASS | Quick start page |  |
| PASS | TC-107 | PASS | About page |  |
| PASS | TC-108 | PASS | Feedback page |  |
| PASS | TC-109 | PASS | Knowledge list page |  |
| WARN | TC-110 | WARN | Knowledge detail page | no seed KB data; empty/error state expected |
| PASS | TC-111 | PASS | 404 NotFound page |  |
| WARN | TC-201 | WARN | Forum post detail page | no seed post data; empty/error state expected |
| WARN | TC-202 | WARN | Video detail page | no seed video data; empty/error state expected |
| PASS | TC-301 | PASS | Upload tool page |  |
| PASS | TC-302 | PASS | Profile page |  |
| PASS | TC-303 | PASS | Edit tool page |  |
| PASS | TC-304 | PASS | New post editor |  |
| PASS | TC-305 | PASS | My posts page |  |
| PASS | TC-306 | PASS | My favorites page |  |
| PASS | TC-307 | PASS | Upload video page |  |
| PASS | TC-308 | PASS | My videos page |  |
| PASS | TC-309 | PASS | My video favorites page |  |
| PASS | TC-310 | PASS | My knowledge page |  |
| PASS | TC-311 | PASS | Create knowledge page |  |
| PASS | TC-312 | PASS | Login page |  |
| PASS | TC-313 | PASS | Register page |  |
| WARN | TC-401 | WARN | Approval management page | RBAC redirect (URL=/) - user lacks SUPER_ADMIN |
| WARN | TC-402 | WARN | User management page | RBAC redirect (URL=/) - user lacks ADMIN |
| PASS | TC-501 | PASS | Theme toggle (dark/light) | data-theme flag flipped |
| PASS | TC-502 | PASS | Tool detail like button | liked state toggled |
| PASS | TC-503 | PASS | Tool detail favorite button | favorited state toggled |
| PASS | TC-504 | PASS | Submit comment | comment posted and listed |
| PASS | TC-505 | PASS | Home tool-card navigation | click card -> /tools/1 |
| PASS | TC-506 | PASS | Home search input | v-model receives typed value |

## Notes

- **Defect fixed (real bug):** `frontend/src/pages/knowledge/KnowledgeEditorPage.vue` had an unclosed `<div class="advanced-content">` (the closing `</div>` before `</details>` was missing). This caused a Vite/vue template compile error and a white-screen (only the header rendered, no `knowledge-editor-page` root) for BOTH the Create and Edit knowledge pages. Fixed by adding the missing `</div>`. Verified: `/knowledge/create` now renders the full form (TC-311 PASS).
- **API prefix reminder (not a bug):** frontend modules use different base URLs — tools/videos/knowledge use `/api/v1`, while forum and overview use `/api`. There is no `/api/v1/forum/*` or `/api/v1/overview` route; those 500s only appear if the wrong prefix is used by a caller. Confirmed working per the correct prefixes.
- **Harness notes:** opencli must be invoked via `node <install>/.opencli/.../main.js` (not on PATH on this machine). Browser Bridge extension must be connected (opencli doctor). Each run uses a timestamped session to avoid stale-tab state. PowerShell scripts are pure ASCII (CSS selectors only) to avoid parse errors; the `.` prefix is auto-added to bare class names.

## How to re-run

```powershell
cd d:\repos\CodingHub
powershell -ExecutionPolicy Bypass -File scripts\e2e-pages.ps1          # Stage A1: public + detail pages
powershell -ExecutionPolicy Bypass -File scripts\e2e-pages2.ps1         # Stage A2: protected + admin pages
powershell -ExecutionPolicy Bypass -File scripts\e2e-interactions.ps1     # Stage B: interactions
# (A1 resets scripts/e2e-report.md; A2 and B append to it)
```
