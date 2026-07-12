# CodingHub full-feature E2E - Stage B: interactions (TC-501..TC-506)
# Pure ASCII. Appends rows to e2e-report.md (run e2e-pages.ps1 first).
# Run: powershell -ExecutionPolicy Bypass -File scripts/e2e-interactions.ps1
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

$SESSION = "e2eB_$(Get-Date -Format HHmmss)"
$BASE    = "http://localhost:5173"
$TOOL_ID = 1
$REPORT  = Join-Path $PSScriptRoot "e2e-report.md"

$PASS = 0; $FAIL = 0; $WARN = 0

function oc {
    param([string[]]$rest)
    $all = $OC_BASE + @('browser', $SESSION) + $rest
    try { & $all[0] $all[1..($all.Length - 1)] 2>&1 | Out-String -Width 6000 }
    catch { return "[oc-error] $_" }
}
function Nav($path) { oc @('open', "$BASE$path") | Out-Null; Start-Sleep -Milliseconds 4000 }
function CountSel($sel) {
    if ($sel -notmatch '^[.#\[]') { $sel = '.' + $sel }
    $o = oc @('find', '--css', $sel, '--limit', '30')
    $m = ($o | Select-String -Pattern '"matches_n":\s*(\d+)')
    if ($m) { return [int]$m.Matches.Groups[1].Value } else { return 0 }
}
function ClickSel($sel, $nth) {
    if ($nth -ne $null) { oc @('click', '--nth', $nth, $sel) | Out-Null }
    else { oc @('click', $sel) | Out-Null }
}
function TypeSel($sel, $text) { oc @('type', $sel, $text) | Out-Null }
function Get-Url { (oc @('get', 'url') | Select-String -Pattern 'http[^"\s]*').Matches.Value }
function GetText($sel) {
    if ($sel -notmatch '^[.#\[]') { $sel = '.' + $sel }
    $o = oc @('get', 'text', '--css', $sel)
    $m = ($o | Select-String -Pattern '"value":\s*"([^"]*)"')
    if ($m) { return $m.Matches.Groups[1].Value } else { return '' }
}
function GetValue($sel) {
    if ($sel -notmatch '^[.#\[]') { $sel = '.' + $sel }
    $o = oc @('get', 'value', $sel)
    $m = ($o | Select-String -Pattern '"value":\s*"([^"]*)"')
    if ($m) { return $m.Matches.Groups[1].Value } else { return '' }
}
function Report($st, $id, $desc, $note) {
    switch ($st) { 'PASS' { $script:PASS++ } 'FAIL' { $script:FAIL++ } default { $script:WARN++ } }
    Write-Host "[$st] $id $desc$(if ($note) { " -- $note" })"
    Add-Content -Path $REPORT -Encoding utf8 -Value "| $st | $id | $st | $desc | $(if ($note) { $note }) |"
}
function ToggleTest($sel, $activeClass, $id, $desc) {
    try {
        Nav "/tools/$TOOL_ID"
        if ((CountSel $sel) -lt 1) { Report FAIL $id $desc "element [$sel] not found"; return }
        $before = (CountSel "$sel.$activeClass")
        ClickSel $sel
        Start-Sleep -Milliseconds 1500
        $after = (CountSel "$sel.$activeClass")
        if ($before -ne $after) { Report PASS $id $desc "state changed ($before -> $after)" }
        else { Report FAIL $id $desc "state did not change (before=$before after=$after)" }
    } catch { Report FAIL $id $desc "exception: $_" }
}

Write-Host "=== Stage B: interactions ==="

# TC-501 Theme toggle (dark <-> light) on home page
try {
    Nav '/'
    $lightBefore = (CountSel '[data-theme="light"]')
    ClickSel '.theme-toggle-btn'
    Start-Sleep -Milliseconds 1500
    $lightAfter = (CountSel '[data-theme="light"]')
    if ($lightBefore -ne $lightAfter) { Report PASS 'TC-501' 'Theme toggle (dark/light)' "light flag $lightBefore -> $lightAfter" }
    else { Report FAIL 'TC-501' 'Theme toggle (dark/light)' "theme unchanged (light=$lightBefore)" }
} catch { Report FAIL 'TC-501' 'Theme toggle (dark/light)' "exception: $_" }

# TC-502 Tool like toggle on tool detail
ToggleTest '.unified-like-btn' 'liked' 'TC-502' 'Tool detail like button'

# TC-503 Tool favorite toggle on tool detail
ToggleTest '.unified-fav-btn' 'favorited' 'TC-503' 'Tool detail favorite button'

# TC-504 Submit a comment on tool detail (verify count increases)
try {
    Nav "/tools/$TOOL_ID"
    if ((CountSel '.comment-input') -lt 1) { Report FAIL 'TC-504' 'Submit comment' "comment input not found"; }
    else {
        $beforeTxt = GetText '.comment-header'
        $bm = ($beforeTxt | Select-String -Pattern '\((\d+)\)')
        $before = if ($bm) { [int]$bm.Matches.Groups[1].Value } else { 0 }
        $marker = "E2Eautotest$(Get-Date -Format HHmmss)"
        TypeSel '.comment-input' $marker
        Start-Sleep -Milliseconds 500
        ClickSel '.submit-btn'
        Start-Sleep -Milliseconds 4000
        $afterTxt = GetText '.comment-header'
        $am = ($afterTxt | Select-String -Pattern '\((\d+)\)')
        $after = if ($am) { [int]$am.Matches.Groups[1].Value } else { 0 }
        if ($after -gt $before) { Report PASS 'TC-504' 'Submit comment' "comment count $before -> $after" }
        else {
            $out = oc @('find', '--css', '.comment-content', '--limit', '30')
            if ($out -match [regex]::Escape($marker)) { Report PASS 'TC-504' 'Submit comment' "comment posted ($marker)" }
            else { Report FAIL 'TC-504' 'Submit comment' "count $before -> $after, marker not visible" }
        }
    }
} catch { Report FAIL 'TC-504' 'Submit comment' "exception: $_" }

# TC-505 Home -> tool detail navigation (click first tool name)
try {
    Nav '/'
    if ((CountSel '.tool-name') -lt 1) { Report WARN 'TC-505' 'Home tool-card navigation' "no tool cards rendered (empty data?)" }
    else {
        ClickSel '.tool-name' '0'
        Start-Sleep -Milliseconds 3000
        $u = Get-Url
        if ($u -like "*/tools/*") { Report PASS 'TC-505' 'Home tool-card navigation' "navigated to $u" }
        else { Report FAIL 'TC-505' 'Home tool-card navigation' "did not navigate to detail (URL=$u)" }
    }
} catch { Report FAIL 'TC-505' 'Home tool-card navigation' "exception: $_" }

# TC-506 Home search box accepts input (soft check)
try {
    Nav '/'
    if ((CountSel '.search-input') -lt 1) { Report FAIL 'TC-506' 'Home search input' "search input not found" }
    else {
        TypeSel '.search-input' 'test'
        Start-Sleep -Milliseconds 700
        $v = GetValue '.search-input'
        if ($v -eq 'test') { Report PASS 'TC-506' 'Home search input' "input value='test'" }
        else { Report WARN 'TC-506' 'Home search input' "value='$v' (client filter may differ)" }
    }
} catch { Report FAIL 'TC-506' 'Home search input' "exception: $_" }

$TOTAL = $PASS + $FAIL + $WARN
Write-Host "--- B partial: PASS $PASS | FAIL $FAIL | WARN $WARN ---"
Write-Host "STAGE_B_DONE"
