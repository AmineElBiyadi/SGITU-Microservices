# Déploie G4 sur Kubernetes (simulation) — Postgres/Kafka via docker compose.
# Usage : .\scripts\deploy-g4-k8s.ps1
#        .\scripts\deploy-g4-k8s.ps1 -SkipBuild

param(
    [switch]$SkipBuild,
    [switch]$NoPortForward
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Image = "sgitu/g4-coordination:0.0.1-SNAPSHOT"
$Namespace = "sgitu-g4"

Set-Location $Root

function Test-Command($name) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        throw "Commande introuvable : $name"
    }
}

Test-Command kubectl
Test-Command docker

Write-Host "==> Infra Postgres + Kafka (Compose, sans conteneur G4)" -ForegroundColor Cyan
docker compose stop g4-coordination 2>$null | Out-Null
docker compose up -d postgres kafka
Start-Sleep -Seconds 5

if (-not $SkipBuild) {
    Write-Host "==> Build image $Image" -ForegroundColor Cyan
    docker build -t $Image .
}

$ctx = kubectl config current-context 2>$null
if ($ctx -match "minikube") {
    Write-Host "==> Contexte Minikube : chargement image" -ForegroundColor Cyan
    Test-Command minikube
    minikube image load $Image
    $hostHint = "host.minikube.internal"
    Write-Host "    Pensez a K8S_INFRA_HOST=$hostHint dans k8s/configmap.yaml si la DB echoue." -ForegroundColor Yellow
}

Write-Host "==> kubectl apply -k k8s/" -ForegroundColor Cyan
kubectl apply -k k8s/

Write-Host "==> Attente pod Ready (max 3 min)..." -ForegroundColor Cyan
kubectl wait --for=condition=ready pod -l app=g4-coordination -n $Namespace --timeout=180s

kubectl get pods,svc -n $Namespace

if (-not $NoPortForward) {
    Write-Host ""
    Write-Host "==> API : kubectl port-forward -n $Namespace svc/g4-coordination 8084:8084" -ForegroundColor Green
    Write-Host "    Health : http://localhost:8084/api/g4/health" -ForegroundColor Green
    Write-Host "    (Ctrl+C pour arreter le port-forward)" -ForegroundColor DarkGray
    kubectl port-forward -n $Namespace svc/g4-coordination 8084:8084
}
