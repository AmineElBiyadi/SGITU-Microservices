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
                    Write-Host "Stopping existing process $($_.OwningProcess) on port $port"
                    Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
                }
    }
}

function Wait-HttpOk {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 180
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

function Invoke-CommandChecked {
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

function Prepare-G1SwaggerTickets {
    $suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $payTicket = "SWAGGER-PAY-$suffix"
    $refundTicket = "SWAGGER-REFUND-$suffix"

    Write-Host ""
    Write-Host "== Preparing Swagger test data =="

    $mongoEval = "db.tickets.insertMany([{_id:'$payTicket', tripId:'TRIP-SWAGGER', price:9, currency:'MAD', ticket_type:'ONE_WAY', ticket_class:'ORDINARY', holder_id:'1', token_value:'TOKEN-$payTicket', identity_method:'QR_CODE', status:'CREATED', transfer_history:[], metadata:{}, expires_at:new Date(Date.now()+86400000), created_at:new Date(), updated_at:new Date()}, {_id:'$refundTicket', tripId:'TRIP-SWAGGER', price:8, currency:'MAD', ticket_type:'ONE_WAY', ticket_class:'ORDINARY', holder_id:'1', token_value:'TOKEN-$refundTicket', identity_method:'QR_CODE', status:'ISSUED', transfer_history:[], metadata:{}, expires_at:new Date(Date.now()+86400000), issued_at:new Date(), created_at:new Date(), updated_at:new Date()}]);"
    docker exec billetterie-mongo mongosh -u admin -p change_me --authenticationDatabase admin billetterie --quiet --eval $mongoEval | Out-Null

    $paymentBody = @{
        userId = 1
        sourceType = "TICKET"
        sourceId = $refundTicket
        amount = 8.00
        paymentMethod = "CARD"
        savedPaymentToken = "CARD-TOKEN-001"
        email = "swagger-refund@test.local"
    } | ConvertTo-Json -Compress

    $paymentResponse = Invoke-WebRequest -UseBasicParsing -Method POST -Uri "$G6Url/payments" -ContentType "application/json" -Body $paymentBody -TimeoutSec 30

    $info = [pscustomobject]@{
        generatedAt = (Get-Date).ToString("s")
        g6Swagger = "$G6Url/swagger-ui.html"
        g6SwaggerIndex = "$G6Url/swagger-ui/index.html"
        g1Swagger = "$G1Url/swagger-ui/index.html"
        g1SwaggerHtml = "$G1Url/swagger-ui.html"
        g1PayTicketId = $payTicket
        g1RefundTicketId = $refundTicket
        g6RefundPreparationPayment = ($paymentResponse.Content | ConvertFrom-Json)
        g1Headers = @{
            "X-User-Id" = "11111111-1111-1111-1111-111111111111"
            "X-User-Email" = "tester@g1.local"
            "X-Roles" = "ROLE_USER"
        }
    }

    $jsonPath = Join-Path $TestDir "swagger-env-info.json"
    $mdPath = Join-Path $TestDir "swagger-env-info.md"

    $info | ConvertTo-Json -Depth 20 | Set-Content -Path $jsonPath -Encoding UTF8

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add("# Swagger test environment")
    $lines.Add("")
    $lines.Add("G6 Swagger: $($info.g6Swagger)")
    $lines.Add("G1 Swagger: $($info.g1Swagger)")
    $lines.Add("")
    $lines.Add("G1 headers to use in Swagger:")
    $lines.Add("")
    $lines.Add('```text')
    $lines.Add("X-User-Id: 11111111-1111-1111-1111-111111111111")
    $lines.Add("X-User-Email: tester@g1.local")
    $lines.Add("X-Roles: ROLE_USER")
    $lines.Add('```')
    $lines.Add("")
    $lines.Add("G1 ticket IDs:")
    $lines.Add("")
    $lines.Add('```text')
    $lines.Add("Pay ticket: $payTicket")
    $lines.Add("Refund ticket: $refundTicket")
    $lines.Add('```')
    $lines.Add("")
    $lines.Add("G6 POST /payments payload:")
    $lines.Add("")
    $lines.Add('```json')
    $lines.Add('{"userId":1,"sourceType":"TICKET","sourceId":"SWAGGER-MANUAL-001","amount":10.00,"paymentMethod":"CARD","savedPaymentToken":"CARD-TOKEN-001","email":"client@test.local"}')
    $lines.Add('```')
    $lines.Add("")
    $lines.Add("G1 POST /api/v1/tickets/{ticketId}/pay current payload, expected 422 because G1 DTO is incomplete:")
    $lines.Add("")
    $lines.Add('```json')
    $lines.Add('{"userId":"1","paymentMethod":"CARD","savedPaymentToken":"CARD-TOKEN-001"}')
    $lines.Add('```')
    $lines.Add("")
    $lines.Add("G1 POST /api/v1/tickets/{ticketId}/refund uses the refund ticket above and should return 200.")
    $lines | Set-Content -Path $mdPath -Encoding UTF8

    Write-Host "Swagger info written to: $mdPath"
    Write-Host "Pay ticket: $payTicket"
    Write-Host "Refund ticket: $refundTicket"
}

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

Invoke-CommandChecked -Title "Compile G6" -WorkDir $Root -Command ".\mvnw.cmd -q -DskipTests compile"
Invoke-CommandChecked -Title "Compile G1 with Java 21 target" -WorkDir $G1Root -Command ".\mvnw.cmd -q -DskipTests -Djava.version=21 compile"

Write-Host ""
Write-Host "== Starting G6 on http://localhost:18086 =="
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
Start-Process -FilePath ".\mvnw.cmd" -ArgumentList "spring-boot:run" -RedirectStandardOutput (Join-Path $TestDir "payment-swagger.out") -RedirectStandardError (Join-Path $TestDir "payment-swagger.err") -WindowStyle Hidden | Out-Null
Pop-Location
Wait-HttpOk -Url "$G6Url/actuator/health"

Write-Host ""
Write-Host "== Starting G1 on http://localhost:18081 =="
Push-Location $G1Root
$env:SERVER_PORT = "18081"
$env:MONGO_URI = "mongodb://admin:change_me@localhost:27017/billetterie?authSource=admin"
$env:SPRING_KAFKA_BOOTSTRAP_SERVERS = "localhost:29093"
$env:PAYMENT_SERVICE_URL = $G6Url
$env:COORDINATION_SERVICE_URL = "http://localhost:65529"
Start-Process -FilePath ".\mvnw.cmd" -ArgumentList "spring-boot:run", "-Djava.version=21" -RedirectStandardOutput (Join-Path $TestDir "g1-swagger.out") -RedirectStandardError (Join-Path $TestDir "g1-swagger.err") -WindowStyle Hidden | Out-Null
Pop-Location
Wait-HttpOk -Url "$G1Url/actuator/health"

Prepare-G1SwaggerTickets

Write-Host ""
Write-Host "== Swagger URLs =="
Write-Host "G6 Swagger: $G6Url/swagger-ui.html"
Write-Host "G6 Swagger alternative: $G6Url/swagger-ui/index.html"
Write-Host "G1 Swagger: $G1Url/swagger-ui/index.html"
Write-Host "G1 Swagger alternative: $G1Url/swagger-ui.html"
Write-Host ""
Write-Host "To stop local Swagger services:"
Write-Host "powershell -ExecutionPolicy Bypass -File scripts\stop-swagger-env.ps1"
