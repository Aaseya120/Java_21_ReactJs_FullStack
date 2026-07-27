$ports = 8080, 8081, 8082, 8083, 8084

Write-Host "Stopping all microservices..." -ForegroundColor Cyan

foreach ($port in $ports) {
    # Find any process listening on the microservice ports
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    
    if ($connections) {
        foreach ($conn in $connections) {
            $pidToKill = $conn.OwningProcess
            try {
                $processName = (Get-Process -Id $pidToKill).Name
                Write-Host "Found $processName (PID: $pidToKill) running on port $port. Stopping it..." -ForegroundColor Yellow
                Stop-Process -Id $pidToKill -Force -ErrorAction Stop
                Write-Host "Successfully stopped service on port $port." -ForegroundColor Green
            } catch {
                Write-Host "Could not stop process $pidToKill on port $port. It may have already exited." -ForegroundColor DarkGray
            }
        }
    } else {
        Write-Host "No service running on port $port." -ForegroundColor DarkGray
    }
}

Write-Host "All services stopped!" -ForegroundColor Cyan
