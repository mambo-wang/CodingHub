@echo off
cd /d D:\repos\CodingHub\backend
set SERVER_PORT=8082
set SPRING_DATASOURCE_PASSWORD=123456
echo ===== Starting Spring Boot on port 8082 =====
D:\repos\CodingHub\tools\gradle-8.5\bin\gradle.bat bootRun --stacktrace
echo.
echo ===== Backend process exited =====
pause
