#!/usr/bin/env pwsh
$ErrorActionPreference = 'Continue'
$base = 'http://127.0.0.1:8082'
$logFile = Join-Path $PSScriptRoot 'mcp_sse.log'
Remove-Item $logFile -ErrorAction SilentlyContinue

# 1. 启动 SSE 监听进程（用 Start-Process，PID 可控）
Write-Host '[1] 启动 SSE 监听进程...' -ForegroundColor Cyan
$sseArgs = @('-sS', '-m', '30', '-N', '-H', 'Accept: text/event-stream', "$base/sse")
$proc = Start-Process -FilePath 'curl.exe' -ArgumentList $sseArgs `
                       -RedirectStandardOutput $logFile `
                       -RedirectStandardError "$logFile.err" `
                       -NoNewWindow -PassThru
Write-Host "    SSE PID = $($proc.Id)"

# 2. 轮询等待 sessionId
$sid = $null
for ($i = 0; $i -lt 100; $i++) {
    Start-Sleep -Milliseconds 100
    if (Test-Path $logFile) {
        $content = Get-Content $logFile -Raw -ErrorAction SilentlyContinue
        if ($content -match 'sessionId=([a-f0-9-]+)') {
            $sid = $Matches[1]
            break
        }
    }
}
if (-not $sid) {
    Write-Host '[X] 未获取到 sessionId' -ForegroundColor Red
    Stop-Process $proc -Force -ErrorAction SilentlyContinue
    exit 1
}
Write-Host "[2] sessionId = $sid" -ForegroundColor Green

# 3. 发送 initialize
$msgUrl = "$base/mcp/message?sessionId=$sid"
$initBody = '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"codebuddy-cli","version":"1.0"}}}'
Write-Host '[3] 发送 initialize...' -ForegroundColor Cyan
$tmp1 = New-TemporaryFile
[System.IO.File]::WriteAllText($tmp1.FullName, $initBody, [System.Text.Encoding]::UTF8)
curl.exe -sS -m 5 -X POST -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' --data-binary "@$($tmp1.FullName)" $msgUrl 2>&1 | Out-Null
Start-Sleep -Milliseconds 1500

# 4. 发送 notifications/initialized
$notif = '{"jsonrpc":"2.0","method":"notifications/initialized"}'
$tmp2 = New-TemporaryFile
[System.IO.File]::WriteAllText($tmp2.FullName, $notif, [System.Text.Encoding]::UTF8)
Write-Host '[4] 发送 notifications/initialized...' -ForegroundColor Cyan
curl.exe -sS -m 5 -X POST -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' --data-binary "@$($tmp2.FullName)" $msgUrl 2>&1 | Out-Null
Start-Sleep -Milliseconds 500

# 5. 发送 tools/list
$listReq = '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
$tmp3 = New-TemporaryFile
[System.IO.File]::WriteAllText($tmp3.FullName, $listReq, [System.Text.Encoding]::UTF8)
Write-Host '[5] 发送 tools/list...' -ForegroundColor Cyan
curl.exe -sS -m 5 -X POST -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' --data-binary "@$($tmp3.FullName)" $msgUrl 2>&1 | Out-Null

# 6. 等待响应
Start-Sleep -Seconds 3

# 7. 停止 SSE 监听
try { Stop-Process $proc -Force -ErrorAction SilentlyContinue } catch {}

# 清理
Remove-Item $tmp1, $tmp2, $tmp3 -ErrorAction SilentlyContinue

# 8. 输出日志
Write-Host "`n===== SSE 流日志 =====" -ForegroundColor Yellow
if (Test-Path $logFile) {
    Get-Content $logFile -Raw
} else {
    Write-Host '(日志文件不存在)'
}
