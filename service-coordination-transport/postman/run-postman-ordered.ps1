# Exécute tous les dossiers Postman G4 dans l'ordre (Newman).
# Prérequis : G4 UP sur http://localhost:8084

$ErrorActionPreference = 'Stop'
$BaseUrl = 'http://localhost:8084'
$Collection = Join-Path $PSScriptRoot 'SGITU-G4-Coordination-Transport.postman_collection.json'
$VehiculeId = '00000000-0000-4000-8000-000000000001'

function Write-Step($msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }

function Get-G4Token($username) {
	$body = @{ username = $username; password = 'password' } | ConvertTo-Json
	$r = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method POST -Body $body -ContentType 'application/json'
	return $r.token
}

function Seed-VehiculeReferentiel {
	$pg = @('g4-postgres', 'sgitu-g4-postgres') | Where-Object { docker ps --format '{{.Names}}' | Select-String -Pattern "^$_$" -Quiet } | Select-Object -First 1
	if (-not $pg) {
		Write-Host 'Postgres G4 introuvable — sync G7 ou Kafka requis pour vehiculeId' -ForegroundColor Yellow
		return
	}
	$sql = @"
CREATE TABLE IF NOT EXISTS vehicules_referentiel (
    vehicule_id VARCHAR(64) PRIMARY KEY,
    immatriculation VARCHAR(32),
    type_vehicule VARCHAR(32),
    statut_g7 VARCHAR(32) NOT NULL,
    disponible_pour_affectation BOOLEAN NOT NULL DEFAULT TRUE,
    ligne_affectee_id BIGINT,
    registered_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ
);
INSERT INTO vehicules_referentiel (vehicule_id, immatriculation, type_vehicule, statut_g7, disponible_pour_affectation, registered_at, updated_at)
VALUES ('$VehiculeId', 'BUS-POSTMAN-001', 'BUS', 'DISPONIBLE', true, NOW(), NOW())
ON CONFLICT (vehicule_id) DO UPDATE SET
  statut_g7 = 'DISPONIBLE',
  disponible_pour_affectation = true,
  updated_at = NOW();
"@
	docker exec $pg psql -U g4 -d sgitu_g4 -c $sql | Out-Null
	Write-Host "Référentiel véhicule $VehiculeId prêt dans $pg"
}

function Invoke-NewmanFolder($folderName, $token) {
	$args = @(
		'run', $Collection,
		'--folder', $folderName,
		'--env-var', "baseUrl=$BaseUrl",
		'--env-var', "vehiculeId=$VehiculeId",
		'--color', 'on',
		'--reporters', 'cli',
		'--disable-unicode',
		'--bail', 'failure'
	)
	if ($token) {
		$args += '--env-var', "accessToken=$token"
	}
	& npx --yes newman @args
	if ($LASTEXITCODE -ne 0) { throw "Newman échec : $folderName" }
}

Write-Step 'Regénération collection'
Push-Location (Split-Path $PSScriptRoot -Parent)
node postman/build-collection.js
Pop-Location

Write-Step 'Health G4'
$h = Invoke-RestMethod -Uri "$BaseUrl/api/g4/health" -Method GET
if ($h.status -ne 'UP') { throw 'G4 non UP' }

Write-Step 'Seed véhicule référentiel (tests 6b/7/8)'
Seed-VehiculeReferentiel

$plan = @(
	@{ Folder = '00 — Démarrage'; User = $null },
	@{ Folder = 'GUIDE — Parcours complet (1→13)'; User = $null },
	@{ Folder = '02 — Lignes'; User = 'gestionnaire.reseau' },
	@{ Folder = '03 — Arrêts'; User = 'gestionnaire.reseau' },
	@{ Folder = '04 — Trajets'; User = 'gestionnaire.reseau' },
	@{ Folder = '05 — Horaires'; User = 'gestionnaire.reseau' },
	@{ Folder = '05b — Référentiel véhicules G7'; User = 'gestionnaire.flotte' },
	@{ Folder = '06 — Affectations'; User = 'gestionnaire.flotte' },
	@{ Folder = '07 — Missions'; User = 'gestionnaire.flotte' },
	@{ Folder = '08 — Événements coordination'; User = 'gestionnaire.flotte' },
	@{ Folder = '09 — Impacts incident G9'; User = 'gestionnaire.flotte' },
	@{ Folder = '10 — Notifications (G5)'; User = 'gestionnaire.flotte' },
	@{ Folder = '11 — Référence G7 (lecture v1)'; User = 'gestionnaire.reseau' },
	@{ Folder = '12 — Supervision'; User = 'admin.technique' },
	@{ Folder = '90 — Erreurs'; User = 'gestionnaire.reseau' },
	@{ Folder = '101 — OpenAPI'; User = $null }
)

$failed = @()
foreach ($step in $plan) {
	Write-Step $step.Folder
	$token = $null
	if ($step.User) {
		try { $token = Get-G4Token $step.User } catch { Write-Host "Login $($step.User) échoué" -ForegroundColor Red }
	}
	try {
		Invoke-NewmanFolder $step.Folder $token
	} catch {
		$failed += $step.Folder
		Write-Host "ERREUR: $($_.Exception.Message)" -ForegroundColor Red
	}
}

# Optionnels (ne bloquent pas le rapport final)
foreach ($opt in @('99 — Intégration G3 (optionnel)', '100 — Chaos G5 (optionnel)')) {
	Write-Step "$opt (optionnel)"
	if ($opt -like '99*') {
		try {
			Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8083/api/auth/login' -Method Post -ContentType 'application/json' -Body '{"username":"x","password":"x"}' -TimeoutSec 2 | Out-Null
		} catch {
			Write-Host 'Ignoré: 99 — G3 non démarré sur 8083' -ForegroundColor Yellow
			continue
		}
	}
	try { Invoke-NewmanFolder $opt (Get-G4Token 'gestionnaire.flotte') } catch { Write-Host "Ignoré: $opt" -ForegroundColor Yellow }
}

Write-Step 'Résumé'
if ($failed.Count -eq 0) {
	Write-Host 'Tous les dossiers obligatoires : OK' -ForegroundColor Green
	exit 0
}
Write-Host "Échecs: $($failed -join ', ')" -ForegroundColor Red
exit 1
