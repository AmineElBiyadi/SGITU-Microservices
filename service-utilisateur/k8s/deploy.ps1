# Script de deploiement PowerShell pour Windows
# Usage: .\deploy.ps1

$ErrorActionPreference = "Stop"
$NAMESPACE = "sgitu"

Write-Host "Deploiement du User Service sur Kubernetes" -ForegroundColor Green
Write-Host "===========================================" -ForegroundColor Green

function Log-Info {
    param($Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Log-Warn {
    param($Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Log-Error {
    param($Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

# Verifier kubectl
try {
    kubectl version --client | Out-Null
    Log-Info "kubectl installe"
} catch {
    Log-Error "kubectl n'est pas installe"
    exit 1
}

# Verifier la connexion au cluster
try {
    kubectl cluster-info | Out-Null
    Log-Info "Connexion au cluster OK"
} catch {
    Log-Error "Impossible de se connecter au cluster Kubernetes"
    exit 1
}

# 1. Creer le namespace
Log-Info "Creation du namespace $NAMESPACE..."
kubectl apply -f namespace.yaml
Start-Sleep -Seconds 2

# 2. Appliquer les ConfigMaps et Secrets
Log-Info "Application des ConfigMaps et Secrets..."
kubectl apply -f configmap.yaml
kubectl apply -f secrets.yaml

# 3. Deployer PostgreSQL
Log-Info "Deploiement de PostgreSQL..."
kubectl apply -f postgres-pvc.yaml
kubectl apply -f postgres-deployment.yaml
kubectl apply -f postgres-service.yaml

# 4. Deployer Redis
Log-Info "Deploiement de Redis..."
kubectl apply -f redis-deployment.yaml
kubectl apply -f redis-service.yaml

# 5. Deployer Kafka
Log-Info "Deploiement de Kafka..."
kubectl apply -f kafka-deployment.yaml
kubectl apply -f kafka-service.yaml

# 6. Attendre que les dependances soient pretes
Log-Info "Attente que PostgreSQL soit pret (max 120s)..."
kubectl wait --for=condition=ready pod -l app=postgres -n $NAMESPACE --timeout=120s

Log-Info "Attente que Redis soit pret (max 60s)..."
kubectl wait --for=condition=ready pod -l app=redis -n $NAMESPACE --timeout=60s

Log-Info "Attente que Kafka soit pret (max 120s)..."
kubectl wait --for=condition=ready pod -l app=kafka -n $NAMESPACE --timeout=120s

# 7. Deployer le User Service
Log-Info "Deploiement du User Service..."
kubectl apply -f user-service-deployment.yaml
kubectl apply -f user-service-service.yaml

# 8. Deployer l'Ingress
Log-Info "Deploiement de l'Ingress..."
kubectl apply -f ingress.yaml

# 9. Deployer l'HPA
Log-Info "Deploiement de l'HPA..."
kubectl apply -f hpa.yaml

# 10. Deployer les Network Policies
Log-Info "Application des Network Policies..."
kubectl apply -f network-policy.yaml

# 11. ServiceMonitor (optionnel - ignore les erreurs)
Log-Info "Tentative de deploiement du ServiceMonitor..."
kubectl apply -f servicemonitor.yaml 2>$null
if ($?) {
    Log-Info "ServiceMonitor deploye"
} else {
    Log-Warn "ServiceMonitor non deploye (Prometheus Operator non installe)"
}

# 12. Attendre que le User Service soit pret
Log-Info "Attente que le User Service soit pret (max 180s)..."
kubectl wait --for=condition=ready pod -l app=user-service -n $NAMESPACE --timeout=180s

# Afficher le statut
Write-Host ""
Log-Info "=================================================="
Log-Info "Deploiement termine !"
Log-Info "=================================================="
Write-Host ""

Log-Info "Statut des pods:"
kubectl get pods -n $NAMESPACE

Write-Host ""
Log-Info "Statut des services:"
kubectl get svc -n $NAMESPACE

Write-Host ""
Log-Info "Pour acceder au service localement:"
Write-Host "  kubectl port-forward svc/user-service 8083:8083 -n $NAMESPACE" -ForegroundColor Cyan
Write-Host ""
Log-Info "Pour voir les logs:"
Write-Host "  kubectl logs -f deployment/user-service -n $NAMESPACE" -ForegroundColor Cyan
Write-Host ""
Log-Info "Pour tester API:"
Write-Host "  curl http://localhost:8083/api/actuator/health" -ForegroundColor Cyan
Write-Host ""

Log-Info "Deploiement reussi !"
