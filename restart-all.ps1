Write-Host "Restarting all microservices..." -ForegroundColor Cyan

# Run the stop script
.\stop-all.ps1

Write-Host "Waiting 3 seconds for ports to clear..." -ForegroundColor DarkGray
Start-Sleep -Seconds 3

# Run the start script
.\start-all.ps1
