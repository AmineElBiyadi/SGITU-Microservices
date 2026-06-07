# Shared helpers for G8 sender integration scripts.

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

if (-not $Script:Results) {
    $Script:Results = [ordered]@{ Total = 0; Pass = 0; Fail = 0 }
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

function Resolve-RepoRoot {
    $serviceDir = $PSScriptRoot
    $repoRoot = Resolve-Path (Join-Path $serviceDir "..")
    if (-not (Test-Path (Join-Path $repoRoot "docker-compose.yml"))) {
        $repoRoot = Resolve-Path (Join-Path $serviceDir "..\..")
    }
    return $repoRoot
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
        [int]$Timeout = 75
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
        [int]$MaxMessages = 50,
        [int]$TimeoutMs = 7000
    )

    $oldPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & docker exec sgitu-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic $Topic --from-beginning --timeout-ms $TimeoutMs --max-messages $MaxMessages 2>&1
        return @{ ExitCode = $LASTEXITCODE; Text = ($output | Out-String) }
    } finally {
        $ErrorActionPreference = $oldPreference
    }
}

function ConvertTo-Base64Url {
    param([byte[]]$Bytes)
    return [Convert]::ToBase64String($Bytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
}

function New-Hs256Jwt {
    param(
        [string]$Secret,
        [string]$Subject = "g8-stage4-test",
        [string[]]$Roles = @("ROLE_ADMIN", "ROLE_SUPERVISOR", "ROLE_DISPATCHER", "ADMIN")
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
    $signature64 = ConvertTo-Base64Url ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($input)))
    return "$input.$signature64"
}

function Read-DotEnv {
    param([string]$Path)

    $values = @{}
    if (-not (Test-Path $Path)) {
        return $values
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
    }
    return $values
}

function Get-G8AuthHeaders {
    param([string]$RepoRoot)

    $envValues = Read-DotEnv (Join-Path $RepoRoot ".env")
    $secret = $envValues["JWT_SECRET"]
    if (-not $secret) {
        $secret = "sgitu_g8_secret_key_2025_very_long_secret_for_analytics"
    }
    return @{ Authorization = "Bearer $(New-Hs256Jwt -Secret $secret)" }
}

function Invoke-G8Run {
    param([hashtable]$Headers)
    Invoke-WebRequest -Uri "http://localhost:8088/test/run" -Method Get -Headers $Headers -UseBasicParsing -TimeoutSec 90 | Out-Null
}

function Complete-Script {
    param(
        [string[]]$UsefulLogs = @("docker logs g8-analytics-service --tail=150")
    )

    Write-Step "Summary"
    Write-Host "Total: $($Script:Results.Total)"
    Write-Host "Pass:  $($Script:Results.Pass)" -ForegroundColor Green
    if ($Script:Results.Fail -gt 0) {
        Write-Host "Fail:  $($Script:Results.Fail)" -ForegroundColor Red
        Write-Host "Useful logs:" -ForegroundColor Yellow
        foreach ($logCommand in $UsefulLogs) {
            Write-Host "  $logCommand"
        }
        exit 1
    }
    Write-Host "Fail:  0" -ForegroundColor Green
    exit 0
}
