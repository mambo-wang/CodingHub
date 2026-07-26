# 以管理员身份运行：安装 PostgreSQL 18.4.0
$lock = "C:\ProgramData\chocolatey\lib\5afc917dd1f2c278e34b4aee307e0748b9620e0a"
if (Test-Path $lock) { Remove-Item $lock -Force -ErrorAction SilentlyContinue }

choco install postgresql -y --params "'/Password:codinghub /Port:5432'" *>&1 | Tee-Object -FilePath C:\pg_install.log
$code = $LASTEXITCODE
Add-Content -Path C:\pg_install.log -Value "`n=== EXIT CODE: $code ==="
exit $code
