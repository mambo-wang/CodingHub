#!/usr/bin/env pwsh
# MCP 客户端脚本：连接 SSE，握手 initialize，调用 tools/list
$ErrorActionPreference = 'Continue'
$base = 'http://127.0.0.1:8082'
$logFile = Join-Path $PSScriptRoot 'mcp_sse.log'

# 1. 启动 SSE 监听作业
Write-Host '[1] 启动 SSE 监听...' -ForegroundColor Cyan
$job = Start-Job -ScriptBlock {
    param($url, $log)
    curl.exe -sS -m 30 -N -H 'Accept: text/event-stream' $url 2>&1 | Out-File -FilePath $log -Encoding utf8 -Append
} -ArgumentList "$base/sse", $logFile

# 2. 等待 endpoint 事件出现（最多 5 秒）
$sid = $null
for ($i = 0; $i -lt 50; $i++) {
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
    Stop-Job $job -ErrorAction SilentlyContinue
    Remove-Job $job -ErrorAction SilentlyContinue
    exit 1
}
Write-Host "[2] sessionId = $sid" -ForegroundColor Green

# 3. 发送 initialize
$msgUrl = "$base/mcp/message?sessionId=$sid"
$initBody = '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"codebuddy-cli","version":"1.0"}}}'
Write-Host '[3] 发送 initialize...' -ForegroundColor Cyan
curl.exe -sS -m 5 -X POST -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' --data-binary $initBody $msgUrl 2>&1 | Out-Null
Start-Sleep -Milliseconds 800

# 4. 发送 notifications/initialized + tools/list
$initNotif = '{"jsonrpc":"2.0","method":"notifications/initialized"}'
$listReq = '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
Write-Host '[4] 发送 notifications/initialized + tools/list...' -ForegroundColor Cyan
curl.exe -sS -m 5 -X POST -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' --data-binary $initNotif $msgUrl 2>&1 | Out-Null
Start-Sleep -Milliseconds 500
curl.exe -sS -m 5 -X POST -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' --data-binary $listReq $msgUrl 2>&1 | Out-Null

# 5. 等待响应
Start-Sleep -Seconds 2
Stop-Job $job -ErrorAction SilentlyContinue
Remove-Job $job -ErrorAction SilentlyContinue

# 6. 输出日志
Write-Host "`n===== SSE 流日志 =====" -ForegroundColor Yellow
if (Test-Path $logFile) {
    Get-Content $logFile -Raw
} else {
    Write-Host '(空)'
}
