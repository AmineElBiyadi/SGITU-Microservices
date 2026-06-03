# Script de nettoyage PowerShell pour Windows
# Usage: .\cleanup.ps1 [-Force]

param(
    [switch]$Force
)

$NAMESPACE = "sgitu"

Write-Host "🗑️  Nettoyage du User Service sur Kubernetes" -ForegroundColor Yellow
Write-Host "=============================================" -ForegroundColor Yellow

function Log-Info {
    param($Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Log-Warn {
    param($Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

if (-not $Force) {
    $confirmation = Read-Host "Cette action va supprimer tous les composants du namespace $NAMESPACE. Êtes-vous sûr ? (yes/no)"
    if ($confirmation -ne "yes") {
        Log-Info "Annulation du nettoyage"
        exit 0
    }
}

Log-Info "Suppression des ressources Kubernetes..."

# Supprimer dans l'ordre inverse
Log-Info "Suppression des Network Policies..."
kubectl delete -f network-policy.yaml --ignore-not-found=true

Log-Info "Suppression du ServiceMonitor..."
kubectl delete -f servicemonitor.yaml --ignore-not-found=true 2>$null

Log-Info "Suppression de l'HPA..."
kubectl delete -f hpa.yaml --ignore-not-found=true

Log-Info "Suppression de l'Ingress..."
kubectl delete -f ingress.yaml --ignore-not-found=true

Log-Info "Suppression du User Service..."
kubectl delete -f user-service-service.yaml --ignore-not-found=true
kubectl delete -f user-service-deployment.yaml --ignore-not-found=true

Log-Info "Suppression de Kafka..."
kubectl delete -f kafka-service.yaml --ignore-not-found=true
kubectl delete -f kafka-deployment.yaml --ignore-not-found=true

Log-Info "Suppression de Redis..."
kubectl delete -f redis-service.yaml --ignore-not-found=true
kubectl delete -f redis-deployment.yaml --ignore-not-found=true

Log-Info "Suppression de PostgreSQL..."
kubectl delete -f postgres-service.yaml --ignore-not-found=true
kubectl delete -f postgres-deployment.yaml --ignore-not-found=true
kubectl delete -f postgres-pvc.yaml --ignore-not-found=true

Log-Info "Suppression des ConfigMaps et Secrets..."
kubectl delete -f secrets.yaml --ignore-not-found=true
kubectl delete -f configmap.yaml --ignore-not-found=true

if ($Force) {
    Log-Warn "Suppression du namespace $NAMESPACE..."
    kubectl delete namespace $NAMESPACE --ignore-not-found=true
} else {
    Log-Info "Namespace $NAMESPACE conservé (utilisez -Force pour le supprimer)"
}

Log-Info "✓ Nettoyage terminé"

# Vérifier qu'il ne reste rien
$remaining = kubectl get all -n $NAMESPACE 2>$null
if ($remaining) {
    Log-Warn "Il reste des ressources dans le namespace:"
    kubectl get all -n $NAMESPACE
} else {
    Log-Info "Toutes les ressources ont été supprimées"
}
