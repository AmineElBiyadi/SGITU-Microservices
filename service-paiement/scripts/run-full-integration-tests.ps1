$ErrorActionPreference = "Stop"

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$G1Root = Resolve-Path (Join-Path $Root "..\service-billetterie")
$TestDir = Join-Path $Root "target\test-run"
$G6Url = "http://localhost:18086"
$G1Url = "http://localhost:18081"

New-Item -ItemType Directory -Force -Path $TestDir | Out-Null

function Stop-PortProcess {
    param([int[]]$Ports)

    foreach ($port in $Ports) {
        Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
                ForEach-Object {
                    Write-Host "Stopping process $($_.OwningProcess) on port $port"
                    Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
                }
    }
}

function Wait-HttpOk {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3
            if ($response.StatusCode -eq 200) {
                Write-Host "Ready: $Url"
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }

    throw "Service not ready: $Url"
}

function Invoke-Checked {
    param(
        [string]$Title,
        [string]$Command,
        [string]$WorkDir
    )

    Write-Host ""
    Write-Host "== $Title =="
    Push-Location $WorkDir
    try {
        cmd.exe /c $Command
        if ($LASTEXITCODE -ne 0) {
            throw "Command failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

function Get-ErrorText {
    param($Exception)

    if ($null -eq $Exception.Response) {
        return $Exception.Message
    }

    try {
        $stream = $Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        return $reader.ReadToEnd()
    } catch {
        return $Exception.Message
    }
}

function Add-LinkResult {
    param(
        [System.Collections.Generic.List[object]]$Results,
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [int]$Status,
        [int[]]$Expected,
        [string]$Body
    )

    $passed = $Expected -contains $Status
    $Results.Add([pscustomobject]@{
        name = $Name
        method = $Method
        url = $Url
        status = $Status
        expected = ($Expected -join ",")
        passed = $passed
        body = $Body
    }) | Out-Null

    if ($passed) {
        Write-Host "[PASS] $Name => $Status"
    } else {
        Write-Host "[FAIL] $Name => $Status expected $($Expected -join ',')"
        if ($Body) { Write-Host $Body }
    }
}

function Invoke-G1Test {
    param(
        [System.Collections.Generic.List[object]]$Results,
        [hashtable]$Headers,
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [int[]]$Expected = @(200)
    )

    $url = "$G1Url$Path"

    try {
        if ($null -eq $Body) {
            $response = Invoke-WebRequest -UseBasicParsing -Method $Method -Uri $url -Headers $Headers -TimeoutSec 30
        } else {
            $json = $Body | ConvertTo-Json -Compress -Depth 10
            $response = Invoke-WebRequest -UseBasicParsing -Method $Method -Uri $url -Headers $Headers -ContentType "application/json" -Body $json -TimeoutSec 30
        }

        Add-LinkResult -Results $Results -Name $Name -Method $Method -Url $url -Status ([int]$response.StatusCode) -Expected $Expected -Body ([string]$response.Content)
        return [string]$response.Content
    } catch {
        $status = 0
        if ($null -ne $_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
        }
        $text = Get-ErrorText $_.Exception
        Add-LinkResult -Results $Results -Name $Name -Method $Method -Url $url -Status $status -Expected $Expected -Body $text
        return $text
    }
}

function Invoke-MySqlScalar {
    param([string]$Sql)

    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $out = & docker exec payment-db mysql -N -B -usgitu -psgitupassword payment_db -e $Sql 2>&1
    $ErrorActionPreference = $previousPreference

    if ($LASTEXITCODE -ne 0) {
        throw (($out | Out-String).Trim())
    }

    $clean = $out | Where-Object { $_ -notmatch "^mysql: \[Warning\]" }
    return (($clean | Select-Object -First 1) -as [string])
}

function Invoke-G1G6LinkTests {
    $suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $payTicket = "G1-PAY-$suffix"
    $refundTicket = "G1-REFUND-$suffix"
    $headers = @{
        "X-User-Id" = "11111111-1111-1111-1111-111111111111"
        "X-User-Email" = "tester@g1.local"
        "X-Roles" = "ROLE_USER"
    }
    $results = New-Object System.Collections.Generic.List[object]

    Write-Host ""
    Write-Host "== Preparing G1 Mongo tickets =="
    $mongoEval = "db.tickets.insertMany([{_id:'$payTicket', tripId:'TRIP-TEST', price:9, currency:'MAD', ticket_type:'ONE_WAY', ticket_class:'ORDINARY', holder_id:'1', token_value:'TOKEN-$payTicket', identity_method:'QR_CODE', status:'CREATED', transfer_history:[], metadata:{}, expires_at:new Date(Date.now()+86400000), created_at:new Date(), updated_at:new Date()}, {_id:'$refundTicket', tripId:'TRIP-TEST', price:8, currency:'MAD', ticket_type:'ONE_WAY', ticket_class:'ORDINARY', holder_id:'1', token_value:'TOKEN-$refundTicket', identity_method:'QR_CODE', status:'ISSUED', transfer_history:[], metadata:{}, expires_at:new Date(Date.now()+86400000), issued_at:new Date(), created_at:new Date(), updated_at:new Date()}]);"
    docker exec billetterie-mongo mongosh -u admin -p change_me --authenticationDatabase admin billetterie --quiet --eval $mongoEval | Out-Null

    Write-Host "== Preparing G6 payment for G1 refund ticket =="
    $paymentBody = @{
        userId = 1
        sourceType = "TICKET"
        sourceId = $refundTicket
        amount = 8.00
        paymentMethod = "CARD"
        savedPaymentToken = "CARD-TOKEN-001"
        email = "refund-g1@test.local"
    } | ConvertTo-Json -Compress
    Invoke-WebRequest -UseBasicParsing -Method POST -Uri "$G6Url/payments" -ContentType "application/json" -Body $paymentBody -TimeoutSec 30 | Out-Null

    Write-Host ""
    Write-Host "== Running real G1 -> G6 link tests =="
    Invoke-G1Test -Results $results -Headers $headers -Name "G1 health" -Method GET -Path "/actuator/health" -Expected @(200) | Out-Null
    Invoke-G1Test -Results $results -Headers $headers -Name "G1 get CREATED ticket" -Method GET -Path "/api/v1/tickets/$payTicket" -Expected @(200) | Out-Null
    Invoke-G1Test -Results $results -Headers $headers -Name "G1 get ISSUED ticket" -Method GET -Path "/api/v1/tickets/$refundTicket" -Expected @(200) | Out-Null
    Invoke-G1Test -Results $results -Headers $headers -Name "G1 pay ticket with current G1 DTO" -Method POST -Path "/api/v1/tickets/$payTicket/pay" -Body @{
        userId = "1"
        paymentMethod = "CARD"
        savedPaymentToken = "CARD-TOKEN-001"
    } -Expected @(422) | Out-Null
    Invoke-G1Test -Results $results -Headers $headers -Name "G1 refund ticket through G6 compatibility endpoint" -Method POST -Path "/api/v1/tickets/$refundTicket/refund" -Expected @(200) | Out-Null

    $ticketStatus = docker exec billetterie-mongo mongosh -u admin -p change_me --authenticationDatabase admin billetterie --quiet --eval "print(db.tickets.findOne({_id:'$refundTicket'}).status)"
    $refundCount = Invoke-MySqlScalar "SELECT COUNT(*) FROM refunds r JOIN payments p ON p.id=r.payment_id WHERE p.source_id='$refundTicket' AND r.status='REFUNDED';"

    Add-LinkResult -Results $results -Name "Mongo G1 ticket marked REFUNDED" -Method "MONGO" -Url $refundTicket -Status $(if ($ticketStatus -eq "REFUNDED") { 200 } else { 500 }) -Expected @(200) -Body "status=$ticketStatus"
    Add-LinkResult -Results $results -Name "MySQL G6 refund row created for G1 ticket" -Method "SQL" -Url $refundTicket -Status $(if ([int]$refundCount -ge 1) { 200 } else { 500 }) -Expected @(200) -Body "refundCount=$refundCount"

    $summary = [pscustomobject]@{
        generatedAt = (Get-Date).ToString("s")
        total = $results.Count
        passed = @($results | Where-Object { $_.passed }).Count
        failed = @($results | Where-Object { -not $_.passed }).Count
        payTicket = $payTicket
        refundTicket = $refundTicket
        results = $results
    }

    $jsonPath = Join-Path $TestDir "g1-link-test-results.json"
    $mdPath = Join-Path $TestDir "g1-link-test-results.md"
    $summary | ConvertTo-Json -Depth 20 | Set-Content -Path $jsonPath -Encoding UTF8

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add("# G1-G6 link test results")
    $lines.Add("")
    $lines.Add("Summary: $($summary.passed)/$($summary.total) passed, $($summary.failed) failed.")
    $lines.Add("")
    $lines.Add("| Test | Method | Status | Expected | Result |")
    $lines.Add("|---|---:|---:|---:|---|")
    foreach ($r in $results) {
        $label = if ($r.passed) { "PASS" } else { "FAIL" }
        $lines.Add("| $($r.name) | $($r.method) | $($r.status) | $($r.expected) | $label |")
    }
    $lines | Set-Content -Path $mdPath -Encoding UTF8

    Write-Host "SUMMARY_G1_G6 passed=$($summary.passed) total=$($summary.total) failed=$($summary.failed)"

    if ($summary.failed -gt 0) {
        throw "G1-G6 link tests failed"
    }
}

try {
    Stop-PortProcess -Ports @(18081, 18086)

    Write-Host ""
    Write-Host "== Starting Docker dependencies =="
    Push-Location $Root
    docker compose up -d payment-db zookeeper kafka | Out-Host
    Pop-Location

    $env:MONGO_ROOT_USERNAME = "admin"
    $env:MONGO_ROOT_PASSWORD = "change_me"
    $env:MONGO_DATABASE = "billetterie"
    $env:KAFKA_CLUSTER_ID = "sgitu-kafka-cluster-001"
    Push-Location $G1Root
    docker compose up -d mongodb | Out-Host
    Pop-Location

    Invoke-Checked -Title "Compile G6" -WorkDir $Root -Command ".\mvnw.cmd -q -DskipTests compile"
    Invoke-Checked -Title "Compile G1 with Java 21 target" -WorkDir $G1Root -Command ".\mvnw.cmd -q -DskipTests -Djava.version=21 compile"

    Write-Host ""
    Write-Host "== Starting G6 locally on port 18086 =="
    Push-Location $Root
    $env:SPRING_PROFILES_ACTIVE = "docker"
    $env:SERVER_PORT = "18086"
    $env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3316/payment_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
    $env:SPRING_DATASOURCE_USERNAME = "sgitu"
    $env:SPRING_DATASOURCE_PASSWORD = "sgitupassword"
    $env:SPRING_KAFKA_BOOTSTRAP_SERVERS = "localhost:29093"
    $env:SUBSCRIPTION_SERVICE_URL = "http://localhost:65532"
    $env:TICKET_SERVICE_URL = "http://localhost:65531"
    $env:NOTIFICATION_SERVICE_URL = "http://localhost:65530"
    Start-Process -FilePath ".\mvnw.cmd" -ArgumentList "spring-boot:run" -RedirectStandardOutput (Join-Path $TestDir "payment-local.out") -RedirectStandardError (Join-Path $TestDir "payment-local.err") -WindowStyle Hidden | Out-Null
    Pop-Location
    Wait-HttpOk -Url "$G6Url/actuator/health" -TimeoutSeconds 180

    Write-Host ""
    Write-Host "== Running G6 endpoint tests =="
    Push-Location $Root
    powershell -ExecutionPolicy Bypass -File "target\test-run\run-endpoint-tests.ps1"
    if ($LASTEXITCODE -ne 0) { throw "G6 endpoint tests failed" }
    Pop-Location

    Write-Host ""
    Write-Host "== Starting G1 locally on port 18081 =="
    Push-Location $G1Root
    $env:SERVER_PORT = "18081"
    $env:MONGO_URI = "mongodb://admin:change_me@localhost:27017/billetterie?authSource=admin"
    $env:SPRING_KAFKA_BOOTSTRAP_SERVERS = "localhost:29093"
    $env:PAYMENT_SERVICE_URL = $G6Url
    $env:COORDINATION_SERVICE_URL = "http://localhost:65529"
    Start-Process -FilePath ".\mvnw.cmd" -ArgumentList "spring-boot:run", "-Djava.version=21" -RedirectStandardOutput (Join-Path $TestDir "g1-local.out") -RedirectStandardError (Join-Path $TestDir "g1-local.err") -WindowStyle Hidden | Out-Null
    Pop-Location
    Wait-HttpOk -Url "$G1Url/actuator/health" -TimeoutSeconds 180

    Invoke-G1G6LinkTests

    Write-Host ""
    Write-Host "ALL TESTS PASSED"
    Write-Host "G6 report: target\test-run\endpoint-test-results.md"
    Write-Host "G1-G6 report: target\test-run\g1-link-test-results.md"
} finally {
    Write-Host ""
    Write-Host "== Stopping local Maven services on 18081 and 18086 =="
    Stop-PortProcess -Ports @(18081, 18086)
}
