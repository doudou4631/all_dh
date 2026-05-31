@echo off
chcp 65001 >nul
cd /d "%~dp0"
set PORT=8080

echo.
echo ========================================
echo   本机访问（仅当前电脑）
echo   地址: http://127.0.0.1:%PORT%/
echo   局域网访问请双击: 启动-局域网.bat
echo   请勿关闭本窗口
echo ========================================
echo.

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " ^| findstr LISTENING') do (
  taskkill /F /PID %%a >nul 2>&1
)

timeout /t 1 >nul
start "" cmd /c "timeout /t 2 >nul & start http://127.0.0.1:%PORT%/"

echo 正在启动 Python 静态服务器...
python -m http.server %PORT% --bind 127.0.0.1
pause
