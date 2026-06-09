$ErrorActionPreference = "SilentlyContinue"

Get-CimInstance Win32_Process |
    Where-Object { $_.CommandLine -match 'gradlew\.bat bootRun|GradleWrapperMain bootRun|vite' } |
    ForEach-Object { Stop-Process -Id $_.ProcessId -Force }

Write-Host "已停止后端/前端开发进程。"
