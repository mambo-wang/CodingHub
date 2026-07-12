# CodingHub full-feature E2E - Stage A2: protected + admin pages (TC-301..TC-402)
# Pure ASCII. Appends to e2e-report.md (run e2e-pages.ps1 first).
# Run: powershell -ExecutionPolicy Bypass -File scripts/e2e-pages2.ps1
param()
$ErrorActionPreference = 'Continue'

function Get-OpenCliBase {
    if (Get-Command opencli -ErrorAction SilentlyContinue) { return @('opencli') }
    $p = Join-Path $env:USERPROFILE ".opencli\node_modules\@jackwener\opencli\dist\src\main.js"
    if (Test-Path $p) { return @('node', $p) }
    return $null
}
$OC_BASE = Get-OpenCliBase
if (-not $OC_BASE) { Write-Host "ERROR: opencli not found"; exit 2 }

$SESSION = "e2eA2_$(Get-Date -Format HHmmss)"
$BASE    = "http://localhost:5173"
$TOOL_ID = 1
$REPORT  = Join-Path $PSScriptRoot "e2e-report.md"

$PASS = 0; $FAIL = 0; $WARN = 0

function oc {
    param([string[]]$rest)
    $all = $OC_BASE + @('browser', $SESSION) + $rest
    try { & $all[0] $all[1..($all.Length - 1)] 2>&1 | Out-String -Width 4000 }
    catch { return "[oc-error] $_" }
}
function Nav($path) { oc @('open', "$BASE$path") | Out-Null; Start-Sleep -Milliseconds 3500 }
function FindSel($sel) {
    if ($sel -notmatch '^[.#\[]') { $sel = '.' + $sel }
    $o = oc @('find', '--css', $sel, '--limit', '3')
    return ($o | Select-String -Pattern '"matches_n":\s*[1-9]')
}
function Get-Url { (oc @('get', 'url') | Select-String -Pattern 'http[^"]*').Matches.Value }

function Report($st, $id, $desc, $note) {
    switch ($st) { 'PASS' { $script:PASS++ } 'FAIL' { $script:FAIL++ } default { $script:WARN++ } }
    Write-Host "[$st] $id $desc$(if ($note) { " -- $note" })"
    Add-Content -Path $REPORT -Encoding utf8 -Value "| $st | $id | $st | $desc | $(if ($note) { $note }) |"
}
function AssertSel($sel, $id, $desc) {
    try {
        if (FindSel $sel) { Report PASS $id $desc }
        else { Start-Sleep -Milliseconds 2000; if (FindSel $sel) { Report PASS $id $desc } else { Report FAIL $id $desc "selector [$sel] not found" } }
    } catch { Report FAIL $id $desc "exception: $_" }
}
function Test-ProtectedPage($path, $root, $id, $desc) {
    try {
        Nav $path
        $u = Get-Url
        if ($u -like "*/login*") { Report WARN $id $desc "auth redirect to /login (URL=$u) - not logged in this session"; return }
        AssertSel $root $id $desc
    } catch { Report FAIL $id $desc "exception: $_" }
}
function Test-PublicPage($path, $root, $id, $desc) {
    try {
        Nav $path
        AssertSel $root $id $desc
    } catch { Report FAIL $id $desc "exception: $_" }
}
function Test-AdminPage($path, $root, $id, $desc) {
    try {
        Nav $path
        $u = Get-Url
        if ($u -notlike "*/admin/*") {
            Report WARN $id $desc "RBAC redirect (URL=$u) - current user lacks required role"; return
        }
        AssertSel $root $id $desc
    } catch { Report FAIL $id $desc "exception: $_" }
}

Write-Host "=== Stage A2: protected + admin pages ==="
Test-ProtectedPage "/tools/upload"            "upload-page"            "TC-301" "Upload tool page"
Test-ProtectedPage "/me/profile"             "profile-page"          "TC-302" "Profile page"
Test-ProtectedPage "/me/tools/$TOOL_ID/edit" "edit-page"             "TC-303" "Edit tool page"
Test-ProtectedPage "/forum/editor"           "post-editor-page"      "TC-304" "New post editor"
Test-ProtectedPage "/forum/my-posts"         "my-posts-page"         "TC-305" "My posts page"
Test-ProtectedPage "/forum/my-favorites"     "my-favorites-page"     "TC-306" "My favorites page"
Test-ProtectedPage "/videos/upload"          "upload-page"            "TC-307" "Upload video page"
Test-ProtectedPage "/videos/my-videos"      "my-videos-page"        "TC-308" "My videos page"
Test-ProtectedPage "/videos/my-favorites"    "my-video-favorites-page" "TC-309" "My video favorites page"
Test-ProtectedPage "/knowledge/my"           "knowledge-list-page"   "TC-310" "My knowledge page"
Test-ProtectedPage "/knowledge/create"       "knowledge-editor-page" "TC-311" "Create knowledge page"
Test-PublicPage     "/login"                  "auth-page"             "TC-312" "Login page"
Test-PublicPage     "/register"               "auth-page"             "TC-313" "Register page"
Test-AdminPage "/admin/approvals" "admin-page" "TC-401" "Approval management page"
Test-AdminPage "/admin/users"     "admin-page" "TC-402" "User management page"

$TOTAL = $PASS + $FAIL + $WARN
Write-Host "--- A2 partial: PASS $PASS | FAIL $FAIL | WARN $WARN ---"
Write-Host "STAGE_A2_DONE"
