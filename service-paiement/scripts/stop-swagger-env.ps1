$ErrorActionPreference = "SilentlyContinue"

foreach ($port in @(18081, 18086)) {
    Get-NetTCPConnection -LocalPort $port -State Listen |
            ForEach-Object {
                Write-Host "Stopping process $($_.OwningProcess) on port $port"
                Stop-Process -Id $_.OwningProcess -Force
            }
}

Write-Host "Local Swagger services stopped. Docker DB/Kafka/Mongo containers were left running."
