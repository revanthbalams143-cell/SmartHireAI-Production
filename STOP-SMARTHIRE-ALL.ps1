$ErrorActionPreference = 'SilentlyContinue'
$ports = 5500, 8080, 8091, 8092, 8093, 8094, 8095
foreach ($port in $ports) {
    $pids = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | Where-Object { $_ -and $_ -gt 4 }
    foreach ($p in $pids) {
        Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
    }
}
Write-Host 'SmartHire local application processes stopped. PostgreSQL service was not stopped.' -ForegroundColor Green
