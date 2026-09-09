# CodingHub 论坛 md/html 支持 —— 端到端测试脚本
#
# 用途：编码完成后自动重启后端并回归「论坛 md/html 导入与编辑 + MCP 同步支持」全链路。
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts/e2e-forum.ps1            # 重启后端 + 全量回归
#   powershell -ExecutionPolicy Bypass -File scripts/e2e-forum.ps1 -SkipRestart  # 后端已在跑，只跑测试
#   powershell -ExecutionPolicy Bypass -File scripts/e2e-forum.ps1 -KeepData    # 保留测试帖便于肉眼看渲染
#
# 覆盖：
#   REST  登录 / 建 HTML 帖 / 建 MD 帖 / 缺省格式 / 读取 / 更新
#   REST  导入 .md（含 BOM）/ 导入 .html / 中文 title 覆盖 / 非法扩展名 400 / 未认证 401
#   MCP   tools/list 计数 / post_list / post_get / post_import / post_create / post_update
#
# 退出码：0 全部通过；1 存在失败项。

param(
    [switch]$SkipRestart,
    [switch]$KeepData,
    [string]$BaseUrl = 'http://localhost:8082',
    [string]$RepoRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$Username = 'wangbao',
    [string]$Password = '123456'
)

$ErrorActionPreference = 'Continue'
Add-Type -AssemblyName System.Net.Http

$script:Results = @()
$script:CreatedPostIds = @()

function Add-Result {
    param([string]$Name, [bool]$Ok, [string]$Detail = '')
    $script:Results += [pscustomobject]@{ Name = $Name; Ok = $Ok; Detail = $Detail }
    $mark = if ($Ok) { 'PASS' } else { 'FAIL' }
    Write-Host "  [$mark] $Name" -ForegroundColor $(if ($Ok) { 'Green' } else { 'Red' })
    if (-not $Ok -and $Detail) { Write-Host "         $Detail" -ForegroundColor DarkGray }
}

# ── 1. 重启后端 ────────────────────────────────────────────────
function Restart-Backend {
    Write-Host "`n== 重启后端 ==" -ForegroundColor Cyan
    $backendDir = Join-Path $RepoRoot 'backend'
    $log = Join-Path $backendDir 'boot.log'

    $conn = Get-NetTCPConnection -State Listen -LocalPort 8082 -ErrorAction SilentlyContinue
    if ($conn) {
        $pids = $conn.OwningProcess | Select-Object -Unique
        foreach ($p in $pids) {
            Get-CimInstance Win32_Process -Filter "ParentProcessId=$p" -ErrorAction SilentlyContinue |
                ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
            Get-Process -Id $p -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
        }
        Start-Sleep -Seconds 4
    }

    if (Test-Path $log) { Remove-Item $log -Force -ErrorAction SilentlyContinue }
    Start-Process -FilePath 'cmd.exe' `
        -ArgumentList "/c", "gradlew.bat bootRun > `"$log`" 2>&1" `
        -WorkingDirectory $backendDir -WindowStyle Minimized

    $deadline = (Get-Date).AddSeconds(300)
    while ((Get-Date) -lt $deadline) {
        if (Test-Path $log) {
            $c = Get-Content $log -Raw -ErrorAction SilentlyContinue
            if ($c -match 'Started .+ in ') { return (Wait-Ready) }
            if ($c -match 'APPLICATION FAILED TO START|BUILD FAILED') { return $false }
        }
        Start-Sleep -Seconds 5
    }
    return $false
}

# 就绪探针：Spring 打印 "Started ... in" 之后 DispatcherServlet 可能还要十几秒才初始化完，
# 此时直接打接口会拿到连接失败。必须轮询真实 HTTP 端点，不能只看日志。
function Wait-Ready {
    param([int]$TimeoutSec = 120)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-WebRequest -Uri "$BaseUrl/api/forum/categories" -UseBasicParsing -TimeoutSec 10
            if ([int]$r.StatusCode -lt 500) { return $true }
        } catch { }
        Start-Sleep -Seconds 2
    }
    return $false
}

# ── 2. HTTP 辅助 ───────────────────────────────────────────────
function Invoke-Json {
    param([string]$Method, [string]$Path, $Body = $null, [hashtable]$Headers = @{}, [int]$Expected = 200)
    $params = @{ Uri = "$BaseUrl$Path"; Method = $Method; Headers = $Headers; UseBasicParsing = $true; TimeoutSec = 30 }
    if ($null -ne $Body) {
        $params.ContentType = 'application/json; charset=utf-8'
        $params.Body = ($Body | ConvertTo-Json -Depth 6)
    }
    try {
        $r = Invoke-WebRequest @params
        $json = $null
        if ($r.Content) { try { $json = $r.Content | ConvertFrom-Json } catch { } }
        return @{ Status = [int]$r.StatusCode; Json = $json; Ok = ([int]$r.StatusCode -eq $Expected) }
    } catch {
        $code = 0
        if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
        $body = ''
        try {
            $stream = $_.Exception.Response.GetResponseStream()
            if ($stream) { $body = (New-Object System.IO.StreamReader($stream)).ReadToEnd() }
        } catch { }
        return @{ Status = $code; Json = $body; Ok = ($code -eq $Expected) }
    }
}

# multipart 上传：必须显式 UTF-8 —— Windows curl.exe 会按控制台代码页发 GBK，
# 导致中文表单字段乱码（服务端按 UTF-8 解析，乱码是测试工具的锅，不是产品问题）。
function Invoke-Import {
    param([string]$FilePath, [hashtable]$Fields = @{}, [hashtable]$Headers = @{}, [int]$Expected = 201)
    $client = New-Object System.Net.Http.HttpClient
    try {
        foreach ($k in $Headers.Keys) { $client.DefaultRequestHeaders.Add($k, $Headers[$k]) }
        $content = New-Object System.Net.Http.MultipartFormDataContent
        $bytes = [System.IO.File]::ReadAllBytes($FilePath)
        $fileContent = New-Object System.Net.Http.ByteArrayContent(, $bytes)
        $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse('application/octet-stream')
        $content.Add($fileContent, 'file', [System.IO.Path]::GetFileName($FilePath))
        foreach ($k in $Fields.Keys) {
            $content.Add((New-Object System.Net.Http.StringContent([string]$Fields[$k], [System.Text.Encoding]::UTF8)), $k)
        }
        $resp = $client.PostAsync("$BaseUrl/api/forum/posts/import", $content).Result
        $body = $resp.Content.ReadAsStringAsync().Result
        $code = [int]$resp.StatusCode
        $json = $null
        try { $json = $body | ConvertFrom-Json } catch { }
        return @{ Status = $code; Json = $json; Ok = ($code -eq $Expected) }
    } finally { $client.Dispose() }
}

# ── 3. MCP 辅助（Streamable HTTP，响应为 SSE）────────────────────
function New-McpSession {
    $client = New-Object System.Net.Http.HttpClient
    $init = '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"e2e-forum","version":"1.0"}}}'
    $r = Send-Mcp $client $init $null
    $sid = $r.sid
    if ($sid) { $null = Send-Mcp $client '{"jsonrpc":"2.0","method":"notifications/initialized"}' $sid }
    return @{ Client = $client; SessionId = $sid }
}

function Send-Mcp {
    param($Client, [string]$Json, [string]$SessionId)
    $req = New-Object System.Net.Http.HttpRequestMessage
    $req.Method = 'POST'
    $req.RequestUri = "$BaseUrl/mcp"
    $req.Content = New-Object System.Net.Http.StringContent($Json, [System.Text.Encoding]::UTF8, 'application/json')
    $req.Headers.Accept.Add([System.Net.Http.Headers.MediaTypeWithQualityHeaderValue]::Parse('application/json'))
    $req.Headers.Accept.Add([System.Net.Http.Headers.MediaTypeWithQualityHeaderValue]::Parse('text/event-stream'))
    if ($SessionId) { $req.Headers.Add('Mcp-Session-Id', $SessionId) }
    $resp = $Client.SendAsync($req).Result
    $v = $null
    $sid = ''
    if ($resp.Headers.TryGetValues('Mcp-Session-Id', [ref]$v)) { $sid = ($v -join ',') }
    return @{ Status = [int]$resp.StatusCode; Sid = $sid; Body = $resp.Content.ReadAsStringAsync().Result }
}

function Get-McpPayload {
    param([string]$Body)
    $lines = $Body -split "`n" | Where-Object { $_ -like 'data: *' }
    if ($lines) { return ($lines | Select-Object -Last 1).Substring(5) }
    return $Body
}

# 每个 MCP 调用独立握手：同一会话连续并发调用会被服务端判为非法请求（HTTP 400）。
function Invoke-McpTool {
    param([string]$Tool, [hashtable]$Arguments = @{})
    $s = New-McpSession
    try {
        $argsJson = if ($Arguments.Count -gt 0) { $Arguments | ConvertTo-Json -Compress } else { '{}' }
        $payload = "{""jsonrpc"":""2.0"",""id"":2,""method"":""tools/call"",""params"":{""name"":""$Tool"",""arguments"":$argsJson}}"
        $r = Send-Mcp $s.Client $payload $s.SessionId
        $data = Get-McpPayload $r.Body
        $text = ''
        try {
            $parsed = $data | ConvertFrom-Json
            $text = $parsed.result.content[0].text
        } catch { $text = $data }
        return @{ Status = $r.Status; Text = $text; Ok = ($r.Status -eq 200) }
    } finally { $s.Client.Dispose() }
}

# ── 4. 测试固件 ────────────────────────────────────────────────
function New-Fixtures {
    param([string]$Dir)
    New-Item -ItemType Directory -Force -Path $Dir | Out-Null

    # 故意带 BOM：Windows 记事本保存的 .md 普遍带 BOM，
    # 未剥离会让 "^#{1,6}\s" 标题推导失效（^ 锚在行首，BOM 顶在 # 前面）。
    # 用 [char]0xFEFF 而非 `u{FEFF}——后者是 PowerShell 6+ 语法，5.1 下不识别。
    $bom = [char]0xFEFF
    $md = "$bom# 导入的 Markdown 标题`n`n这是导入正文段落。`n"
    [System.IO.File]::WriteAllText((Join-Path $Dir 'sample.md'), $md, (New-Object System.Text.UTF8Encoding($false)))

    $html = '<!DOCTYPE html>' + "`n" +
            '<html><head><title>导入的 HTML 标题</title></head>' + "`n" +
            '<body><h1>备用 H1</h1><p>HTML 正文</p></body></html>'
    [System.IO.File]::WriteAllText((Join-Path $Dir 'sample.html'), $html, (New-Object System.Text.UTF8Encoding($false)))

    [System.IO.File]::WriteAllText((Join-Path $Dir 'bad.txt'), 'not allowed', (New-Object System.Text.UTF8Encoding($false)))
}

function New-Post {
    param([hashtable]$Headers, [string]$Title, [string]$Content, [int]$CategoryId, [string]$Format)
    $body = @{ title = $Title; content = $Content; categoryId = $CategoryId }
    if ($Format) { $body.contentFormat = $Format }
    $r = Invoke-Json -Method 'POST' -Path '/api/forum/posts' -Body $body -Headers $Headers -Expected 201
    if ($r.Ok -and $r.Json -and $r.Json.id) { $script:CreatedPostIds += $r.Json.id }
    return $r
}

function Remove-Post {
    param([hashtable]$Headers, [int]$Id)
    $null = Invoke-Json -Method 'DELETE' -Path "/api/forum/posts/$Id" -Headers $Headers -Expected 204
}

# ── 主流程 ─────────────────────────────────────────────────────
Write-Host "CodingHub 论坛 md/html 端到端测试" -ForegroundColor Cyan
Write-Host "BaseUrl = $BaseUrl`n"

if (-not $SkipRestart) {
    $up = Restart-Backend
    if (-not $up) { Write-Host "后端启动失败，查看 $(Join-Path $RepoRoot 'backend\boot.log')" -ForegroundColor Red; exit 1 }
    Add-Result '后端重启并启动成功' $true
}

if (-not (Wait-Ready)) {
    Add-Result '后端就绪（HTTP 可响应）' $false "超时未响应 $BaseUrl"
    exit 1
}
Add-Result '后端就绪（HTTP 可响应）' $true

$tmp = Join-Path $env:TEMP 'ch-e2e'
New-Fixtures $tmp

# 登录
$login = Invoke-Json -Method 'POST' -Path '/api/v1/auth/login' -Body @{ username = $Username; password = $Password }
if (-not $login.Ok -or -not $login.Json.data.accessToken) {
    Add-Result '登录获取 token' $false "HTTP $($login.Status)"
    exit 1
}
$hdr = @{ Authorization = "Bearer $($login.Json.data.accessToken)" }
Add-Result '登录获取 token' $true

# 分类
$cats = Invoke-Json -Method 'GET' -Path '/api/forum/categories' -Headers $hdr
$catId = if ($cats.Json -and $cats.Json.Count -gt 0) { $cats.Json[0].id } else { 1 }

Write-Host "`n== REST：创建 / 读取 / 更新 ==" -ForegroundColor Cyan
$p1 = New-Post $hdr 'E2E HTML 帖' '<h2>小标题</h2><p>这是一段 <b>HTML</b> 正文</p>' $catId 'HTML'
Add-Result '创建 HTML 帖返回 HTML' ($p1.Ok -and $p1.Json.contentFormat -eq 'HTML') "实际=$($p1.Json.contentFormat)"

$p2 = New-Post $hdr 'E2E MD 帖' "# 一级标题`n正文 **加粗**" $catId 'MARKDOWN'
Add-Result '创建 MD 帖返回 MARKDOWN' ($p2.Ok -and $p2.Json.contentFormat -eq 'MARKDOWN') "实际=$($p2.Json.contentFormat)"

$p3 = New-Post $hdr 'E2E 缺省格式帖' '不带 contentFormat' $catId $null
Add-Result '未指定格式默认 MARKDOWN' ($p3.Ok -and $p3.Json.contentFormat -eq 'MARKDOWN') "实际=$($p3.Json.contentFormat)"

$g = Invoke-Json -Method 'GET' -Path "/api/forum/posts/$($p1.Json.id)" -Headers $hdr
Add-Result '读取帖子带回 contentFormat' ($g.Ok -and $g.Json.contentFormat -eq 'HTML') "实际=$($g.Json.contentFormat)"

$u = Invoke-Json -Method 'PUT' -Path "/api/forum/posts/$($p1.Json.id)" -Headers $hdr -Body @{
    title = 'E2E HTML 帖(改)'; content = '<p>改过了</p>'; categoryId = $catId; contentFormat = 'HTML'
}
Add-Result '更新帖子保持格式' ($u.Ok -and $u.Json.contentFormat -eq 'HTML') "实际=$($u.Json.contentFormat)"

Write-Host "`n== REST：导入 ==" -ForegroundColor Cyan
$i1 = Invoke-Import (Join-Path $tmp 'sample.md') @{ categoryId = $catId } $hdr
if ($i1.Ok -and $i1.Json.id) { $script:CreatedPostIds += $i1.Json.id }
Add-Result '导入 .md（带 BOM）标题从 # 推导' ($i1.Ok -and $i1.Json.title -eq '导入的 Markdown 标题') "实际标题=$($i1.Json.title)"
Add-Result '导入 .md 格式为 MARKDOWN' ($i1.Ok -and $i1.Json.contentFormat -eq 'MARKDOWN') "实际=$($i1.Json.contentFormat)"
# 必须用 Ordinal：.NET 默认的文化敏感比较会把 U+FEFF 当零权重字符忽略，
# 于是 StartsWith(BOM) 对任意字符串都返回 true（跟服务端那个 BOM bug 是同一个坑）。
$bomStr = [string][char]0xFEFF
Add-Result '导入 .md 正文已剥离 BOM' ($i1.Ok -and -not $i1.Json.content.StartsWith($bomStr, [System.StringComparison]::Ordinal)) ''

$i2 = Invoke-Import (Join-Path $tmp 'sample.html') @{ categoryId = $catId } $hdr
if ($i2.Ok -and $i2.Json.id) { $script:CreatedPostIds += $i2.Json.id }
Add-Result '导入 .html 标题从 <title> 推导' ($i2.Ok -and $i2.Json.title -eq '导入的 HTML 标题') "实际标题=$($i2.Json.title)"
Add-Result '导入 .html 格式为 HTML' ($i2.Ok -and $i2.Json.contentFormat -eq 'HTML') "实际=$($i2.Json.contentFormat)"

$i3 = Invoke-Import (Join-Path $tmp 'sample.html') @{ title = '显式覆盖标题'; contentFormat = 'MARKDOWN' } $hdr
if ($i3.Ok -and $i3.Json.id) { $script:CreatedPostIds += $i3.Json.id }
Add-Result '导入支持中文 title 覆盖' ($i3.Ok -and $i3.Json.title -eq '显式覆盖标题') "实际标题=$($i3.Json.title)"
Add-Result '导入支持 contentFormat 覆盖' ($i3.Ok -and $i3.Json.contentFormat -eq 'MARKDOWN') "实际=$($i3.Json.contentFormat)"

$i4 = Invoke-Import (Join-Path $tmp 'bad.txt') @{} $hdr -Expected 400
Add-Result '导入非法扩展名返回 400' $i4.Ok "实际=$($i4.Status)"

$i5 = Invoke-Import (Join-Path $tmp 'sample.md') @{} @{} -Expected 401
Add-Result '导入未认证返回 401' $i5.Ok "实际=$($i5.Status)"

Write-Host "`n== MCP ==" -ForegroundColor Cyan
$s = New-McpSession
try {
    $list = Send-Mcp $s.Client '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}' $s.SessionId
    $names = [regex]::Matches($list.Body, '"name":"(h3_coding_hub_[a-z_]+)"') | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique
    Add-Result 'MCP 工具总数 = 26' ($names.Count -eq 26) "实际=$($names.Count)"
    $postTools = $names | Where-Object { $_ -like '*post*' }
    Add-Result 'MCP 帖子工具 = 6 个' ($postTools.Count -eq 6) "实际=$(($postTools) -join ',')"
} finally { $s.Client.Dispose() }

$mList = Invoke-McpTool 'h3_coding_hub_post_list' @{ size = 3; sortBy = 'latest' }
Add-Result 'MCP post_list 可用' $mList.Ok "HTTP $($mList.Status)"
Add-Result 'MCP post_list 不含正文' ($mList.Ok -and $mList.Text -notmatch '\\?"content\\?":') ''
Add-Result 'MCP post_list 含 contentFormat' ($mList.Ok -and $mList.Text -match 'contentFormat') ''

$mImport = Invoke-McpTool 'h3_coding_hub_post_import' @{ categoryId = $catId }
Add-Result 'MCP post_import 只回传接口信息' ($mImport.Ok -and $mImport.Text -match '/api/forum/posts/import') ''

$mCreate = Invoke-McpTool 'h3_coding_hub_post_create' @{
    title = 'MCP E2E HTML 帖'; content = '<p>来自 MCP 的 <b>HTML</b> 正文</p>'
    categoryId = $catId; contentFormat = 'HTML'; username = $Username; password = $Password
}
Add-Result 'MCP post_create 建出 HTML 帖' ($mCreate.Ok -and $mCreate.Text -match '"contentFormat":"HTML"') ''
$mcpPostId = 0
if ($mCreate.Ok -and $mCreate.Text -match '"id":(\d+)') { $mcpPostId = [int]$Matches[1]; $script:CreatedPostIds += $mcpPostId }

if ($mcpPostId -gt 0) {
    $mGet = Invoke-McpTool 'h3_coding_hub_post_get' @{ postId = $mcpPostId }
    Add-Result 'MCP post_get 返回 contentFormat' ($mGet.Ok -and $mGet.Text -match '"contentFormat":"HTML"') ''

    $mUpd = Invoke-McpTool 'h3_coding_hub_post_update' @{
        postId = $mcpPostId; title = 'MCP 更新后的标题'; username = $Username; password = $Password
    }
    Add-Result 'MCP post_update 改标题' ($mUpd.Ok -and $mUpd.Text -match 'MCP 更新后的标题') ''
    Add-Result 'MCP post_update 未传字段保持原值' ($mUpd.Ok -and $mUpd.Text -match '来自 MCP 的') ''
}

# ── 清理与汇总 ─────────────────────────────────────────────────
if (-not $KeepData) {
    Write-Host "`n== 清理测试帖 ==" -ForegroundColor Cyan
    foreach ($id in ($script:CreatedPostIds | Select-Object -Unique)) { Remove-Post $hdr $id }
    Write-Host "  已软删: $(($script:CreatedPostIds | Select-Object -Unique) -join ', ')" -ForegroundColor DarkGray
} else {
    Write-Host "`n  保留测试帖（-KeepData）: $(($script:CreatedPostIds | Select-Object -Unique) -join ', ')" -ForegroundColor DarkGray
}

$failed = $script:Results | Where-Object { -not $_.Ok }
Write-Host "`n== 汇总 ==" -ForegroundColor Cyan
Write-Host "  通过 $(($script:Results | Where-Object { $_.Ok }).Count) / 共 $($script:Results.Count)"
if ($failed) {
    Write-Host "  失败项：" -ForegroundColor Red
    $failed | ForEach-Object { Write-Host "    - $($_.Name)" -ForegroundColor Red }
    exit 1
}
Write-Host "  全部通过" -ForegroundColor Green
exit 0
