param(
    [switch]$RebuildEnvironments
)
$ErrorActionPreference = "Stop"

$root = if ($PSScriptRoot) { $PSScriptRoot } elseif ($MyInvocation.MyCommand.Path) { Split-Path -Parent $MyInvocation.MyCommand.Path } else { (Get-Location).Path }
$logsDir = Join-Path $root "logs"
$venvRoot = Join-Path $root ".venvs"
if (-not (Test-Path $logsDir)) { New-Item -ItemType Directory -Path $logsDir -Force | Out-Null }
if (-not (Test-Path $venvRoot)) { New-Item -ItemType Directory -Path $venvRoot -Force | Out-Null }

function Test-Health([string]$url) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 4
        return ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400)
    } catch { return $false }
}

function Get-VenvPython([string]$name) {
    return (Join-Path $venvRoot (Join-Path $name "Scripts\python.exe"))
}

function Find-BasePython {
    $candidates = @(
        (Get-Command python.exe -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -First 1),
        (Get-Command py.exe -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -First 1),
        "C:\Users\$env:USERNAME\AppData\Local\Programs\Python\Python313\python.exe",
        "C:\Program Files\Python313\python.exe"
    ) | Where-Object { $_ -and (Test-Path $_) }
    $found = $candidates | Select-Object -First 1
    if (-not $found) { throw "Python 3.13 was not found. Install Python 3.13 and ensure python.exe is available on PATH." }
    return $found
}

function Ensure-Venv([string]$name, [string]$requirementsPath) {
    $venvDir = Join-Path $venvRoot $name
    $venvPython = Get-VenvPython $name

    if ($RebuildEnvironments -and (Test-Path $venvDir)) {
        Write-Host "[$name] removing old isolated environment..." -ForegroundColor Yellow
        Remove-Item -LiteralPath $venvDir -Recurse -Force -ErrorAction Stop
    }

    if (-not (Test-Path $venvPython)) {
        Write-Host "[$name] creating isolated Python environment..." -ForegroundColor Yellow
        $basePython = Find-BasePython
        & $basePython -m venv $venvDir
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path $venvPython)) { throw "Could not create Python venv for $name at $venvDir" }
    }

    if (-not (Test-Path $requirementsPath)) { throw "requirements.txt not found: $requirementsPath" }

    $marker = Join-Path $venvDir ".smarthire-requirements-installed"
    $reqHash = (Get-FileHash -Algorithm SHA256 $requirementsPath).Hash
    $needsInstall = $true
    if (Test-Path $marker) {
        $saved = (Get-Content -LiteralPath $marker -Raw).Trim()
        if ($saved -eq $reqHash) { $needsInstall = $false }
    }

    if ($needsInstall) {
        Write-Host "[$name] installing dependencies into isolated venv..." -ForegroundColor Yellow
        & $venvPython -m pip install --disable-pip-version-check --upgrade pip setuptools wheel --prefer-binary
        if ($LASTEXITCODE -ne 0) { throw "pip bootstrap failed for $name" }

        & $venvPython -m pip install --disable-pip-version-check --prefer-binary -r $requirementsPath
        if ($LASTEXITCODE -ne 0) { throw "Dependency installation failed for $name : $requirementsPath" }
        Set-Content -LiteralPath $marker -Value $reqHash -Encoding ASCII
    } else {
        Write-Host "[$name] isolated environment already prepared." -ForegroundColor DarkGray
    }

    return $venvPython
}

function Stop-StalePort([int]$port) {
    $conns = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    $pids = $conns | Select-Object -ExpandProperty OwningProcess -Unique | Where-Object { $_ -and $_ -gt 4 }
    foreach ($pidToStop in $pids) {
        $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$pidToStop" -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "[port $port] stopping stale process $pidToStop ($($proc.Name))..." -ForegroundColor Yellow
            Stop-Process -Id $pidToStop -Force -ErrorAction SilentlyContinue
        }
    }
    Start-Sleep -Milliseconds 500
}

function Start-Service([string]$name, [int]$port, [string]$workingDir, [string]$venvPython, [string]$checkUrl) {
    # Check if already running healthy on the correct isolated venv
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) {
        $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$($conn.OwningProcess)" -ErrorAction SilentlyContinue
        $cmdLine = if ($proc) { $proc.CommandLine } else { "" }
        $isCurrentVenv = $cmdLine -match [regex]::Escape(".venvs\$name\Scripts\python.exe") -or $cmdLine -match [regex]::Escape(".venvs/$name/Scripts/python.exe")
        if ($isCurrentVenv -and (Test-Health $checkUrl)) {
            Write-Host "[$name] already running and healthy on port $port (PID: $($conn.OwningProcess))" -ForegroundColor Green
            return
        }
    }

    Stop-StalePort $port

    if (-not (Test-Path -LiteralPath $venvPython)) {
        throw "Isolated Python executable not found: $venvPython"
    }
    if (-not (Test-Path -LiteralPath $workingDir)) {
        throw "Service directory not found: $workingDir"
    }

    $outFile = Join-Path $logsDir "$name.out.log"
    $errFile = Join-Path $logsDir "$name.err.log"
    
    # Initialize / clean log files
    Set-Content -Path $outFile -Value "" -Encoding UTF8 -Force
    Set-Content -Path $errFile -Value "" -Encoding UTF8 -Force

    Write-Host "[$name] starting isolated service on port $port..." -ForegroundColor Yellow

    # Launch python.exe directly from the isolated virtual environment
    $proc = Start-Process -FilePath $venvPython -ArgumentList @("-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "$port") -WorkingDirectory $workingDir -RedirectStandardOutput $outFile -RedirectStandardError $errFile -WindowStyle Hidden -PassThru

    $deadline = (Get-Date).AddSeconds(60)
    $ready = $false
    do {
        Start-Sleep -Milliseconds 800
        if ($proc.HasExited) {
            $exitCode = $proc.ExitCode
            $errContent = if (Test-Path $errFile) { (Get-Content $errFile -Tail 15 -ErrorAction SilentlyContinue) -join "`n" } else { "No error log" }
            throw "[$name] process terminated unexpectedly (ExitCode: $exitCode). Log: $errContent"
        }
        if (Test-Health $checkUrl) {
            $ready = $true
            Write-Host "[$name] HEALTHY on port $port [PID: $($proc.Id)]" -ForegroundColor Green
            break
        }
    } while ((Get-Date) -lt $deadline)

    if (-not $ready) {
        $errTail = if (Test-Path $errFile) { (Get-Content $errFile -Tail 10 -ErrorAction SilentlyContinue) -join " | " } else { "" }
        if ($errTail) { Write-Warning "[$name] health check timed out ($port). Log: $errTail" }
        else { Write-Warning "[$name] health check timed out ($port). Check $outFile and $errFile" }
    }
}

$services = @(
    @{ Name="whisper"; Port=8091; Dir=(Join-Path $root "ai-services\whisper-service"); Health="http://127.0.0.1:8091/health" },
    @{ Name="deepface"; Port=8092; Dir=(Join-Path $root "ai-services\deepface-service"); Health="http://127.0.0.1:8092/health" },
    @{ Name="mediapipe"; Port=8093; Dir=(Join-Path $root "ai-services\mediapipe-service"); Health="http://127.0.0.1:8093/health" },
    @{ Name="object-detection"; Port=8094; Dir=(Join-Path $root "ai-services\object-detection-service"); Health="http://127.0.0.1:8094/health" },
    @{ Name="emotion-cnn"; Port=8095; Dir=(Join-Path $root "ai-services\emotion-cnn-service"); Health="http://127.0.0.1:8095/health" }
)

$hasError = $false
foreach ($svc in $services) {
    $req = Join-Path $svc.Dir "requirements.txt"
    try {
        $venvPython = Ensure-Venv $svc.Name $req
        Start-Service $svc.Name $svc.Port $svc.Dir $venvPython $svc.Health
    } catch {
        Write-Warning "[$($svc.Name)] startup failed: $($_.Exception.Message)"
        $hasError = $true
    }
}

Write-Host ""
Write-Host "================ AI SERVICES STATUS ================" -ForegroundColor Cyan
$allOk = $true
foreach ($svc in $services) {
    $venvPython = Get-VenvPython $svc.Name
    $ok = Test-Health $svc.Health
    $venvOk = Test-Path $venvPython
    
    $conn = Get-NetTCPConnection -LocalPort $svc.Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    $procInfo = if ($conn) { Get-CimInstance Win32_Process -Filter "ProcessId=$($conn.OwningProcess)" -ErrorAction SilentlyContinue } else { $null }
    $cmdLine = if ($procInfo) { $procInfo.CommandLine } else { "" }
    $isIsolated = $cmdLine -match [regex]::Escape(".venvs\$($svc.Name)\Scripts\python.exe") -or $cmdLine -match [regex]::Escape(".venvs/$($svc.Name)/Scripts/python.exe")

    if ($ok -and $venvOk -and $isIsolated) {
        Write-Host ("{0,-18} :{1,-5} [OK]   (PID: {2,5}) Isolated .venv" -f $svc.Name, $svc.Port, $conn.OwningProcess) -ForegroundColor Green
    } elseif ($ok) {
        Write-Host ("{0,-18} :{1,-5} [WARN] Responding but verify isolation" -f $svc.Name, $svc.Port) -ForegroundColor Yellow
        $allOk = $false
    } else {
        Write-Host ("{0,-18} :{1,-5} [DOWN]" -f $svc.Name, $svc.Port) -ForegroundColor Red
        $allOk = $false
    }
}
Write-Host "====================================================" -ForegroundColor Cyan

if (-not $allOk) {
    exit 1
}

