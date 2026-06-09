$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$ToolsDir = Join-Path $Root "tools"
$GradleDir = Join-Path $ToolsDir "gradle-8.5"
$GradleBat = Join-Path $GradleDir "bin\gradle.bat"
$ZipPath = Join-Path $ToolsDir "gradle-8.5-bin.zip"

if (Test-Path $GradleBat) {
    Write-Host "本地 Gradle 已存在：$GradleDir" -ForegroundColor Green
}
else {
    if (-not (Test-Path $ZipPath)) {
        Write-Host "下载 Gradle 8.5..."
        New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
        Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-8.5-bin.zip" -OutFile $ZipPath
    }

    Write-Host "解压 Gradle 8.5..."
    Expand-Archive -Path $ZipPath -DestinationPath $ToolsDir -Force
}

if (-not (Get-Command "java" -ErrorAction SilentlyContinue)) {
    throw "未找到 java。请安装 JDK 17 并把 java 加入 PATH。"
}
$javaVersionOutput = (& java -version) 2>&1 | Out-String
if ($javaVersionOutput -notmatch 'version "17\.') {
    Write-Warning "当前 java 不是 17：$javaVersionOutput"
}

Write-Host "Gradle 版本：" -ForegroundColor Green
& $GradleBat -v | Select-String "Gradle|JVM"
