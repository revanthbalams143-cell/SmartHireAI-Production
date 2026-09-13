@echo off
setlocal
cd /d "%~dp0"
echo.
echo SmartHire AI - clean isolated AI setup
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0REBUILD-AI-VENVS.ps1"
set "RC=%ERRORLEVEL%"
echo.
if not "%RC%"=="0" (
  echo [ERROR] AI rebuild script returned %RC%.
  echo Check logs in "%~dp0logs".
  exit /b %RC%
)
echo AI rebuild completed. Run RUN-SMARTHIRE.cmd for the complete stack.
pause
endlocal
