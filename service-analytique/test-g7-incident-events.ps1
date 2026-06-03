# SGITU Incident Management -> G8 integration test.
# Start G8, then start service-gestion-incidents separately, then run:
#   powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g7-incident-events.ps1

param([int]$TimeoutSeconds = 120)

. (Join-Path $PSScriptRoot "test-sender-common.ps1")

$repoRoot = Resolve-RepoRoot
Set-Location $repoRoot
$g8Headers = Get-G8AuthHeaders $repoRoot
$envValues = Read-DotEnv (Join-Path $repoRoot ".env")
$secret = $envValues["JWT_SECRET"]
if (-not $secret) {
    $secret = "sgitu_g8_secret_key_2025_very_long_secret_for_analytics"
}
$incidentHeaders = @{
    Authorization = "Bearer $(New-Hs256Jwt -Secret $secret -Subject "g8-incident-stage4" -Roles @("ROLE_PASSENGER","ROLE_SUPERVISOR","ROLE_DISPATCHER"))"
    "X-User-Id" = "9001"
    "X-User-Role" = "ROLE_PASSENGER"
}

$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$baseUrl = "http://localhost:8089/api/incidents"
$topic = "incident.analytique.topic"

Write-Host "SGITU Incident Management -> G8 integration test" -ForegroundColor Yellow
Write-Host "This script assumes G8 is running from the root compose and service-gestion-incidents is running separately on localhost:8089."

Write-Step "Service readiness"
$checks = [ordered]@{
    "sgitu-kafka" = "Kafka"
    "g8-mongo" = "G8 Mongo"
    "g8-analytics-service" = "G8 Analytics"
}
foreach ($container in $checks.Keys) {
    Add-Result "$($checks[$container]) container is ready" (Wait-ContainerHealthy -ContainerName $container -Timeout $TimeoutSeconds) $container
}
Add-Result "G8 health endpoint is reachable" (Wait-HttpOk -Url "http://localhost:8088/actuator/health" -Timeout $TimeoutSeconds)

Write-Step "Trigger a real incident action"
$incidentId = $null
try {
    $incidentBody = @{
        type = "ACCIDENT"
        description = "G8 Stage 4 analytics incident probe $runId"
        dateIncident = ([DateTime]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ss"))
        latitude = 33.5731
        longitude = -7.5898
        vehiculeId = "VEH-G8-$runId"
        ligneTransport = "L-STAGE4"
        preuves = @()
        role = "ROLE_PASSENGER"
    } | ConvertTo-Json -Depth 10 -Compress

    $incident = Invoke-RestMethod -Uri "$baseUrl/signaler" -Method Post -ContentType "application/json" -Headers $incidentHeaders -Body $incidentBody -TimeoutSec 45
    $incidentId = "$($incident.id)"
    if ([string]::IsNullOrWhiteSpace($incidentId)) {
        $incidentId = "$($incident.incidentId)"
    }
    Add-Result "Incident signalement endpoint was called" (-not [string]::IsNullOrWhiteSpace($incidentId)) ($incident | ConvertTo-Json -Compress -Depth 8)
} catch {
    Add-Result "Incident signalement endpoint was called" $false $_.Exception.Message
}

Write-Step "Verify Incident -> Kafka -> G8"
$rawKafka = Read-KafkaTopicQuiet -Topic $topic -MaxMessages 80 -TimeoutMs 8000
$eventPublished = ($incidentId -and $rawKafka.Text -match [regex]::Escape($incidentId)) -or ($rawKafka.Text -match [regex]::Escape("G8 Stage 4 analytics incident probe $runId"))
Add-Result "Incident service published to $topic" $eventPublished "incidentId=$incidentId"

$g8Stored = Wait-MongoCondition "db.incoming_events.countDocuments({sourceType:'INCIDENT', `$or:[{'payload.incidentId':'$incidentId'},{'payload.id':'$incidentId'},{'payload.description':/G8 Stage 4 analytics incident probe $runId/}]})" { param($value) [int]$value -gt 0 } 75
Add-Result "G8 stored the incident event" $g8Stored "incidentId=$incidentId"

Write-Step "Run G8 analytics"
try {
    Invoke-G8Run -Headers $g8Headers
    Add-Result "Manual analytics job completed" $true
} catch {
    Add-Result "Manual analytics job completed" $false $_.Exception.Message
}
$incExists = Wait-MongoCondition "db.stat_snapshots.countDocuments({statId:'INC_TOTAL'})" { param($value) [int]$value -gt 0 } 45
Add-Result "Incident impacted INC_TOTAL snapshot" $incExists

try {
    $prometheus = (Invoke-WebRequest -Uri "http://localhost:8088/actuator/prometheus" -Headers $g8Headers -UseBasicParsing -TimeoutSec 15).Content
    if ($prometheus -match 'sgitu_alerts_triggered_total\{alert_type="INCIDENT_ZONE_RISK"') {
        Write-Host "[INFO] INCIDENT_ZONE_RISK counter is present in Prometheus output." -ForegroundColor Green
    } else {
        Write-Host "[INFO] No INCIDENT_ZONE_RISK counter observed. That is acceptable unless this run intentionally created a repeated incident zone." -ForegroundColor Yellow
    }
} catch {
    Write-Host "[INFO] Could not read Prometheus alert counters: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Step "Diagnosis"
if (-not $eventPublished) {
    Write-Host "[DIAGNOSIS] The incident endpoint did not produce a matching incident.analytique.topic event. Check service-gestion-incidents Kafka bootstrap; from host it should use localhost:29093, from the compose network kafka:9092." -ForegroundColor Yellow
} elseif (-not $g8Stored) {
    Write-Host "[DIAGNOSIS] Incident service published, but G8 did not persist. Check payload field names and G8 incident consumer logs." -ForegroundColor Yellow
} else {
    Write-Host "[OK] Incident service published and G8 persisted the event." -ForegroundColor Green
}

Complete-Script -UsefulLogs @(
    "docker logs g8-analytics-service --tail=150",
    "docker exec sgitu-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group g8-analytics-group"
)
