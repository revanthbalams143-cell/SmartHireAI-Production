$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
& (Join-Path $root "START-AI-SERVICES.ps1") -RebuildEnvironments
