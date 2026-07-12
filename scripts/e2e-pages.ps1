# CodingHub full-feature E2E - Stage A1: public + detail pages (TC-101..TC-202)
# Pure ASCII. opencli drives a real browser. Report written incrementaly.
# Run: powershell -ExecutionPolicy Bypass -File scripts/e2e-pages.ps1
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

$SESSION = "e2eA1_$(Get-Date -Format HHmmss)"
$BASE    = "http://localhost:5173"
$TOOL_ID = 1
$REPORT  = Join-Path $PSScriptRoot "e2e-report.md"

$PASS = 0; $FAIL = 0; $WARN = 0
Set-Content -Path $REPORT -Encoding utf8 -Value @(
    "# CodingHub Full-Feature E2E Report",
    "",
    "> Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')   Frontend: $BASE   Session: $SESSION",
    "",
    "| Result | Case | Status | Description | Note |",
    "|--------|------|--------|-------------|------|"
)

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
function Test-PublicPage($path, $root, $id, $desc) {
    try {
        Nav $path
        $u = Get-Url
        if ($u -like "*/login*") { Report FAIL $id $desc "unexpected redirect to /login (URL=$u)"; return }
        AssertSel $root $id $desc
    } catch { Report FAIL $id $desc "exception: $_" }
}

Write-Host "=== Stage A1: public + detail pages ==="
Test-PublicPage "/"                "home-page"            "TC-101" "Home / Tool Plaza"
Test-PublicPage "/tools/$TOOL_ID" "detail-page"          "TC-102" "Tool detail page"
Test-PublicPage "/forum"           "post-list-page"       "TC-103" "Forum list page"
Test-PublicPage "/videos"          "video-list-page"      "TC-104" "Video list page"
Test-PublicPage "/overview"        "overview-page"        "TC-105" "Overview / Hot ranking"
Test-PublicPage "/quickstart"      "quickstart-page"      "TC-106" "Quick start page"
Test-PublicPage "/about"           "about-page"           "TC-107" "About page"
Test-PublicPage "/feedback"        "feedback-page"        "TC-108" "Feedback page"
Test-PublicPage "/knowledge"       "knowledge-list-page"  "TC-109" "Knowledge list page"
Nav "/knowledge/1"
Report WARN "TC-110" "Knowledge detail page" "no seed KB data; detail may render empty/error state"
Test-PublicPage "/this-route-does-not-exist" "not-found-page" "TC-111" "404 NotFound page"
Nav "/forum/posts/1"
Report WARN "TC-201" "Forum post detail page" "no seed post data; detail may render empty/error state"
Nav "/videos/1"
Report WARN "TC-202" "Video detail page" "no seed video data; detail may render empty/error state"

Write-Host "--- A1 partial: PASS $PASS | FAIL $FAIL | WARN $WARN ---"
Write-Host "STAGE_A1_DONE"
