@echo off
chcp 65001 >nul
cd /d "%~dp0"
set PORT=8080

echo.
echo ========================================
echo   局域网访问模式
echo   请勿关闭本窗口
echo ========================================
echo.
echo 本机: http://127.0.0.1:%PORT%/
echo.
echo 局域网其他设备:
for /f "tokens=2 delims=:" %%i in ('ipconfig ^| findstr /c:"IPv4"') do (
  for /f "tokens=1" %%j in ("%%i") do echo   http://%%j:%PORT%/
)
echo.

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " ^| findstr LISTENING') do (
  taskkill /F /PID %%a >nul 2>&1
)

timeout /t 1 >nul

echo 正在启动 Python 静态服务器...
python -m http.server %PORT% --bind 0.0.0.0
pause
