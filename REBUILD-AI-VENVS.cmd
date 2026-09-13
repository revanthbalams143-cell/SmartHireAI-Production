@echo off
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0REBUILD-AI-VENVS.ps1"
pause
endlocal
