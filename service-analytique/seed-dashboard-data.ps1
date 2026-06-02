# ========================================================================
#   DASHBOARD SEEDING SCRIPT (Populates Grafana with realistic data)
# ========================================================================
$BaseUrl = "http://localhost:8088"
$JwtSecret = "sgitu_g8_secret_key_2025_very_long_secret_for_analytics"

Write-Host "Generating JWT Token..."
function New-MockJwtToken {
    param (
        [string]$Subject = "integration-tester",
        [string[]]$Roles = @("ADMIN")
    )
    # Header (Base64UrlEncoded)
    $Header = @{ alg = "HS256"; typ = "JWT" }
    $HeaderJson = $Header | ConvertTo-Json -Compress
    $HeaderBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($HeaderJson)).Split('=')[0].Replace('+', '-').Replace('/', '_')

    # Payload (Base64UrlEncoded)
    $Exp = [DateTimeOffset]::UtcNow.AddHours(1).ToUnixTimeSeconds()
    $Payload = @{
        sub = $Subject
        roles = $Roles
        exp = $Exp
    }
    $PayloadJson = $Payload | ConvertTo-Json -Compress
    $PayloadBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($PayloadJson)).Split('=')[0].Replace('+', '-').Replace('/', '_')

    # Signature
    $SignatureInput = "$HeaderBase64.$PayloadBase64"
    $Hmac = New-Object System.Security.Cryptography.HMACSHA256
    $Hmac.Key = [Text.Encoding]::UTF8.GetBytes($JwtSecret)
    $SignatureBytes = $Hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($SignatureInput))
    $SignatureBase64 = [Convert]::ToBase64String($SignatureBytes).Split('=')[0].Replace('+', '-').Replace('/', '_')

    return "$HeaderBase64.$PayloadBase64.$SignatureBase64"
}

$JwtToken = New-MockJwtToken -Subject "dashboard-seeder" -Roles @("ADMIN", "SYSTEM")

$Headers = @{
    "Authorization" = "Bearer $JwtToken"
    "Content-Type" = "application/json"
}

$CurrentTime = (Get-Date).ToUniversalTime()

function Generate-Timestamp() {
    # Spread data over the last 7 days to show rich historical trends in the dashboard
    $offsetHours = Get-Random -Minimum 0 -Maximum 168 # 7 days * 24h = 168h
    $offsetMinutes = Get-Random -Minimum 0 -Maximum 60
    return $CurrentTime.AddHours(-$offsetHours).AddMinutes(-$offsetMinutes).ToString("yyyy-MM-ddTHH:mm:ssZ")
}

# 1. VEHICLES (G6)
Write-Host "Seeding Vehicle Events..."
$Vehicles = @()
for ($i=1; $i -le 100; $i++) {
    $Vehicles += @{
        schemaVersion = 1
        timestamp = Generate-Timestamp
        vehicleId = "VEH-$((Get-Random -Minimum 1 -Maximum 20))"
        status = "in_service"
        line = "L$((Get-Random -Minimum 1 -Maximum 5))"
        speed = (Get-Random -Minimum 30 -Maximum 80)
    }
}
$VehiclesJson = $Vehicles | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "$BaseUrl/api/v1/ingestion/vehicles" -Method Post -Headers $Headers -Body $VehiclesJson | Out-Null

# 2. INCIDENTS (G7)
Write-Host "Seeding Incident Events..."
$Incidents = @()
$IncidentTypes = @("delay", "breakdown", "accident")
$Severities = @("LOW", "MEDIUM", "HIGH", "CRITICAL")
for ($i=1; $i -le 15; $i++) {
    $Incidents += @{
        schemaVersion = 1
        timestamp = Generate-Timestamp
        incidentId = "INC-$i"
        type = $IncidentTypes[(Get-Random -Minimum 0 -Maximum 3)]
        severity = $Severities[(Get-Random -Minimum 0 -Maximum 4)]
        latitude = (33.5 + (Get-Random -Minimum 1 -Maximum 100) / 1000.0) 
        longitude = (-7.6 + (Get-Random -Minimum 1 -Maximum 100) / 1000.0)
    }
}
$IncidentsJson = $Incidents | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "$BaseUrl/api/v1/ingestion/incidents" -Method Post -Headers $Headers -Body $IncidentsJson | Out-Null

# 3. TICKETS (G2)
Write-Host "Seeding Ticketing Events..."
$Tickets = @()
for ($i=1; $i -le 300; $i++) {
    $Tickets += @{
        schemaVersion = 1
        timestamp = Generate-Timestamp
        userId = "user-$((Get-Random -Minimum 1 -Maximum 100))"
        status = "validated"
        line = "L$((Get-Random -Minimum 1 -Maximum 5))"
        stationId = "ST-$((Get-Random -Minimum 100 -Maximum 120))"
    }
}
$TicketsJson = $Tickets | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "$BaseUrl/api/v1/ingestion/tickets" -Method Post -Headers $Headers -Body $TicketsJson | Out-Null

# 4. SUBSCRIPTIONS (G3)
Write-Host "Seeding Subscription Events..."
$Subscriptions = @()
for ($i=1; $i -le 50; $i++) {
    $Subscriptions += @{
        schemaVersion = 1
        timestamp = Generate-Timestamp
        userId = "user-$((Get-Random -Minimum 1 -Maximum 100))"
        action = "created"
    }
}
$SubscriptionsJson = $Subscriptions | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "$BaseUrl/api/v1/ingestion/subscriptions" -Method Post -Headers $Headers -Body $SubscriptionsJson | Out-Null

# 5. USERS (G1)
Write-Host "Seeding User Events..."
$Users = @()
for ($i=1; $i -le 100; $i++) {
    $Users += @{
        schemaVersion = 1
        timestamp = Generate-Timestamp
        userId = "user-$i"
        action = "active"
    }
}
$UsersJson = $Users | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "$BaseUrl/api/v1/ingestion/users" -Method Post -Headers $Headers -Body $UsersJson | Out-Null

# 6. PAYMENTS (G4)
Write-Host "Seeding Payment Events..."
$Payments = @()
for ($i=1; $i -le 200; $i++) {
    $Payments += @{
        schemaVersion = 1
        timestamp = Generate-Timestamp
        transactionId = "tx-$i"
        status = "completed"
        amount = (Get-Random -Minimum 10 -Maximum 100)
        paymentMethod = "CARD"
    }
}
$PaymentsJson = $Payments | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "$BaseUrl/api/v1/ingestion/payments" -Method Post -Headers $Headers -Body $PaymentsJson | Out-Null

Write-Host "Data ingested successfully! Triggering aggregation job..."
Invoke-RestMethod -Uri "$BaseUrl/test/run" -Method Get -Headers $Headers | Out-Null

Write-Host "Aggregation complete! Waiting 15 seconds for Prometheus to scrape the new metrics..."
Start-Sleep -Seconds 15

Write-Host "Done! Refresh your Grafana dashboard now!"
