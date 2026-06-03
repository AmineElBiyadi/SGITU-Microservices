# SGITU Payment -> G8 integration test.
# Start G8, then start payment-service + g6-payment-db, then run:
#   powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g6-payment-events.ps1

param([int]$TimeoutSeconds = 120)

. (Join-Path $PSScriptRoot "test-sender-common.ps1")

$repoRoot = Resolve-RepoRoot
Set-Location $repoRoot
$g8Headers = Get-G8AuthHeaders $repoRoot
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$baseUrl = "http://localhost:8086"
$analyticsTopic = "payment.transaction.completed"
$notificationTopic = "payment.notification"
$userId = [int](800000 + ($runId % 100000))

Write-Host "SGITU Payment -> G8 integration test" -ForegroundColor Yellow
Write-Host "This script assumes G8 and payment-service were started manually."

Write-Step "Service readiness"
$checks = [ordered]@{
    "sgitu-kafka" = "Kafka"
    "g8-mongo" = "G8 Mongo"
    "g8-analytics-service" = "G8 Analytics"
    "g6-payment-db" = "Payment MySQL"
    "g6-payment-service" = "Payment service"
}
foreach ($container in $checks.Keys) {
    Add-Result "$($checks[$container]) container is ready" (Wait-ContainerHealthy -ContainerName $container -Timeout $TimeoutSeconds) $container
}
Add-Result "G8 health endpoint is reachable" (Wait-HttpOk -Url "http://localhost:8088/actuator/health" -Timeout $TimeoutSeconds)

Write-Step "Trigger a real payment action"
$paymentId = $null
$transactionToken = $null
try {
    $paymentBody = @{
        userId = $userId
        sourceType = "TICKET"
        sourceId = [int](700000 + ($runId % 100000))
        amount = 25.0
        paymentMethod = "CARD"
        savedPaymentToken = "G8-STAGE4-NONEXISTENT-TOKEN-$runId"
        email = "g8-payment-$runId@example.com"
        description = "G8 Stage 4 payment analytics probe"
    } | ConvertTo-Json -Depth 10 -Compress

    $payment = Invoke-RestMethod -Uri "$baseUrl/payments" -Method Post -ContentType "application/json" -Body $paymentBody -TimeoutSec 45
    $paymentId = "$($payment.paymentId)"
    $transactionToken = "$($payment.transactionToken)"
    Add-Result "Payment endpoint was called" (-not [string]::IsNullOrWhiteSpace($paymentId)) ($payment | ConvertTo-Json -Compress -Depth 8)
} catch {
    Add-Result "Payment endpoint was called" $false $_.Exception.Message
}

Write-Step "Verify Payment -> Kafka -> G8"
$analyticsKafka = Read-KafkaTopicQuiet -Topic $analyticsTopic -MaxMessages 80 -TimeoutMs 8000
$analyticsPublished = ($paymentId -and $analyticsKafka.Text -match [regex]::Escape($paymentId)) -or ($transactionToken -and $analyticsKafka.Text -match [regex]::Escape($transactionToken))
Add-Result "Payment service published to $analyticsTopic" $analyticsPublished "paymentId=$paymentId transactionToken=$transactionToken"

$notificationKafka = Read-KafkaTopicQuiet -Topic $notificationTopic -MaxMessages 80 -TimeoutMs 6000
$notificationPublished = ($paymentId -and $notificationKafka.Text -match [regex]::Escape($paymentId)) -or ($notificationKafka.Text -match "PAYMENT")
Add-Result "Payment service published notification event to $notificationTopic" $notificationPublished "This is useful sender-side evidence, but G8 does not consume this topic."

$g8Stored = Wait-MongoCondition "db.incoming_events.countDocuments({sourceType:'PAYMENT', `$or:[{'payload.paymentId':'$paymentId'},{'payload.transactionId':'$transactionToken'},{'payload.transactionToken':'$transactionToken'}]})" { param($value) [int]$value -gt 0 } 75
Add-Result "G8 stored the payment event" $g8Stored "paymentId=$paymentId transactionToken=$transactionToken"

Write-Step "Run G8 analytics"
try {
    Invoke-G8Run -Headers $g8Headers
    Add-Result "Manual analytics job completed" $true
} catch {
    Add-Result "Manual analytics job completed" $false $_.Exception.Message
}
$revExists = Wait-MongoCondition "db.stat_snapshots.countDocuments({statId:'REV_TOTAL'})" { param($value) [int]$value -gt 0 } 45
Add-Result "Payment impacted REV_TOTAL snapshot" $revExists

Write-Step "Diagnosis"
if (-not $analyticsPublished -and $notificationPublished) {
    Write-Host "[DIAGNOSIS] G6 produced a notification event, but not the G8 analytics topic payment.transaction.completed. This is sender-side contract drift/topic drift." -ForegroundColor Yellow
} elseif ($analyticsPublished -and -not $g8Stored) {
    Write-Host "[DIAGNOSIS] G6 published the analytics topic, but G8 did not persist. Check payload fields and G8 consumer logs." -ForegroundColor Yellow
} elseif ($g8Stored) {
    Write-Host "[OK] Payment published and G8 persisted the event." -ForegroundColor Green
}

Complete-Script -UsefulLogs @(
    "docker logs g6-payment-service --tail=150",
    "docker logs g8-analytics-service --tail=150",
    "docker exec sgitu-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group g8-analytics-group"
)
