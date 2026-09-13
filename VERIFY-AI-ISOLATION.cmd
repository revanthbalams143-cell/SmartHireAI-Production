@echo off
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0VERIFY-AI-ISOLATION.ps1"
echo.
pause
endlocal
