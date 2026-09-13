@echo off
setlocal
cd /d "%~dp0"
echo.
echo =========================================================
echo  SmartHire AI - AI Services Verification and Diagnostics
echo =========================================================
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0VERIFY-AI-ISOLATION.ps1"
set "RC=%ERRORLEVEL%"
echo.
if not "%RC%"=="0" (
    echo [ERROR] Verification failed with code %RC%.
) else (
    echo [SUCCESS] All AI services verified healthy and isolated.
)
echo.
pause
exit /b %RC%
