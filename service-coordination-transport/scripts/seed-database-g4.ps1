# Charge le jeu de données démo G4 dans PostgreSQL
# Usage : powershell -ExecutionPolicy Bypass -File scripts/seed-database-g4.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$SqlFile = Join-Path $Root "src\main\resources\db\postgresql-seed-demo-g4.sql"
if (-not (Test-Path $SqlFile)) {
    throw "Fichier introuvable : $SqlFile"
}

$Container = $env:SGITU_G4_PG_CONTAINER
if (-not $Container) { $Container = "sgitu-g4-postgres" }

$Db = $env:POSTGRES_DB
if (-not $Db) { $Db = "sgitu_g4" }

$User = $env:POSTGRES_USER
if (-not $User) { $User = "g4" }

Write-Host "SGITU G4 — chargement seed demo"
Write-Host "  SQL   : $SqlFile"
Write-Host "  Docker: $Container / $Db"

$running = docker ps --filter "name=$Container" --format "{{.Names}}" 2>$null
if ($running -eq $Container) {
    Get-Content -Raw $SqlFile | docker exec -i $Container psql -U $User -d $Db
    if ($LASTEXITCODE -ne 0) { throw "docker exec psql a échoué" }
    Write-Host "OK — données démo chargées."
    exit 0
}

$HostPort = $env:SGITU_G4_PG_PORT
if (-not $HostPort) { $HostPort = "5434" }

$psql = Get-Command psql -ErrorAction SilentlyContinue
if ($psql) {
    $env:PGPASSWORD = if ($env:POSTGRES_PASSWORD) { $env:POSTGRES_PASSWORD } else { "g4" }
    & psql -h localhost -p $HostPort -U $User -d $Db -f $SqlFile
    Write-Host "OK — données démo chargées (psql local)."
    exit 0
}

Write-Host "ERREUR : démarrez Docker (sgitu-g4-postgres) ou installez psql."
Write-Host "  docker compose up -d postgres"
exit 1
