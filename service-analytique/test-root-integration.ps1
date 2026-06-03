# SGITU G8 root-compose integration smoke test.
# Run from either the repository root or service-analytique:
#   powershell -ExecutionPolicy Bypass -File .\service-analytique\test-root-integration.ps1
#   powershell -ExecutionPolicy Bypass -File .\test-root-integration.ps1
#
# By default this script does not build or start containers. Start the G8
# runtime manually first, then run this script to verify it.

param(
    [int]$TimeoutSeconds = 180,
    [switch]$StartServices,
    [switch]$SkipBuild
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

function ConvertTo-Base64Url {
    param([byte[]]$Bytes)
    return [Convert]::ToBase64String($Bytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
}

function New-Hs256Jwt {
    param(
        [string]$Secret,
        [string]$Subject = "g8-root-integration",
        [string[]]$Roles = @("ROLE_ADMIN", "ADMIN")
    )

    $header = @{ alg = "HS256"; typ = "JWT" } | ConvertTo-Json -Compress
    $now = [DateTimeOffset]::UtcNow
    $payload = @{
        sub = $Subject
        roles = $Roles
        iat = $now.ToUnixTimeSeconds()
        exp = $now.AddHours(2).ToUnixTimeSeconds()
    } | ConvertTo-Json -Compress

    $header64 = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($header))
    $payload64 = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($payload))
    $input = "$header64.$payload64"

    $hmac = [System.Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($Secret))
    $signature = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($input))
    $signature64 = ConvertTo-Base64Url $signature
    return "$input.$signature64"
}

function Read-DotEnv {
    param([string]$Path)

    $values = @{}
    $counts = @{}
    if (-not (Test-Path $Path)) {
        throw ".env not found at $Path"
    }

    foreach ($line in Get-Content $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#")) {
            continue
        }
        $idx = $trimmed.IndexOf("=")
        if ($idx -le 0) {
            continue
        }
        $key = $trimmed.Substring(0, $idx).Trim()
        $value = $trimmed.Substring($idx + 1).Trim()
        $values[$key] = $value
        if (-not $counts.ContainsKey($key)) {
            $counts[$key] = 0
        }
        $counts[$key]++
    }

    return @{
        Values = $values
        Counts = $counts
    }
}

function Invoke-Compose {
    param([string[]]$ComposeArgs)

    if ($Script:UseComposePlugin) {
        $output = & docker compose @ComposeArgs 2>&1
    } else {
        $output = & docker-compose @ComposeArgs 2>&1
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Compose command failed: $($ComposeArgs -join ' '). $($output | Out-String)"
    }
}

function Invoke-Docker {
    param([string[]]$DockerArgs)

    $output = & docker @DockerArgs 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed: docker $($DockerArgs -join ' '). $($output | Out-String)"
    }
}

function Wait-ContainerHealthy {
    param(
        [string]$ContainerName,
        [int]$Timeout = $TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($Timeout)
    while ((Get-Date) -lt $deadline) {
        $state = (& docker inspect --format "{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}" $ContainerName 2>$null)
        if ($LASTEXITCODE -eq 0 -and $state) {
            $parts = "$state".Split("|")
            if ($parts[0] -eq "running" -and ($parts[1] -eq "healthy" -or $parts[1] -eq "none")) {
                return $true
            }
        }
        Start-Sleep -Seconds 3
    }
    return $false
}

function Wait-HttpOk {
    param(
        [string]$Url,
        [hashtable]$Headers = @{},
        [int]$Timeout = $TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($Timeout)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Url -Headers $Headers -TimeoutSec 5 -ErrorAction Stop
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
                return $true
            }
        } catch {
            Start-Sleep -Seconds 3
        }
    }
    return $false
}

function New-UtcTimestamp {
    return [DateTimeOffset]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ssZ")
}

function Get-WebErrorDetails {
    param([System.Management.Automation.ErrorRecord]$ErrorRecord)

    $message = $ErrorRecord.Exception.Message
    $response = $ErrorRecord.Exception.Response
    if ($response -and $response.GetResponseStream()) {
        try {
            $reader = [System.IO.StreamReader]::new($response.GetResponseStream())
            $body = $reader.ReadToEnd()
            if (-not [string]::IsNullOrWhiteSpace($body)) {
                return "$message Body: $body"
            }
        } catch {
            return $message
        }
    }
    return $message
}

function Invoke-AuthenticatedJson {
    param(
        [string]$Method,
        [string]$Url,
        [string]$Token,
        [object]$Body = $null
    )

    $headers = @{
        Authorization = "Bearer $Token"
        "Content-Type" = "application/json"
    }

    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers -TimeoutSec 20
    }

    $json = ConvertTo-Json -InputObject $Body -Compress -Depth 10
    return Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers -Body $json -TimeoutSec 20
}

function New-KafkaTopic {
    param([string]$Topic)

    & docker exec sgitu-kafka kafka-topics --bootstrap-server localhost:9092 --create --if-not-exists --topic $Topic --partitions 3 --replication-factor 1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create Kafka topic $Topic"
    }
}

function Send-KafkaMessage {
    param(
        [string]$Topic,
        [string]$Json
    )

    $Json | & docker exec -i sgitu-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic $Topic | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to publish message to $Topic"
    }
}

function Invoke-MongoEval {
    param([string]$Eval)

    $output = & docker exec g8-mongo mongosh g8_analytics --quiet --eval $Eval
    if ($LASTEXITCODE -ne 0) {
        throw "Mongo eval failed: $Eval"
    }
    return ($output | Out-String).Trim()
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

$serviceDir = $PSScriptRoot
$repoRoot = Split-Path -Parent $serviceDir
if (-not (Test-Path (Join-Path $repoRoot "docker-compose.yml"))) {
    throw "Could not locate repository root from $serviceDir"
}

Set-Location $repoRoot

Write-Host "SGITU G8 root-compose integration smoke test" -ForegroundColor Yellow
Write-Host "Repository root: $repoRoot"

$envData = Read-DotEnv (Join-Path $repoRoot ".env")
$envValues = $envData.Values
$envCounts = $envData.Counts

if (-not $envValues.ContainsKey("JWT_SECRET") -or [string]::IsNullOrWhiteSpace($envValues["JWT_SECRET"])) {
    throw "JWT_SECRET is missing from .env"
}

$jwtSecret = $envValues["JWT_SECRET"]
if ($envCounts.ContainsKey("JWT_SECRET") -and $envCounts["JWT_SECRET"] -gt 1) {
    Write-Host "JWT_SECRET appears $($envCounts["JWT_SECRET"]) times in .env; using the last value, matching Docker Compose behavior." -ForegroundColor Yellow
}

if ([Text.Encoding]::UTF8.GetByteCount($jwtSecret) -lt 32) {
    throw "JWT_SECRET must be at least 32 UTF-8 bytes for HS256/JJWT. Current value is too short."
}

$token = New-Hs256Jwt -Secret $jwtSecret
$authHeaders = @{ Authorization = "Bearer $token" }
$baseUrl = "http://localhost:8088"
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

$Script:UseComposePlugin = $false
& docker compose version *> $null
if ($LASTEXITCODE -eq 0) {
    $Script:UseComposePlugin = $true
}

Write-Host "Compose command: $(if ($Script:UseComposePlugin) { 'docker compose' } else { 'docker-compose' })"

Write-Step "A. Compose validation"
try {
    Invoke-Compose -ComposeArgs @("config")
    Add-Result "Compose model renders" $true
} catch {
    Add-Result "Compose model renders" $false $_.Exception.Message
    throw
}

Write-Step "B. Verify manually started G8 runtime"
if ($StartServices) {
    $upArgs = @("up", "-d")
    if (-not $SkipBuild) {
        $upArgs += "--build"
    }
    $upArgs += @("kafka", "g8-mongo", "g8-ml-service", "g8-analytics-service", "prometheus")
    try {
        Invoke-Compose -ComposeArgs $upArgs
        Add-Result "Started Kafka, G8 Mongo, G8 ML, G8 analytics, Prometheus" $true
    } catch {
        Add-Result "Started minimal G8 runtime" $false $_.Exception.Message
        throw
    }
} else {
    Write-Host "Startup is manual for this stage. Expected services: kafka, g8-mongo, g8-ml-service, g8-analytics-service, prometheus."
    Add-Result "Service startup left manual" $true
}

Write-Step "C. Container health"
foreach ($container in @("sgitu-kafka", "g8-mongo", "g8-ml-service", "g8-analytics-service")) {
    $healthy = Wait-ContainerHealthy $container
    Add-Result "$container is running/healthy" $healthy
}

Write-Step "D. Direct infrastructure checks"
try {
    Invoke-Docker -DockerArgs @("exec", "sgitu-kafka", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092")
    Add-Result "Kafka broker responds" $true
} catch {
    Add-Result "Kafka broker responds" $false $_.Exception.Message
}

try {
    $mongoPing = Invoke-MongoEval "JSON.stringify(db.adminCommand('ping'))"
    Add-Result "Mongo ping succeeds" ($mongoPing -match '"ok"\s*:\s*1') $mongoPing
} catch {
    Add-Result "Mongo ping succeeds" $false $_.Exception.Message
}

try {
    $mlHealth = & docker exec g8-analytics-service curl -s http://g8-ml-service:5000/health
    Add-Result "ML service reachable from G8 container" ("$mlHealth" -match "ok") "$mlHealth"
} catch {
    Add-Result "ML service reachable from G8 container" $false $_.Exception.Message
}

Write-Step "E. G8 HTTP and JWT checks"
$g8Ready = Wait-HttpOk "$baseUrl/actuator/health"
Add-Result "G8 actuator health is reachable" $g8Ready

try {
    Invoke-WebRequest -Uri "$baseUrl/api/v1/analytics/dashboard" -TimeoutSec 10 -ErrorAction Stop | Out-Null
    Add-Result "G8 blocks unauthenticated dashboard request" $false "Expected 401, got success"
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    Add-Result "G8 blocks unauthenticated dashboard request" ($status -eq 401) "Expected 401, got $status"
}

try {
    Invoke-WebRequest -Uri "$baseUrl/api/v1/analytics/dashboard" -Headers $authHeaders -TimeoutSec 10 -ErrorAction Stop | Out-Null
    Add-Result "G8 accepts generated JWT from .env secret" $true
} catch {
    Add-Result "G8 accepts generated JWT from .env secret" $false $_.Exception.Message
}

Write-Step "F. Kafka topic preparation"
$topics = @(
    "ticket.validated",
    "abonnement.souscription",
    "payment.transaction.completed",
    "g8.vehicule.status",
    "incident.analytique.topic",
    "g8-user-events",
    "g8-analytics-dlt",
    "g8-analytics-results",
    "g8-ml-predictions"
)

$topicFailures = @()
foreach ($topic in $topics) {
    try {
        New-KafkaTopic $topic
    } catch {
        $topicFailures += "$topic ($($_.Exception.Message))"
    }
}
Add-Result "Kafka topics are ready" ($topicFailures.Count -eq 0) ($topicFailures -join "; ")

Write-Step "G. REST ingestion with current timestamps"
$ts = New-UtcTimestamp
try {
    $ticketEvents = @(@{
        schemaVersion = 1
        timestamp = $ts
        userId = "rest-user-$runId"
        status = "validated"
        line = "L1"
        stationId = "ST-$runId"
    })
    $ticketResponse = Invoke-AuthenticatedJson "POST" "$baseUrl/api/v1/ingestion/tickets" $token $ticketEvents
    Add-Result "REST ticket ingestion accepted" ($ticketResponse.status -eq "SUCCESS") ($ticketResponse | ConvertTo-Json -Compress)
} catch {
    Add-Result "REST ticket ingestion accepted" $false (Get-WebErrorDetails $_)
}

try {
    $vehicleEvents = @(@{
        schemaVersion = 1
        timestamp = (New-UtcTimestamp)
        vehicleId = "veh-rest-$runId"
        status = "in_service"
        line = "L1"
        delayMinutes = 0
        speed = 32
    })
    $vehicleResponse = Invoke-AuthenticatedJson "POST" "$baseUrl/api/v1/ingestion/vehicles" $token $vehicleEvents
    Add-Result "REST vehicle ingestion accepted" ($vehicleResponse.status -eq "SUCCESS") ($vehicleResponse | ConvertTo-Json -Compress)
} catch {
    Add-Result "REST vehicle ingestion accepted" $false (Get-WebErrorDetails $_)
}

try {
    $incidentEvents = @(@{
        schemaVersion = 1
        timestamp = (New-UtcTimestamp)
        incidentId = "inc-rest-$runId"
        type = "delay"
        severity = "HIGH"
        latitude = 33.57
        longitude = -7.59
    })
    $incidentResponse = Invoke-AuthenticatedJson "POST" "$baseUrl/api/v1/ingestion/incidents" $token $incidentEvents
    Add-Result "REST incident ingestion accepted" ($incidentResponse.status -eq "SUCCESS") ($incidentResponse | ConvertTo-Json -Compress)
} catch {
    Add-Result "REST incident ingestion accepted" $false (Get-WebErrorDetails $_)
}

try {
    $count = Invoke-MongoEval "db.incoming_events.countDocuments()"
    Add-Result "Mongo incoming_events has data" ([int]$count -gt 0) "count=$count"
} catch {
    Add-Result "Mongo incoming_events has data" $false $_.Exception.Message
}

Write-Step "H. Kafka ingestion with current timestamps"
$kafkaTicketUser = "kafka-user-$runId"
$kafkaVehicle = "veh-kafka-$runId"
$kafkaIncident = "inc-kafka-$runId"

try {
    Send-KafkaMessage "ticket.validated" (@{
        schemaVersion = 1
        timestamp = (New-UtcTimestamp)
        userId = $kafkaTicketUser
        status = "validated"
        line = "L2"
        stationId = "ST-K-$runId"
    } | ConvertTo-Json -Compress)

    Send-KafkaMessage "g8.vehicule.status" (@{
        schemaVersion = 1
        timestamp = (New-UtcTimestamp)
        vehicleId = $kafkaVehicle
        status = "in_service"
        line = "L2"
        delayMinutes = 2
        speed = 29
    } | ConvertTo-Json -Compress)

    Send-KafkaMessage "incident.analytique.topic" (@{
        schemaVersion = 1
        timestamp = (New-UtcTimestamp)
        incidentId = $kafkaIncident
        type = "breakdown"
        severity = "CRITICAL"
        latitude = 33.57
        longitude = -7.59
    } | ConvertTo-Json -Compress)

    Add-Result "Published Kafka messages to G8 topics" $true
} catch {
    Add-Result "Published Kafka messages to G8 topics" $false $_.Exception.Message
}

$kafkaPersisted = Wait-MongoCondition `
    "db.incoming_events.countDocuments({`$or:[{'payload.userId':'$kafkaTicketUser'},{'payload.vehicleId':'$kafkaVehicle'},{'payload.incidentId':'$kafkaIncident'}]})" `
    { param($value) [int]$value -ge 1 } `
    90
Add-Result "G8 consumed Kafka events into Mongo" $kafkaPersisted

try {
    $groupOutput = & docker exec sgitu-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group g8-analytics-group 2>&1
    $groupVisible = $LASTEXITCODE -eq 0 -and (($groupOutput | Out-String) -match "g8-analytics-group")
    Add-Result "G8 Kafka consumer group is visible" $groupVisible
} catch {
    Add-Result "G8 Kafka consumer group is visible" $false $_.Exception.Message
}

Write-Step "I. Scheduler, snapshots, ML, and Prometheus"
try {
    Invoke-WebRequest -Uri "$baseUrl/test/run" -Headers $authHeaders -TimeoutSec 60 -ErrorAction Stop | Out-Null
    Add-Result "Manual analytics job trigger succeeds" $true
} catch {
    Add-Result "Manual analytics job trigger succeeds" $false $_.Exception.Message
}

$snapshotsExist = Wait-MongoCondition "db.stat_snapshots.countDocuments()" { param($value) [int]$value -gt 0 } 90
Add-Result "Analytics snapshots exist in Mongo" $snapshotsExist

try {
    $metrics = Invoke-WebRequest -Uri "$baseUrl/actuator/prometheus" -TimeoutSec 15 -ErrorAction Stop
    Add-Result "G8 Prometheus endpoint responds" ($metrics.Content -match "jvm|process|http") "Unexpected metrics body"
} catch {
    Add-Result "G8 Prometheus endpoint responds" $false $_.Exception.Message
}

try {
    $prom = Invoke-RestMethod -Uri "http://localhost:9090/api/v1/targets" -TimeoutSec 15 -ErrorAction Stop
    $target = $prom.data.activeTargets | Where-Object { $_.labels.job -eq "g8-analytics" } | Select-Object -First 1
    Add-Result "Prometheus has g8-analytics target" ($null -ne $target) "Target not found"
} catch {
    Write-Host "[INFO] Prometheus is not reachable on localhost:9090; skipping global target check. Start it with 'docker compose up -d --no-deps prometheus' when you want this check." -ForegroundColor Yellow
    Add-Result "Prometheus target check skipped when Prometheus is not running" $true
}

Write-Step "Summary"
Write-Host "Total: $($Script:Results.Total)"
Write-Host "Pass:  $($Script:Results.Pass)" -ForegroundColor Green
if ($Script:Results.Fail -gt 0) {
    Write-Host "Fail:  $($Script:Results.Fail)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Next actions:" -ForegroundColor Yellow
    Write-Host "  1. Re-run this script once after the patch if REST ingestion failed with 'valid non-empty JSON array'."
    Write-Host "  2. If REST still fails, run: docker logs g8-analytics-service --tail=120"
    Write-Host "  3. If Kafka fails, run: docker logs sgitu-kafka --tail=80"
    exit 1
}

Write-Host "Fail:  0" -ForegroundColor Green
Write-Host "All G8 root integration checks passed." -ForegroundColor Green
