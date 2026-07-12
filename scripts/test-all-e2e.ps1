# CodingHub full-feature E2E orchestrator (Windows / PowerShell)
# Pure ASCII. Runs all 3 stage scripts in order and produces scripts/e2e-report.md.
#
# Prereqs:
#   - backend  http://localhost:8082   frontend http://localhost:5173
#   - opencli doctor: Daemon/Extension connected
#     (on this machine opencli is a node submodule at ~/.opencli/...)
#
# Run:
#   powershell -ExecutionPolicy Bypass -File scripts/test-all-e2e.ps1
#
# Notes:
#   - Account wangbao is already logged in (USER role). Protected pages load;
#     admin pages redirect to Home (RBAC) -> reported as WARN, not FAIL.
#   - Dev DB has tools+categories seeded; forum posts / videos / KBs are empty,
#     so their detail pages render empty states -> WARN, not FAIL.
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

$ScriptDir = $PSScriptRoot
$Stages = @('e2e-pages.ps1', 'e2e-pages2.ps1', 'e2e-interactions.ps1')

Write-Host "============================================"
Write-Host "  CodingHub Full-Feature E2E  (opencli)"
Write-Host "============================================"

foreach ($stage in $Stages) {
    $path = Join-Path $ScriptDir $stage
    Write-Host "`n>>> Running $stage ..."
    & powershell -ExecutionPolicy Bypass -NoProfile -File $path
}

Write-Host "`n============================================"
Write-Host "  All stages done. Report: scripts/e2e-report.md"
Write-Host "============================================"
Write-Host "ALL_STAGES_DONE"
