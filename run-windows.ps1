$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Backend = Join-Path $Root "backend"
$Frontend = Join-Path $Root "frontend"
$LocalGradle = Join-Path $Root "tools\gradle-8.5\bin\gradle.bat"

# Windows 配置：后端端口 8082（8082 被 McAfee Agent 占用）
$BackendPort = "8082"
$MysqlPassword = "123456"

function Test-Command($Name) {
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

if (-not (Test-Command "java")) {
    throw "未找到 java。请安装 JDK 17 并把 java 加入 PATH。"
}

$javaVersionOutput = (& java -version) 2>&1 | Out-String
if ($javaVersionOutput -notmatch 'version "17\.') {
    Write-Warning "当前 java 版本看起来不是 17：`n$javaVersionOutput"
}

if (-not (Test-Command "npm")) {
    throw "未找到 npm。请先安装 Node.js 18+。"
}

if (-not (Test-Path $LocalGradle)) {
    throw "未找到本地 Gradle：$LocalGradle。请先执行 setup-windows.ps1。"
}

if (-not (Test-Path (Join-Path $Frontend "node_modules"))) {
    Write-Host "安装前端依赖..."
    Push-Location $Frontend
    npm install
    Pop-Location
}

# 生成后端启动脚本
$backendLaunchPs1 = Join-Path $Root "backend\launch.ps1"
@"
`$env:SPRING_DATASOURCE_PASSWORD='$MysqlPassword'
`$env:SERVER_PORT='$BackendPort'
& '$LocalGradle' bootRun
"@ | Set-Content -Path $backendLaunchPs1 -Force

# 生成前端启动脚本
$frontendLaunchPs1 = Join-Path $Root "frontend\launch.ps1"
@"
`$env:BACKEND_PORT='$BackendPort'
`$env:VITE_BACKEND_PORT='$BackendPort'
npm run dev -- --host 127.0.0.1
"@ | Set-Content -Path $frontendLaunchPs1 -Force

Write-Host "启动后端服务：http://localhost:$BackendPort" -ForegroundColor Green
Start-Process powershell -WindowStyle Normal -ArgumentList "-NoExit", "-File", $backendLaunchPs1

Write-Host "启动前端服务：http://localhost:5173" -ForegroundColor Green
Start-Process powershell -WindowStyle Normal -ArgumentList "-NoExit", "-File", $frontendLaunchPs1

Write-Host ""
Write-Host "已启动。前端: http://localhost:5173  后端: http://localhost:$BackendPort" -ForegroundColor Yellow
Write-Host "提示：Windows 上 MySQL root 密码使用 $MysqlPassword 覆盖（不影响 macOS 端 application.yml）。" -ForegroundColor Yellow
