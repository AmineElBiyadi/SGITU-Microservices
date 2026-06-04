# SGITU Subscription -> G8 integration test.
# Start G8, then start abonnement-service + db-abonnement, then run:
#   powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g2-subscription-events.ps1

param([int]$TimeoutSeconds = 120)

. (Join-Path $PSScriptRoot "test-sender-common.ps1")

$repoRoot = Resolve-RepoRoot
Set-Location $repoRoot
$g8Headers = Get-G8AuthHeaders $repoRoot
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$baseUrl = "http://localhost:8082"
$topic = "abonnement.souscription"
$userId = [int](900000 + ($runId % 100000))

$envValues = Read-DotEnv (Join-Path $repoRoot ".env")
$secret = $envValues["JWT_SECRET"]
if (-not $secret) { $secret = "sgitu_g8_secret_key_2025_very_long_secret_for_analytics" }
$g2AdminToken = New-Hs256Jwt -Secret $secret -Subject "g2-stage4-test" -Roles @("ROLE_ADMIN_G2")
$g2Headers = @{ Authorization = "Bearer $g2AdminToken"; "Content-Type" = "application/json" }

Write-Host "SGITU Subscription -> G8 integration test" -ForegroundColor Yellow
Write-Host "This script assumes G8 and abonnement-service were started manually."

Write-Step "Service readiness"
$checks = [ordered]@{
    "sgitu-kafka" = "Kafka"
    "g8-mongo" = "G8 Mongo"
    "g8-analytics-service" = "G8 Analytics"
    "db-abonnement" = "Subscription MySQL"
    "service-abonnement" = "Subscription service"
}
foreach ($container in $checks.Keys) {
    Add-Result "$($checks[$container]) container is ready" (Wait-ContainerHealthy -ContainerName $container -Timeout $TimeoutSeconds) $container
}
Add-Result "G8 health endpoint is reachable" (Wait-HttpOk -Url "http://localhost:8088/actuator/health" -Timeout $TimeoutSeconds)

Write-Step "Trigger a real subscription action"
$subscriptionId = $null
$planId = $null
try {
    $planBody = @{
        nomPlan = "G8 Stage4 Plan $runId"
        description = "Plan used by G8 Stage 4 sender integration tests"
        prix = 35.0
        duree = "MENSUEL"
        categorie = "ROLE_PASSENGER"
        transportType = "BUS"
        estActif = "ACTIF"
        maxDesactivation = 1
        minJoursEntreDesactivation = 1
        maxPeriodeDesactivation = 3
    } | ConvertTo-Json -Depth 10 -Compress

    $plan = Invoke-RestMethod -Uri "$baseUrl/plans" -Method Post -Headers $g2Headers -Body $planBody -TimeoutSec 30
    $planId = "$($plan.idPlan)"
    Add-Result "Subscription plan creation succeeds" (-not [string]::IsNullOrWhiteSpace($planId)) ($plan | ConvertTo-Json -Compress -Depth 8)
} catch {
    Add-Result "Subscription plan creation succeeds" $false $_.Exception.Message
}

if ($planId) {
    try {
        $created = Invoke-RestMethod -Uri "$baseUrl/abonnements/souscrire?userId=$userId&planId=$planId" -Method Post -Headers $g2Headers -TimeoutSec 45
        $subscriptionId = "$($created.id)"
        Add-Result "Subscription creation endpoint was called" (-not [string]::IsNullOrWhiteSpace($subscriptionId)) ($created | ConvertTo-Json -Compress -Depth 8)
    } catch {
        Add-Result "Subscription creation endpoint was called" $false $_.Exception.Message
    }
}

Write-Step "Verify Subscription -> Kafka -> G8"
$rawKafka = Read-KafkaTopicQuiet -Topic $topic -MaxMessages 80 -TimeoutMs 8000
$eventPublished = ($rawKafka.Text -match [regex]::Escape("$userId")) -or ($subscriptionId -and $rawKafka.Text -match [regex]::Escape($subscriptionId))
Add-Result "Subscription service published to $topic" $eventPublished "userId=$userId subscriptionId=$subscriptionId"

$g8Stored = Wait-MongoCondition "db.incoming_events.countDocuments({sourceType:'SUBSCRIPTION', `$or:[{'payload.userId':'USR-$userId'},{'payload.userId':'$userId'},{'payload.recipient.userId':'$userId'},{'payload.subscriptionId':'$subscriptionId'}]})" { param($value) [int]$value -gt 0 } 75
Add-Result "G8 stored the subscription event" $g8Stored "userId=$userId subscriptionId=$subscriptionId"

Write-Step "Run G8 analytics"
try {
    Invoke-G8Run -Headers $g8Headers
    Add-Result "Manual analytics job completed" $true
} catch {
    Add-Result "Manual analytics job completed" $false $_.Exception.Message
}
$subExists = Wait-MongoCondition "db.stat_snapshots.countDocuments({statId:'SUB_NEW'})" { param($value) [int]$value -gt 0 } 45
Add-Result "Subscription impacted SUB_NEW snapshot" $subExists

Write-Step "Diagnosis"
if (-not $eventPublished) {
    Write-Host "[DIAGNOSIS] The subscription action did not produce a matching abonnement.souscription event. Check service-abonnement dependencies on G3/G6 and its notification publisher logs." -ForegroundColor Yellow
} elseif (-not $g8Stored) {
    Write-Host "[DIAGNOSIS] Subscription published, but G8 did not persist. Check whether the payload is notification-shaped instead of analytics-shaped." -ForegroundColor Yellow
} else {
    Write-Host "[OK] Subscription published and G8 persisted the event." -ForegroundColor Green
}

Complete-Script -UsefulLogs @(
    "docker logs service-abonnement --tail=150",
    "docker logs g8-analytics-service --tail=150",
    "docker exec sgitu-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group g8-analytics-group"
)
