$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$DbType = $env:DB_TYPE
if (-not $DbType) { $DbType = "mysql" }

if ($DbType -eq "postgresql") {
    $SqlFile = Join-Path $Root "scripts\init-db-postgres.sql"
    if (-not (Test-Path $SqlFile)) {
        throw "未找到初始化 SQL：$SqlFile"
    }
    $env:PGPASSWORD = "codinghub"
    try {
        # 创建数据库（已存在则忽略错误）
        psql -U codinghub -h localhost -p 5432 -d postgres -c "CREATE DATABASE ai_tool_square;" 2>$null
        psql -U codinghub -h localhost -p 5432 -d ai_tool_square -f $SqlFile
        if ($LASTEXITCODE -ne 0) {
            throw "psql 退出码 $LASTEXITCODE"
        }
    }
    finally {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
    Write-Host "PostgreSQL 数据库初始化完成" -ForegroundColor Green
}
else {
    $SqlFile = Join-Path $Root "scripts\init-db.sql"
    $Mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
    if (-not (Test-Path $Mysql)) {
        throw "未找到 MySQL 客户端：$Mysql"
    }
    if (-not (Test-Path $SqlFile)) {
        throw "未找到初始化 SQL：$SqlFile"
    }
    # 使用 cmd /c 和输入重定向来避免 PowerShell BOM 问题
    $env:MYSQL_PWD = "123456"
    try {
        cmd /c "`"$Mysql`" -uroot < `"$SqlFile`""
        if ($LASTEXITCODE -ne 0) {
            throw "mysql 退出码 $LASTEXITCODE"
        }
    }
    finally {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }
    Write-Host "MySQL 数据库初始化完成" -ForegroundColor Green
}
