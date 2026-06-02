# SGITU G3 -> G8 user-event integration test.
# Run after starting G8 and G3 manually:
#   powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g3-user-events.ps1
#   powershell -ExecutionPolicy Bypass -File .\test-g3-user-events.ps1

param(
    [int]$TimeoutSeconds = 120
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$Script:Results = [ordered]@{
    Total = 0
    Pass = 0
    Fail = 0
}

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "== $Message ==" -ForegroundColor Cyan
}

function Add-Result {
    param(
        [string]$Name,
        [bool]$Success,
        [string]$Details = ""
    )

    $Script:Results.Total++
    if ($Success) {
        $Script:Results.Pass++
        Write-Host "[PASS] $Name" -ForegroundColor Green
    } else {
        $Script:Results.Fail++
        if ($Details) {
            Write-Host "[FAIL] $Name - $Details" -ForegroundColor Red
        } else {
            Write-Host "[FAIL] $Name" -ForegroundColor Red
        }
    }
}

function Wait-ContainerHealthy {
    param(
        [string]$ContainerName,
        [int]$Timeout = $TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($Timeout)
    while ((Get-Date) -lt $deadline) {
        $status = (& docker inspect -f "{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}" $ContainerName 2>$null)
        if ($LASTEXITCODE -eq 0 -and ($status -eq "healthy" -or $status -eq "running")) {
            return $true
        }
        Start-Sleep -Seconds 3
    }
    return $false
}

function Wait-HttpOk {
    param(
        [string]$Url,
        [int]$Timeout = $TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($Timeout)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ([int]$response.StatusCode -ge 200 -and [int]$response.StatusCode -lt 300) {
                return $true
            }
        } catch {
            Start-Sleep -Seconds 3
        }
    }
    return $false
}

function Invoke-MongoEval {
    param([string]$Eval)

    $output = & docker exec g8-mongo mongosh g8_analytics --quiet --eval $Eval
    if ($LASTEXITCODE -ne 0) {
        throw "Mongo eval failed: $Eval"
    }
    return (($output | Out-String).Trim())
}

function Wait-MongoCondition {
    param(
        [string]$Eval,
        [scriptblock]$Predicate,
        [int]$Timeout = 60
    )

    $deadline = (Get-Date).AddSeconds($Timeout)
    while ((Get-Date) -lt $deadline) {
        try {
            $value = Invoke-MongoEval $Eval
            if (& $Predicate $value) {
                return $true
            }
        } catch {
            Start-Sleep -Seconds 2
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Read-KafkaTopicQuiet {
    param(
        [string]$Topic,
        [int]$MaxMessages = 30,
        [int]$TimeoutMs = 5000
    )

    $oldPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & docker exec sgitu-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic $Topic --from-beginning --timeout-ms $TimeoutMs --max-messages $MaxMessages 2>&1
        return @{
            ExitCode = $LASTEXITCODE
            Text = ($output | Out-String)
        }
    } finally {
        $ErrorActionPreference = $oldPreference
    }
}

$serviceDir = $PSScriptRoot
$repoRoot = Resolve-Path (Join-Path $serviceDir "..")
if (-not (Test-Path (Join-Path $repoRoot "docker-compose.yml"))) {
    $repoRoot = Resolve-Path (Join-Path $serviceDir "..\..")
}
Set-Location $repoRoot

$g3BaseUrl = "http://localhost:8083/api"
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

Write-Host "SGITU G3 -> G8 user-event integration test" -ForegroundColor Yellow
Write-Host "This script assumes G8 and G3 services were started manually."

Write-Step "Service readiness"
$checks = [ordered]@{
    "sgitu-kafka" = "Kafka"
    "g8-mongo" = "G8 Mongo"
    "g8-analytics-service" = "G8 Analytics"
    "g3-users-db" = "G3 database"
    "g3-user-service" = "G3 user-service"
}
foreach ($container in $checks.Keys) {
    $ok = Wait-ContainerHealthy -ContainerName $container
    Add-Result "$($checks[$container]) container is ready" $ok $container
}

$g8Ready = Wait-HttpOk "http://localhost:8088/actuator/health"
Add-Result "G8 health endpoint is reachable" $g8Ready "http://localhost:8088/actuator/health"

$g3Ready = Wait-HttpOk "$g3BaseUrl/actuator/health"
Add-Result "G3 health endpoint is reachable" $g3Ready "$g3BaseUrl/actuator/health"

Write-Step "Create a real G3 user"
$email = "g8-g3-$runId@example.com"
$createdUserId = $null
try {
    $body = @{
        email = $email
        password = "Secret123!"
        role = "ROLE_PASSENGER"
        profile = @{
            firstName = "G8"
            lastName = "G3Kafka"
            phone = "0600000000"
            address = "SGITU integration test"
            birthDate = "1998-01-01"
        }
    } | ConvertTo-Json -Compress -Depth 10

    $created = Invoke-RestMethod -Uri "$g3BaseUrl/users" -Method Post -Body $body -ContentType "application/json" -TimeoutSec 30
    $createdUserId = "$($created.id)"
    Add-Result "G3 public user creation succeeds" (-not [string]::IsNullOrWhiteSpace($createdUserId)) ($created | ConvertTo-Json -Compress)
} catch {
    Add-Result "G3 public user creation succeeds" $false $_.Exception.Message
}

if ($createdUserId) {
    Write-Step "Verify G3 -> Kafka -> G8"
    $g8Ingested = Wait-MongoCondition "db.incoming_events.countDocuments({'payload.userId':'$createdUserId'})" { param($value) [int]$value -gt 0 } 75
    Add-Result "G8 stored the G3 user event" $g8Ingested "createdUserId=$createdUserId"

    if ($g8Ingested) {
        Add-Result "G3 user event path is end-to-end" $true "G3 create -> g8-user-events -> G8 Mongo"
        Write-Host "[OK] G3 produced the user event and G8 consumed it." -ForegroundColor Green
    } else {
        Write-Host "[INFO] G8 did not store the created user event yet; checking Kafka topic quietly for diagnosis." -ForegroundColor Yellow
        $g3Published = $false
        $matchedPayload = ""
        $deadline = (Get-Date).AddSeconds(45)
        while ((Get-Date) -lt $deadline -and -not $g3Published) {
            $topicResult = Read-KafkaTopicQuiet -Topic "g8-user-events" -MaxMessages 40 -TimeoutMs 5000
            $text = $topicResult.Text
            if ($text -match "`"userId`"\s*:\s*`"$createdUserId`"") {
                $g3Published = $true
                $matchedPayload = ($text -split "`r?`n" | Where-Object { $_ -match "`"userId`"\s*:\s*`"$createdUserId`"" } | Select-Object -First 1)
                break
            }
            Start-Sleep -Seconds 3
        }

        Add-Result "G3 published user event to g8-user-events" $g3Published $matchedPayload
        if ($g3Published) {
            Write-Host "[DIAGNOSIS] G3 is publishing, but G8 did not persist the event. Current G3 sends one JSON object; G8's g8-user-events listener expects a JSON array/batch. This is a producer/consumer contract mismatch, not a Docker network issue." -ForegroundColor Yellow
        } else {
            Write-Host "[DIAGNOSIS] G3 user creation worked, but no matching Kafka event was found. Check G3 Kafka bootstrap config, events.user-status.topic, and g3-user-service logs." -ForegroundColor Yellow
        }
    }
}

Write-Step "Summary"
Write-Host "Total: $($Script:Results.Total)"
Write-Host "Pass:  $($Script:Results.Pass)" -ForegroundColor Green
if ($Script:Results.Fail -gt 0) {
    Write-Host "Fail:  $($Script:Results.Fail)" -ForegroundColor Red
    Write-Host "Useful logs:" -ForegroundColor Yellow
    Write-Host "  docker logs g3-user-service --tail=150"
    Write-Host "  docker logs g8-analytics-service --tail=150"
    Write-Host "  docker exec sgitu-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group g8-analytics-group"
    exit 1
}

Write-Host "Fail:  0" -ForegroundColor Green
exit 0
