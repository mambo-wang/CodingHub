$ErrorActionPreference = 'Continue'
$base = 'http://127.0.0.1:8082'
$sseLog = 'd:/repos/CodingHub/.codebuddy/tmp/mcp_sse_full.log'

# 清理旧日志
"" > $sseLog

# 1. 启动 SSE 监听（后台 Job）
Write-Host "[1] Starting SSE listener..."
$sseJob = Start-Job -ScriptBlock {
    param($url, $log)
    curl.exe -sS -m 30 -N -H 'Accept: text/event-stream' $url 2>&1 | Out-File -FilePath $log -Encoding utf8 -Append
} -ArgumentList "$base/sse", $sseLog

# 2. 等待 endpoint 事件
$sid = $null
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Milliseconds 200
    if (Test-Path $sseLog) {
        $c = Get-Content $sseLog -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
        if ($c -match 'sessionId=([a-f0-9-]+)') {
            $sid = $Matches[1]
            break
        }
    }
}

if (-not $sid) {
    Write-Host "[FAIL] No sessionId received" -ForegroundColor Red
    Write-Host "SSE log:"
    Get-Content $sseLog -Raw -Encoding UTF8
    Stop-Job $sseJob; Remove-Job $sseJob
    exit 1
}
Write-Host "[2] sessionId: $sid" -ForegroundColor Green

# 3. 发送 initialize
$msgUrl = "$base/mcp/message?sessionId=$sid"
$initBody = '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"codebuddy","version":"1.0"}}}'

Write-Host "[3] Sending initialize..."
try {
    $initResp = Invoke-WebRequest -Uri $msgUrl -Method POST -ContentType 'application/json' -Body $initBody -TimeoutSec 5 -UseBasicParsing
    Write-Host "   Initialize response: $($initResp.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "   Initialize POST error: $_" -ForegroundColor Yellow
}
Start-Sleep -Milliseconds 500

# 4. 发送 initialized notification + tools/list
$initNotif = '{"jsonrpc":"2.0","method":"notifications/initialized"}'
$listReq = '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'

Write-Host "[4] Sending initialized notification..."
try {
    Invoke-WebRequest -Uri $msgUrl -Method POST -ContentType 'application/json' -Body $initNotif -TimeoutSec 5 -UseBasicParsing | Out-Null
} catch { Write-Host "   (notif error ignored)" -ForegroundColor Yellow }
Start-Sleep -Milliseconds 300

Write-Host "[5] Sending tools/list..."
try {
    $listResp = Invoke-WebRequest -Uri $msgUrl -Method POST -ContentType 'application/json' -Body $listReq -TimeoutSec 5 -UseBasicParsing
    Write-Host "   Status: $($listResp.StatusCode)" -ForegroundColor Green
} catch { Write-Host "   Error: $_" -ForegroundColor Yellow }

# 5. 等待 SSE 响应
Start-Sleep -Seconds 3
Stop-Job $sseJob; Remove-Job $sseJob

# 6. 输出完整 SSE 日志
Write-Host ""
Write-Host "========== SSE STREAM LOG ==========" -ForegroundColor Yellow
Get-Content $sseLog -Raw -Encoding UTF8
