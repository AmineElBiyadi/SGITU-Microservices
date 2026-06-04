# SGITU Vehicle Tracking -> G8 integration test.
# Start G8, then start g7-service + db-g7, then run:
#   powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g7-vehicle-events.ps1

param([int]$TimeoutSeconds = 120)

. (Join-Path $PSScriptRoot "test-sender-common.ps1")

$repoRoot = Resolve-RepoRoot
Set-Location $repoRoot
$g8Headers = Get-G8AuthHeaders $repoRoot
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$baseUrl = "http://localhost:8087"
$analyticsTopic = "g8.vehicule.status"
$positionTopic = "vehicule-positions"

$g7Headers = @{
    "X-User-Id" = "g7-stage4-test"
    "X-Roles" = "ROLE_ADMIN_G7"
    "Content-Type" = "application/json"
}

Write-Host "SGITU Vehicle Tracking -> G8 integration test" -ForegroundColor Yellow
Write-Host "This script assumes G8 and g7-service were started manually."

Write-Step "Service readiness"
$checks = [ordered]@{
    "sgitu-kafka" = "Kafka"
    "g8-mongo" = "G8 Mongo"
    "g8-analytics-service" = "G8 Analytics"
    "db-g7" = "Vehicle PostgreSQL"
    "g7-service" = "Vehicle tracking service"
}
foreach ($container in $checks.Keys) {
    Add-Result "$($checks[$container]) container is ready" (Wait-ContainerHealthy -ContainerName $container -Timeout $TimeoutSeconds) $container
}
Add-Result "G8 health endpoint is reachable" (Wait-HttpOk -Url "http://localhost:8088/actuator/health" -Timeout $TimeoutSeconds)
Add-Result "G7 health endpoint is reachable" (Wait-HttpOk -Url "http://localhost:8087/actuator/health" -Timeout $TimeoutSeconds)

Write-Step "Trigger real vehicle actions"
$vehicleId = $null
try {
    $vehicleBody = @{
        immatriculation = "G8-$runId"
        type = "BUS"
        ligne = "L-STAGE4"
    } | ConvertTo-Json -Depth 10 -Compress

    $vehicle = Invoke-RestMethod -Uri "$baseUrl/api/suivi-vehicules/vehicules" -Method Post -Headers $g7Headers -Body $vehicleBody -TimeoutSec 30
    $vehicleId = "$($vehicle.id)"
    Add-Result "Vehicle creation succeeds" (-not [string]::IsNullOrWhiteSpace($vehicleId)) ($vehicle | ConvertTo-Json -Compress -Depth 8)
} catch {
    Add-Result "Vehicle creation succeeds" $false $_.Exception.Message
}

if ($vehicleId) {
    try {
        $updated = Invoke-RestMethod -Uri "$baseUrl/api/suivi-vehicules/vehicules/$vehicleId/statut?statut=EN_SERVICE" -Method Put -Headers $g7Headers -TimeoutSec 30
        Add-Result "Vehicle status update succeeds" ($true) ($updated | ConvertTo-Json -Compress -Depth 8)
    } catch {
        Add-Result "Vehicle status update succeeds" $false $_.Exception.Message
    }

    try {
        $positionBody = @{
            vehiculeId = $vehicleId
            latitude = 33.5731
            longitude = -7.5898
            vitesse = 8.0
            cap = 180.0
        } | ConvertTo-Json -Depth 10 -Compress

        $position = Invoke-RestMethod -Uri "$baseUrl/api/suivi-vehicules/positions" -Method Post -Headers $g7Headers -Body $positionBody -TimeoutSec 30
        Add-Result "Vehicle GPS position succeeds" ($true) ($position | ConvertTo-Json -Compress -Depth 8)
    } catch {
        Add-Result "Vehicle GPS position succeeds" $false $_.Exception.Message
    }
}

Write-Step "Verify Vehicle -> Kafka -> G8"
$analyticsKafka = Read-KafkaTopicQuiet -Topic $analyticsTopic -MaxMessages 80 -TimeoutMs 8000
$analyticsPublished = $vehicleId -and ($analyticsKafka.Text -match [regex]::Escape($vehicleId))
Add-Result "Vehicle service published to $analyticsTopic" $analyticsPublished "vehicleId=$vehicleId"

$positionKafka = Read-KafkaTopicQuiet -Topic $positionTopic -MaxMessages 80 -TimeoutMs 6000
$positionPublished = $vehicleId -and ($positionKafka.Text -match [regex]::Escape($vehicleId))
Add-Result "Vehicle service published position event to $positionTopic" $positionPublished "This proves normal G7 telemetry, but G8 consumes $analyticsTopic."

$g8Stored = Wait-MongoCondition "db.incoming_events.countDocuments({sourceType:'VEHICLE', `$or:[{'payload.vehicleId':'$vehicleId'},{'payload.vehiculeId':'$vehicleId'}]})" { param($value) [int]$value -gt 0 } 75
Add-Result "G8 stored the vehicle event" $g8Stored "vehicleId=$vehicleId"

Write-Step "Run G8 analytics"
try {
    Invoke-G8Run -Headers $g8Headers
    Add-Result "Manual analytics job completed" $true
} catch {
    Add-Result "Manual analytics job completed" $false $_.Exception.Message
}
$vehExists = Wait-MongoCondition "db.stat_snapshots.countDocuments({statId:'VEH_PUNCTUALITY'})" { param($value) [int]$value -gt 0 } 45
Add-Result "Vehicle data impacted VEH_PUNCTUALITY snapshot" $vehExists

try {
    $prometheus = (Invoke-WebRequest -Uri "http://localhost:8088/actuator/prometheus" -Headers $g8Headers -UseBasicParsing -TimeoutSec 15).Content
    if ($prometheus -match 'sgitu_alerts_triggered_total\{alert_type="PUNCTUALITY_ALERT"') {
        Write-Host "[INFO] PUNCTUALITY_ALERT counter is present in Prometheus output." -ForegroundColor Green
    } else {
        Write-Host "[INFO] No PUNCTUALITY_ALERT counter observed. That is acceptable unless this run intentionally crossed the punctuality threshold." -ForegroundColor Yellow
    }
} catch {
    Write-Host "[INFO] Could not read Prometheus alert counters: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Step "Diagnosis"
if (-not $analyticsPublished -and $positionPublished) {
    Write-Host "[DIAGNOSIS] G7 produced normal position telemetry, but not g8.vehicule.status. The G8 producer method may not be wired into the standard vehicle/status endpoints." -ForegroundColor Yellow
} elseif ($analyticsPublished -and -not $g8Stored) {
    Write-Host "[DIAGNOSIS] G7 published to the G8 topic, but G8 did not persist. Check payload fields and G8 vehicle consumer logs." -ForegroundColor Yellow
} elseif ($g8Stored) {
    Write-Host "[OK] Vehicle tracking published and G8 persisted the event." -ForegroundColor Green
}

Complete-Script -UsefulLogs @(
    "docker logs g7-service --tail=150",
    "docker logs g8-analytics-service --tail=150",
    "docker exec sgitu-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group g8-analytics-group"
)
