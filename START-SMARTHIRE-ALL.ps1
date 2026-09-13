param(
    [switch]$SkipAiInstall,
    [string]$PostgresPassword = ''
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backend = Join-Path $root 'smarthire-backend'
$stateDir = Join-Path $root '.smarthire-local'
$securePasswordFile = Join-Path $stateDir 'postgres-password.secure.txt'
$logDir = Join-Path $root 'logs'
New-Item -ItemType Directory -Path $stateDir,$logDir -Force | Out-Null

Write-Host ''
Write-Host '=========================================================' -ForegroundColor Cyan
Write-Host ' SmartHire AI - FULL LOCAL STACK LAUNCHER' -ForegroundColor Cyan
Write-Host ' PostgreSQL + Backend + AI/CNN + Frontend' -ForegroundColor Cyan
Write-Host '=========================================================' -ForegroundColor Cyan

# Local runtime configuration. Production deployments should use real secrets/env vars.
$env:SPRING_DATASOURCE_URL = if ($env:SPRING_DATASOURCE_URL) { $env:SPRING_DATASOURCE_URL } else { 'jdbc:postgresql://localhost:5432/smarthire' }
$env:SPRING_DATASOURCE_USERNAME = if ($env:SPRING_DATASOURCE_USERNAME) { $env:SPRING_DATASOURCE_USERNAME } else { 'postgres' }
$env:APP_JWT_SECRET = if ($env:APP_JWT_SECRET) { $env:APP_JWT_SECRET } else { 'SmartHireAI_Local_JWT_Secret_2026_09_13_ChangeInProduction_123456' }
$env:APP_CORS_ALLOWED_ORIGINS = if ($env:APP_CORS_ALLOWED_ORIGINS) { $env:APP_CORS_ALLOWED_ORIGINS } else { 'http://localhost:5500,http://127.0.0.1:5500' }
$env:APP_FRONTEND_BASE_URL = if ($env:APP_FRONTEND_BASE_URL) { $env:APP_FRONTEND_BASE_URL } else { 'http://localhost:5500' }
if ($env:APP_JWT_SECRET.Length -lt 32) { throw 'APP_JWT_SECRET must be at least 32 characters.' }

function Test-Url([string]$url) {
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
        return ($r.StatusCode -ge 200 -and $r.StatusCode -lt 300)
    } catch { return $false }
}

function Get-PsqlExe {
    $candidates = @(
        'C:\Program Files\PostgreSQL\18\bin\psql.exe',
        'C:\Program Files\PostgreSQL\17\bin\psql.exe',
        (Get-Command psql.exe -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -First 1)
    ) | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
    if (-not $candidates) { throw 'PostgreSQL psql.exe was not found. PostgreSQL 18 command-line tools are required.' }
    return $candidates
}

function Test-PostgresCredential([string]$password, [string]$psqlPath) {
    if ([string]::IsNullOrWhiteSpace($password)) { return $false }
    $old = $env:PGPASSWORD
    $env:PGPASSWORD = $password
    try {
        & $psqlPath -h localhost -p 5432 -U $env:SPRING_DATASOURCE_USERNAME -d postgres -c 'SELECT 1;' -q -t -A 2>$null | Out-Null
        return ($LASTEXITCODE -eq 0)
    } finally { $env:PGPASSWORD = $old }
}

function Load-SavedPassword {
    if (-not (Test-Path $securePasswordFile)) { return $null }
    try {
        $secure = Get-Content -LiteralPath $securePasswordFile -Raw | ConvertTo-SecureString
        $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
    } catch { return $null }
}

function Save-SecurePassword([string]$password) {
    $secure = ConvertTo-SecureString $password -AsPlainText -Force
    $secure | ConvertFrom-SecureString | Set-Content -LiteralPath $securePasswordFile -Encoding UTF8
}

# 1) PostgreSQL service
$pg = Get-Service -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -like 'postgresql-x64-*' } |
    Sort-Object Name -Descending |
    Select-Object -First 1
if (-not $pg) { throw 'No PostgreSQL Windows service was found. Install PostgreSQL 18 first.' }
if ($pg.Status -ne 'Running') {
    try { Start-Service -Name $pg.Name -ErrorAction Stop; Start-Sleep -Seconds 2 }
    catch { throw "PostgreSQL service $($pg.Name) could not be started. Run PowerShell as Administrator once." }
}
if ((Get-Service -Name $pg.Name).Status -ne 'Running') { throw "PostgreSQL service $($pg.Name) is not running." }
Write-Host "[OK] PostgreSQL service: $($pg.Name)" -ForegroundColor Green

# 2) PostgreSQL connectivity and credentials
$pgIsReady = 'C:\Program Files\PostgreSQL\18\bin\pg_isready.exe'
if (-not (Test-Path $pgIsReady)) { $pgIsReady = 'C:\Program Files\PostgreSQL\17\bin\pg_isready.exe' }
if (Test-Path $pgIsReady) {
    & $pgIsReady -h localhost -p 5432 2>$null | Write-Host
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL service is running but localhost:5432 is not accepting connections.' }
} else { Write-Warning 'pg_isready.exe not found; continuing because the Windows service is running.' }

$psql = Get-PsqlExe
$validPassword = $null
$candidate = $PostgresPassword
if (-not $candidate) { $candidate = Load-SavedPassword }
if ($candidate -and (Test-PostgresCredential $candidate $psql)) { $validPassword = $candidate }

if (-not $validPassword) {
    Write-Host ''
    Write-Host 'First-time local setup: enter the PostgreSQL 18 password for user postgres.' -ForegroundColor Yellow
    $secure = Read-Host 'PostgreSQL password' -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { $entered = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
    if (-not (Test-PostgresCredential $entered $psql)) { throw 'PostgreSQL authentication failed. The entered password is incorrect.' }
    $validPassword = $entered
    Save-SecurePassword $validPassword
    Write-Host '[OK] PostgreSQL password verified and stored securely for this Windows user.' -ForegroundColor Green
} else {
    Write-Host '[OK] Saved PostgreSQL credentials verified.' -ForegroundColor Green
}

$env:SPRING_DATASOURCE_PASSWORD = $validPassword
$env:PGPASSWORD = $validPassword
$dbExists = (& $psql -h localhost -p 5432 -U $env:SPRING_DATASOURCE_USERNAME -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='smarthire';" 2>$null)
if (($dbExists | Out-String).Trim() -ne '1') {
    Write-Host '[CREATE] Creating database smarthire...' -ForegroundColor Yellow
    & $psql -h localhost -p 5432 -U $env:SPRING_DATASOURCE_USERNAME -d postgres -c 'CREATE DATABASE smarthire;' | Write-Host
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the smarthire database.' }
}
Write-Host '[OK] Database smarthire is available.' -ForegroundColor Green
$env:PGPASSWORD = ''

# 3) Start AI services (strictly isolated Python environments)
$aiScript = Join-Path $root 'START-AI-SERVICES.ps1'
if (Test-Path $aiScript) {
    Write-Host ''
    Write-Host 'Starting isolated AI services...' -ForegroundColor Cyan
    & $aiScript
    if ($LASTEXITCODE -ne 0) {
        Write-Warning 'One or more AI services encountered issues during launch. Check logs/ directory.'
    }
} else {
    Write-Warning 'START-AI-SERVICES.ps1 not found; AI services will not start.'
}

# 4) Start Backend and Frontend
function Start-Window([string]$title, [string]$dir, [string]$command) {
    Write-Host "[START] $title" -ForegroundColor Yellow
    $child = "Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force; Set-Location -LiteralPath '$dir'; Write-Host '$title' -ForegroundColor Cyan; $command"
    Start-Process powershell -ArgumentList @('-NoExit','-Command',$child) | Out-Null
}

if (-not (Test-Path (Join-Path $backend 'mvnw.cmd'))) { throw "Backend Maven wrapper not found: $backend\mvnw.cmd" }
Start-Window 'SmartHire Spring Boot Backend :8080' $backend '.\mvnw.cmd spring-boot:run'

if (-not (Get-Command python.exe -ErrorAction SilentlyContinue)) { throw 'Python was not found on PATH.' }
Start-Window 'SmartHire Frontend :5500' $root 'python -m http.server 5500 --bind 127.0.0.1'

Write-Host ''
Write-Host 'Waiting for all services to become ready...' -ForegroundColor Yellow
$checks = @(
    @{Name='PostgreSQL'; Kind='pg'; Port=5432; Url=''},
    @{Name='Backend'; Kind='http'; Port=8080; Url='http://127.0.0.1:8080/api/health'},
    @{Name='CNN'; Kind='http'; Port=8095; Url='http://127.0.0.1:8095/health'},
    @{Name='MediaPipe'; Kind='http'; Port=8093; Url='http://127.0.0.1:8093/health'},
    @{Name='Object Detection'; Kind='http'; Port=8094; Url='http://127.0.0.1:8094/health'},
    @{Name='DeepFace'; Kind='http'; Port=8092; Url='http://127.0.0.1:8092/health'},
    @{Name='Whisper'; Kind='http'; Port=8091; Url='http://127.0.0.1:8091/health'},
    @{Name='Frontend'; Kind='http'; Port=5500; Url='http://127.0.0.1:5500'}
)

# Robust polling up to 90 seconds
$deadline = (Get-Date).AddSeconds(90)
$allReady = $false
while ((Get-Date) -lt $deadline) {
    $pending = 0
    foreach ($c in $checks) {
        if ($c.Kind -eq 'http') {
            if (-not (Test-Url $c.Url)) { $pending++ }
        }
    }
    if ($pending -eq 0) {
        $allReady = $true
        break
    }
    Start-Sleep -Seconds 2
}

Write-Host ''
Write-Host '================ FINAL LOCAL STACK ================' -ForegroundColor Cyan
foreach ($c in $checks) {
    if ($c.Kind -eq 'pg') {
        $env:PGPASSWORD = $validPassword
        & $psql -h localhost -p 5432 -U $env:SPRING_DATASOURCE_USERNAME -d postgres -c 'SELECT 1;' -q -t -A 2>$null | Out-Null
        $ok = ($LASTEXITCODE -eq 0)
        $env:PGPASSWORD = ''
    } else {
        $ok = Test-Url $c.Url
    }
    if ($ok) { Write-Host ("[OK]   {0,-18} :{1}" -f $c.Name,$c.Port) -ForegroundColor Green }
    else { Write-Host ("[DOWN] {0,-18} :{1}" -f $c.Name,$c.Port) -ForegroundColor Red }
}
Write-Host ''
Write-Host 'Open:    http://127.0.0.1:5500' -ForegroundColor Green
Write-Host 'Backend: http://127.0.0.1:8080' -ForegroundColor Green
Write-Host 'CNN:     http://127.0.0.1:8095/health' -ForegroundColor Green
Write-Host 'Whisper: http://127.0.0.1:8091/health' -ForegroundColor Green
Write-Host 'One-time credential file is DPAPI-protected for this Windows user.' -ForegroundColor DarkYellow
