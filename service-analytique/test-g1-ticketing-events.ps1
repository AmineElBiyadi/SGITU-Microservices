# SGITU Ticketing -> G8 integration test.
# Start G8, then start service-billetterie + billetterie-mongo, then run:
#   powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g1-ticketing-events.ps1

param([int]$TimeoutSeconds = 120)

. (Join-Path $PSScriptRoot "test-sender-common.ps1")

$repoRoot = Resolve-RepoRoot
Set-Location $repoRoot
$g8Headers = Get-G8AuthHeaders $repoRoot
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$ticketBaseUrl = "http://localhost:8081"
$topic = "ticket.validated"

Write-Host "SGITU Ticketing -> G8 integration test" -ForegroundColor Yellow
Write-Host "This script assumes G8 and service-billetterie were started manually."

Write-Step "Service readiness"
$checks = [ordered]@{
    "sgitu-kafka" = "Kafka"
    "g8-mongo" = "G8 Mongo"
    "g8-analytics-service" = "G8 Analytics"
    "billetterie-mongo" = "Ticketing Mongo"
    "service-billetterie" = "Ticketing service"
}
foreach ($container in $checks.Keys) {
    Add-Result "$($checks[$container]) container is ready" (Wait-ContainerHealthy -ContainerName $container -Timeout $TimeoutSeconds) $container
}
Add-Result "G8 health endpoint is reachable" (Wait-HttpOk -Url "http://localhost:8088/actuator/health" -Timeout $TimeoutSeconds)

Write-Step "Trigger a real ticketing action"
$ticketId = $null
$tokenValue = $null
try {
    $createBody = @{
        tripId = "TRIP-G8-$runId"
        holderId = "USR-G8-$runId"
        price = 25.0
        currency = "MAD"
        ticketType = "ONE_WAY"
        ticketClass = "ORDINARY"
        identityMethod = "QR_CODE"
        rawPayload = "G8-$runId"
        expiresAt = ([DateTimeOffset]::UtcNow.AddHours(2).ToString("yyyy-MM-ddTHH:mm:ssZ"))
        metadata = @{ line = "L1"; stationId = "ST-G8" }
    } | ConvertTo-Json -Depth 10 -Compress

    $created = Invoke-RestMethod -Uri "$ticketBaseUrl/api/v1/tickets" -Method Post -ContentType "application/json" -Body $createBody -TimeoutSec 30
    $ticketId = "$($created.id)"
    $tokenValue = "$($created.tokenValue)"
    Add-Result "Ticket creation succeeds" (-not [string]::IsNullOrWhiteSpace($ticketId)) ($created | ConvertTo-Json -Compress -Depth 8)
} catch {
    Add-Result "Ticket creation succeeds" $false $_.Exception.Message
}

if ($ticketId) {
    try {
        $validateBody = @{
            ticketId = $ticketId
            tokenValue = $tokenValue
            identityPayload = @{ rawPayload = "G8-$runId" }
        } | ConvertTo-Json -Depth 10 -Compress

        $validated = Invoke-RestMethod -Uri "$ticketBaseUrl/api/v1/tickets/$ticketId/validate" -Method Post -ContentType "application/json" -Body $validateBody -TimeoutSec 30
        Add-Result "Ticket validation endpoint was called" ($true) ($validated | ConvertTo-Json -Compress -Depth 8)
    } catch {
        Add-Result "Ticket validation endpoint was called" $false $_.Exception.Message
    }
}

Write-Step "Verify Ticketing -> Kafka -> G8"
$rawKafka = Read-KafkaTopicQuiet -Topic $topic -MaxMessages 80 -TimeoutMs 8000
$ticketPublished = $ticketId -and ($rawKafka.Text -match [regex]::Escape($ticketId))
Add-Result "Ticketing published to $topic" $ticketPublished "ticketId=$ticketId"

$g8Stored = $false
if ($ticketId) {
    $g8Stored = Wait-MongoCondition "db.incoming_events.countDocuments({'payload.ticketId':'$ticketId', sourceType:'TICKETING'})" { param($value) [int]$value -gt 0 } 75
}
Add-Result "G8 stored the ticketing event" $g8Stored "ticketId=$ticketId"

Write-Step "Run G8 analytics"
try {
    Invoke-G8Run -Headers $g8Headers
    Add-Result "Manual analytics job completed" $true
} catch {
    Add-Result "Manual analytics job completed" $false $_.Exception.Message
}
$freqExists = Wait-MongoCondition "db.stat_snapshots.countDocuments({statId:'FREQ_TOTAL_VALIDATIONS'})" { param($value) [int]$value -gt 0 } 45
Add-Result "Ticketing impacted FREQ_TOTAL_VALIDATIONS snapshot" $freqExists

Write-Step "Diagnosis"
if (-not $ticketPublished) {
    Write-Host "[DIAGNOSIS] Ticketing action completed or was attempted, but no matching ticket.validated event was found. Check ticket status/payment prerequisites and service-billetterie logs." -ForegroundColor Yellow
} elseif (-not $g8Stored) {
    Write-Host "[DIAGNOSIS] Ticketing published, but G8 did not persist. Check G8 topic config for KAFKA_TOPIC_TICKETING and consumer logs." -ForegroundColor Yellow
} else {
    Write-Host "[OK] Ticketing published and G8 persisted the event." -ForegroundColor Green
}

Complete-Script -UsefulLogs @(
    "docker logs service-billetterie --tail=150",
    "docker logs g8-analytics-service --tail=150",
    "docker exec sgitu-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group g8-analytics-group"
)
