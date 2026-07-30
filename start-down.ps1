$services = @(
    @{ Name = "user-service"; Port = 8081 },
    @{ Name = "product-service"; Port = 8083 },
    @{ Name = "order-service"; Port = 8082 },
    @{ Name = "notification-service"; Port = 8084 },
    @{ Name = "payment-service"; Port = 8085 },
    @{ Name = "api-gateway"; Port = 8080 }
)

Write-Host "Scanning for services that are currently down..." -ForegroundColor Cyan

$startedServices = @()

foreach ($svc in $services) {
    $name = $svc.Name
    $port = $svc.Port
    
    $isUp = $false
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:$port/actuator/health" -Method Get -ErrorAction Stop -TimeoutSec 2
        if ($response.status -eq "UP") {
            $isUp = $true
        }
    } catch {
        # Connection refused or timeout means it's down
    }

    if ($isUp) {
        Write-Host "[ALREADY RUNNING] $name (port $port) is healthy." -ForegroundColor Green
    } else {
        Write-Host "[STARTING] $name is down. Starting it on port $port..." -ForegroundColor Yellow
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$host.UI.RawUI.WindowTitle = '$name'; cd $name; mvn clean compile spring-boot:run"
        $startedServices += $svc
    }
}

if ($startedServices.Count -eq 0) {
    Write-Host ""
    Write-Host "All services are already running perfectly!" -ForegroundColor Cyan
    exit
}

Write-Host ""
Write-Host "Waiting for newly started services to become healthy..." -ForegroundColor Cyan

foreach ($svc in $startedServices) {
    $name = $svc.Name
    $port = $svc.Port
    
    $isUp = $false
    $timeoutSeconds = 90
    $startTime = Get-Date

    Write-Host "Waiting for $name " -NoNewline

    while (($startTime.AddSeconds($timeoutSeconds) -gt (Get-Date)) -and ($isUp -eq $false)) {
        try {
            $response = Invoke-RestMethod -Uri "http://localhost:$port/actuator/health" -Method Get -ErrorAction Stop
            if ($response.status -eq "UP") {
                $isUp = $true
            }
        } catch {
            # Still starting
        }

        if (-not $isUp) {
            Write-Host "." -NoNewline
            Start-Sleep -Seconds 2
        }
    }
    
    Write-Host ""
    
    if ($isUp) {
        Write-Host "[SUCCESS] $name is now healthy!" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] $name failed to start within $timeoutSeconds seconds." -ForegroundColor Red
    }
}

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "All missing services have been processed!" -ForegroundColor Cyan
