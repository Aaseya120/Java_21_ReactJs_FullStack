$services = @(
    @{ Name = "user-service"; Port = 8081 },
    @{ Name = "product-service"; Port = 8083 },
    @{ Name = "order-service"; Port = 8082 },
    @{ Name = "notification-service"; Port = 8084 },
    @{ Name = "payment-service"; Port = 8085 },
    @{ Name = "api-gateway"; Port = 8080 }
)

Write-Host "Starting all microservices sequentially with health checks..." -ForegroundColor Cyan

foreach ($svc in $services) {
    $name = $svc.Name
    $port = $svc.Port
    
    Write-Host ""
    Write-Host "=============================================" -ForegroundColor Cyan
    Write-Host "Starting $name on port $port..." -ForegroundColor Yellow
    
    # Opens a new PowerShell window for each service with a custom window title
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$host.UI.RawUI.WindowTitle = '$name'; cd $name; mvn spring-boot:run"
    
    # Poll for health
    $isUp = $false
    $timeoutSeconds = 90
    $startTime = Get-Date

    Write-Host "Waiting for $name to become healthy" -NoNewline

    while (($startTime.AddSeconds($timeoutSeconds) -gt (Get-Date)) -and ($isUp -eq $false)) {
        try {
            # Check the Spring Boot Actuator health endpoint
            $response = Invoke-RestMethod -Uri "http://localhost:$port/actuator/health" -Method Get -ErrorAction Stop
            if ($response.status -eq "UP") {
                $isUp = $true
            }
        } catch {
            # Still starting up, ignore the connection refused errors
        }

        if (-not $isUp) {
            Write-Host "." -NoNewline
            Start-Sleep -Seconds 2
        }
    }
    
    Write-Host "" # New line
    
    if ($isUp) {
        Write-Host "[SUCCESS] $name started successfully and is healthy!" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] $name failed to start within $timeoutSeconds seconds. Check its terminal window for errors!" -ForegroundColor Red
    }
}

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "All services have been processed!" -ForegroundColor Cyan
