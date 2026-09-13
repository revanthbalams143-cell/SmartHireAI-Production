@echo off
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0START-SMARTHIRE-ALL.ps1"
endlocal
