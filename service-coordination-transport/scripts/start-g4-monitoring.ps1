# Démarre G4 + Postgres + Kafka + Prometheus + Grafana (sans G5)
Set-Location $PSScriptRoot\..
Write-Host "G4 + monitoring (Prometheus 9090, Grafana 3000)..." -ForegroundColor Cyan
docker compose --profile monitoring up -d --build
if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "OK — URLs :" -ForegroundColor Green
    Write-Host "  G4 health    http://localhost:8084/api/g4/health"
    Write-Host "  G4 logs      http://localhost:8084/api/g4/logs"
    Write-Host "  Prometheus   http://localhost:9090/targets"
    Write-Host "  Grafana      http://localhost:3000  (admin / admin)"
}
