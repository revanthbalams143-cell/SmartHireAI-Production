$root = if ($PSScriptRoot) { $PSScriptRoot } elseif ($MyInvocation.MyCommand.Path) { Split-Path -Parent $MyInvocation.MyCommand.Path } else { (Get-Location).Path }
$services = @(
    @{ Name="whisper"; Port=8091; Health="http://127.0.0.1:8091/health" },
    @{ Name="deepface"; Port=8092; Health="http://127.0.0.1:8092/health" },
    @{ Name="mediapipe"; Port=8093; Health="http://127.0.0.1:8093/health" },
    @{ Name="object-detection"; Port=8094; Health="http://127.0.0.1:8094/health" },
    @{ Name="emotion-cnn"; Port=8095; Health="http://127.0.0.1:8095/health" }
)

Write-Host "==========================================================================================================" -ForegroundColor Cyan
Write-Host "                             SmartHire AI - Runtime Isolation & Health Verification                       " -ForegroundColor Cyan
Write-Host " Project: $root" -ForegroundColor Cyan
Write-Host "==========================================================================================================" -ForegroundColor Cyan
Write-Host ""

$results = @()
$allPassed = $true

foreach ($svc in $services) {
    $port = $svc.Port
    $name = $svc.Name
    $expectedVenv = Join-Path $root ".venvs\$name\Scripts\python.exe"

    # 1. Health check
    $healthStatus = "DOWN"
    $healthPayload = ""
    try {
        $healthRes = Invoke-RestMethod -Uri $svc.Health -TimeoutSec 6 -ErrorAction Stop
        $healthStatus = "HEALTHY"
        $healthPayload = ($healthRes | ConvertTo-Json -Compress)
    } catch {
        $healthStatus = "DOWN"
        $healthPayload = $_.Exception.Message
        $allPassed = $false
    }

    # 2. Process check & duplicates
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    $pids = $conns | Select-Object -ExpandProperty OwningProcess -Unique | Where-Object { $_ -gt 0 }
    $primaryPid = if ($pids.Count -gt 0) { $pids[0] } else { 0 }
    $duplicateCount = [Math]::Max(0, $pids.Count - 1)

    $proc = if ($primaryPid -gt 0) { Get-CimInstance Win32_Process -Filter "ProcessId=$primaryPid" -ErrorAction SilentlyContinue } else { $null }
    $cmdLine = if ($proc) { $proc.CommandLine } else { "" }

    # 3. Interpreter check
    $pythonType = "None"
    $isIsolated = $false
    if ($cmdLine) {
        if ($cmdLine -match [regex]::Escape(".venvs\$name\Scripts\python.exe") -or $cmdLine -match [regex]::Escape(".venvs/$name/Scripts/python.exe")) {
            $pythonType = ".venv\$name"
            $isIsolated = $true
        } elseif ($cmdLine -match "(?i)Programs\\Python\\Python3" -or $cmdLine -match "(?i)WindowsApps\\python") {
            $pythonType = "GLOBAL"
            $isIsolated = $false
            $allPassed = $false
        } else {
            $pythonType = "Other"
            $isIsolated = ($cmdLine -match "\.venvs")
        }
    }

    if ($duplicateCount -gt 0) {
        $allPassed = $false
    }

    $passFail = if ($healthStatus -eq "HEALTHY" -and $isIsolated -and $duplicateCount -eq 0) { "PASS" } else { "FAIL" }
    if ($passFail -ne "PASS") { $allPassed = $false }

    $results += [PSCustomObject]@{
        SERVICE    = $name
        PORT       = $port
        PID        = if ($primaryPid -gt 0) { $primaryPid } else { "-" }
        HEALTH     = $healthStatus
        PYTHON     = $pythonType
        ISOLATED   = if ($isIsolated) { "YES" } else { "NO" }
        DUPLICATES = $duplicateCount
        RESULT     = $passFail
    }
}

# Print formatted table
$results | Format-Table -Property SERVICE, PORT, PID, HEALTH, PYTHON, ISOLATED, DUPLICATES, RESULT -AutoSize | Out-String | Write-Host

Write-Host "Detailed Service Health Responses:" -ForegroundColor Cyan
foreach ($svc in $services) {
    try {
        $r = Invoke-RestMethod -Uri $svc.Health -TimeoutSec 4 -ErrorAction Stop
        Write-Host "[$($svc.Port) - $($svc.Name)] -> $($r | ConvertTo-Json -Compress)" -ForegroundColor Green
    } catch {
        Write-Host "[$($svc.Port) - $($svc.Name)] -> DOWN: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "----------------------------------------------------------------------------------------------------------" -ForegroundColor Cyan
if ($allPassed) {
    Write-Host ">>> VERIFICATION RESULT: ALL 5 AI SERVICES ARE HEALTHY, PROPERLY ISOLATED & UNIQUE! <<<" -ForegroundColor Green
} else {
    Write-Host ">>> VERIFICATION RESULT: ONE OR MORE SERVICES FAILED HEALTH OR ISOLATION CHECK. <<<" -ForegroundColor Red
    exit 1
}
Write-Host "----------------------------------------------------------------------------------------------------------" -ForegroundColor Cyan
