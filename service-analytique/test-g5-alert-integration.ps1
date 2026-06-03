# SGITU G8 -> G5 alert integration test.
# Run from either the repository root or service-analytique:
#   powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g5-alert-integration.ps1
#   powershell -ExecutionPolicy Bypass -File .\test-g5-alert-integration.ps1
#
# By default this script does not build or start containers. Start G8 and G5
# manually first, then run this script to verify alert delivery.

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
        [string]$Subject = "g8-g5-alert-test",
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

function Wait-ContainerHealthy {
    param(
        [string]$ContainerName,
        [int]$Timeout = 120
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
        [hashtable]$Headers = @{},
        [int]$Timeout = 120
    )

    $deadline = (Get-Date).AddSeconds($Timeout)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Url -Headers $Headers -UseBasicParsing -TimeoutSec 5
            if ([int]$response.StatusCode -ge 200 -and [int]$response.StatusCode -lt 300) {
                return $true
            }
        } catch {
            Start-Sleep -Seconds 3
        }
    }
    return $false
}

function Invoke-G8JsonPost {
    param(
        [string]$Path,
        [object]$Body,
        [hashtable]$Headers
    )

    $json = ConvertTo-Json -InputObject $Body -Depth 12 -Compress
    return Invoke-RestMethod -Uri "http://localhost:8088$Path" -Method Post -Headers $Headers -ContentType "application/json" -Body $json -TimeoutSec 30
}

function Invoke-G5Sql {
    param([string]$Sql)

    $rootPassword = $Script:EnvValues["MYSQL_ROOT_PASSWORD"]
    if (-not $rootPassword) {
        throw "MYSQL_ROOT_PASSWORD is missing from .env"
    }

    $output = & docker exec -e "MYSQL_PWD=$rootPassword" sgitu-mysql-notification mysql -uroot notifications_db -N -B -e $Sql 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL query failed: $Sql"
    }
    return $output
}

function Get-G5NotificationCount {
    $raw = Invoke-G5Sql "SELECT COUNT(*) FROM notifications WHERE source_service='G8_ANALYTICS';"
    if (-not $raw) {
        return 0
    }
    return [int]($raw | Select-Object -First 1)
}

function Get-G5EventTypeCounts {
    $rows = Invoke-G5Sql "SELECT event_type, COUNT(*) FROM notifications WHERE source_service='G8_ANALYTICS' GROUP BY event_type ORDER BY event_type;"
    $counts = [ordered]@{}
    foreach ($row in $rows) {
        $parts = "$row".Split("`t")
        if ($parts.Count -ge 2) {
            $counts[$parts[0]] = [int]$parts[1]
        }
    }
    return $counts
}

function Get-G8AlertMetrics {
    $metrics = Invoke-WebRequest -Uri "http://localhost:8088/actuator/prometheus" -UseBasicParsing -TimeoutSec 10
    $counts = [ordered]@{}
    foreach ($line in ($metrics.Content -split "`n")) {
        if ($line -match '^sgitu_alerts_triggered_total\{.*alert_type="([^"]+)".*\}\s+([0-9.Ee+-]+)') {
            $counts[$matches[1]] = [double]$matches[2]
        }
    }
    return $counts
}

$serviceDir = $PSScriptRoot
$repoRoot = Resolve-Path (Join-Path $serviceDir "..")
if (-not (Test-Path (Join-Path $repoRoot "docker-compose.yml"))) {
    $repoRoot = Resolve-Path (Join-Path $serviceDir "..\..")
}
Set-Location $repoRoot

Write-Step "Preflight"
$dotenv = Read-DotEnv (Join-Path $repoRoot ".env")
$Script:EnvValues = $dotenv.Values

$jwtSecret = $Script:EnvValues["JWT_SECRET"]
if (-not $jwtSecret) {
    throw "JWT_SECRET is missing from .env"
}
if ($dotenv.Counts.ContainsKey("JWT_SECRET") -and $dotenv.Counts["JWT_SECRET"] -gt 1) {
    Write-Host "[INFO] JWT_SECRET appears $($dotenv.Counts["JWT_SECRET"]) times; using the last value, same as Docker Compose." -ForegroundColor Yellow
}
if ([Text.Encoding]::UTF8.GetByteCount($jwtSecret) -lt 32) {
    throw "JWT_SECRET must be at least 32 bytes for G8/G5 HS256 validation."
}
if (-not $Script:EnvValues["MYSQL_ROOT_PASSWORD"]) {
    throw "MYSQL_ROOT_PASSWORD is missing from .env; the script needs it to verify G5 persistence."
}

$composePlugin = $false
& docker compose version *> $null
if ($LASTEXITCODE -eq 0) {
    $composePlugin = $true
} else {
    & docker-compose version *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Neither 'docker compose' nor 'docker-compose' is available."
    }
}
$Script:UseComposePlugin = $composePlugin
Add-Result "Docker Compose is available" $true ($(if ($composePlugin) { "using docker compose" } else { "using docker-compose" }))

$token = New-Hs256Jwt -Secret $jwtSecret
$headers = @{ Authorization = "Bearer $token" }

Write-Step "Verify manually started G8 and G5 dependencies"
$services = @("kafka", "g8-mongo", "g8-ml-service", "g8-analytics-service", "mysql-notification", "notification-service")
if ($StartServices) {
    if ($SkipBuild) {
        Invoke-Compose -ComposeArgs (@("up", "-d") + $services)
    } else {
        Invoke-Compose -ComposeArgs (@("up", "-d", "--build") + $services)
    }
    Add-Result "Required containers requested" $true ($services -join ", ")
} else {
    Write-Host "Startup is manual for this stage. Expected services: $($services -join ', ')."
    Add-Result "Service startup left manual" $true
}

Write-Step "Wait for services"
$containerChecks = [ordered]@{
    "sgitu-kafka" = "Kafka"
    "g8-mongo" = "G8 Mongo"
    "g8-ml-service" = "G8 ML service"
    "g8-analytics-service" = "G8 Analytics"
    "sgitu-mysql-notification" = "G5 MySQL"
    "notification-service" = "G5 Notification service"
}
foreach ($container in $containerChecks.Keys) {
    $ok = Wait-ContainerHealthy -ContainerName $container -Timeout $TimeoutSeconds
    Add-Result "$($containerChecks[$container]) container is ready" $ok $container
}

$g8Ok = Wait-HttpOk -Url "http://localhost:8088/actuator/health" -Timeout $TimeoutSeconds
Add-Result "G8 health endpoint is reachable" $g8Ok "http://localhost:8088/actuator/health"

$g5Ok = Wait-HttpOk -Url "http://localhost:8085/api/notifications/health" -Timeout $TimeoutSeconds
Add-Result "G5 health endpoint is reachable" $g5Ok "http://localhost:8085/api/notifications/health"

Write-Step "Baseline G5 state"
$beforeCount = Get-G5NotificationCount
$beforeTypes = Get-G5EventTypeCounts
Write-Host "[INFO] G5 currently has $beforeCount notification(s) from G8_ANALYTICS."
if ($beforeTypes.Count -gt 0) {
    foreach ($key in $beforeTypes.Keys) {
        Write-Host "[INFO]   $key = $($beforeTypes[$key])"
    }
}

Write-Step "Ingest threshold-violating G8 data"
$runId = [Guid]::NewGuid().ToString("N").Substring(0, 10)
$now = [DateTimeOffset]::UtcNow
$vehicles = @()
for ($i = 1; $i -le 60; $i++) {
    $vehicles += @{
        schemaVersion = 1
        timestamp = $now.AddSeconds($i).ToString("yyyy-MM-ddTHH:mm:ssZ")
        vehicleId = "g8-alert-$runId-veh-$i"
        status = "in_service"
        line = "ALERT-LINE"
        delayMinutes = 25
        speed = 16
    }
}
$vehicleResponse = Invoke-G8JsonPost -Path "/api/v1/ingestion/vehicles" -Body $vehicles -Headers $headers
Add-Result "Delayed vehicle events ingested" $true ($vehicleResponse | ConvertTo-Json -Compress)

$incidents = @()
for ($i = 1; $i -le 12; $i++) {
    $incidents += @{
        schemaVersion = 1
        timestamp = $now.AddMinutes($i).ToString("yyyy-MM-ddTHH:mm:ssZ")
        incidentId = "g8-alert-$runId-inc-$i"
        type = "breakdown"
        severity = "CRITICAL"
        latitude = 33.5731
        longitude = -7.5898
        resolutionMinutes = 90
    }
}
$incidentResponse = Invoke-G8JsonPost -Path "/api/v1/ingestion/incidents" -Body $incidents -Headers $headers
Add-Result "Incident threshold events ingested" $true ($incidentResponse | ConvertTo-Json -Compress)

Write-Step "Run analytics and alert detection"
try {
    $runResponse = Invoke-WebRequest -Uri "http://localhost:8088/test/run" -Method Get -Headers $headers -UseBasicParsing -TimeoutSec 90
    Add-Result "Manual analytics job completed" ([int]$runResponse.StatusCode -ge 200 -and [int]$runResponse.StatusCode -lt 300) $runResponse.Content
} catch {
    Add-Result "Manual analytics job completed" $false $_.Exception.Message
}

Start-Sleep -Seconds 8

Write-Step "Verify G8 alert metrics"
$metrics = Get-G8AlertMetrics
$expectedAlerts = @("PUNCTUALITY_ALERT", "HIGH_INCIDENT_VOLUME", "INCIDENT_ZONE_RISK")
foreach ($alert in $expectedAlerts) {
    $value = if ($metrics.Contains($alert)) { $metrics[$alert] } else { 0 }
    Add-Result "G8 metric incremented for $alert" ($value -gt 0) "sgitu_alerts_triggered_total{alert_type=`"$alert`"} = $value"
}

Write-Step "Verify G5 persistence"
$afterCount = Get-G5NotificationCount
$afterTypes = Get-G5EventTypeCounts
$delta = $afterCount - $beforeCount
Add-Result "G5 stored new G8 notifications" ($delta -ge 2) "before=$beforeCount after=$afterCount delta=$delta"

foreach ($alert in $expectedAlerts) {
    $before = if ($beforeTypes.Contains($alert)) { $beforeTypes[$alert] } else { 0 }
    $after = if ($afterTypes.Contains($alert)) { $afterTypes[$alert] } else { 0 }
    Add-Result "G5 stored $alert" ($after -gt $before) "before=$before after=$after"
}

Write-Step "Actionable diagnosis"
if ($Script:Results.Fail -eq 0) {
    Write-Host "[OK] G8 generated threshold alerts and G5 persisted them." -ForegroundColor Green
} else {
    $triggered = $false
    foreach ($alert in $expectedAlerts) {
        if ($metrics.Contains($alert) -and $metrics[$alert] -gt 0) {
            $triggered = $true
        }
    }

    if (-not $triggered) {
        Write-Host "[DIAGNOSIS] G8 did not expose alert metric increments. Check ingestion responses, /test/run, and stat_snapshots for VEH_PUNCTUALITY, INC_TOTAL, INC_REPEAT_ZONES." -ForegroundColor Yellow
    } elseif ($delta -lt 1) {
        Write-Host "[DIAGNOSIS] G8 triggered alerts, but G5 did not persist them. Check G5_NOTIFICATION_URL, G5 JWT/security behavior, notification-service logs, and the MySQL notifications table." -ForegroundColor Yellow
    } else {
        Write-Host "[DIAGNOSIS] Some alert types passed and others did not. Compare the G8 metric output above with the G5 event_type counts to identify the missing threshold." -ForegroundColor Yellow
    }
}

Write-Step "Summary"
Write-Host "Total: $($Script:Results.Total), Passed: $($Script:Results.Pass), Failed: $($Script:Results.Fail)"
if ($Script:Results.Fail -gt 0) {
    exit 1
}
exit 0
