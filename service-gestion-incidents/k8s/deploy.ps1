# deploy.ps1 — Deploy G9 Service Gestion des Incidents to Kubernetes
# Usage: .\deploy.ps1

Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Deploying G9 - Service Gestion Incidents" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

$k8sDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# 1. Create namespace
Write-Host "`n[1/8] Creating namespace..." -ForegroundColor Yellow
kubectl apply -f "$k8sDir\namespace.yaml"

# 2. Apply ConfigMap and Secrets
Write-Host "`n[2/8] Applying ConfigMap..." -ForegroundColor Yellow
kubectl apply -f "$k8sDir\configmap.yaml"

Write-Host "`n[3/8] Applying Secrets..." -ForegroundColor Yellow
kubectl apply -f "$k8sDir\secrets.yaml"

# 3. Deploy MySQL
Write-Host "`n[4/8] Deploying MySQL..." -ForegroundColor Yellow
kubectl apply -f "$k8sDir\mysql-pvc.yaml"
kubectl apply -f "$k8sDir\mysql-deployment.yaml"
kubectl apply -f "$k8sDir\mysql-service.yaml"

# 4. Deploy Kafka
Write-Host "`n[5/8] Deploying Kafka..." -ForegroundColor Yellow
kubectl apply -f "$k8sDir\kafka-deployment.yaml"
kubectl apply -f "$k8sDir\kafka-service.yaml"

# 5. Wait for MySQL and Kafka to be ready
Write-Host "`n[6/8] Waiting for MySQL and Kafka to be ready..." -ForegroundColor Yellow
kubectl rollout status deployment/mysql-deployment -n service-gestion-incidents --timeout=120s
kubectl rollout status deployment/kafka-deployment -n service-gestion-incidents --timeout=120s

# 6. Deploy the application
Write-Host "`n[7/8] Deploying G9 Service..." -ForegroundColor Yellow
kubectl apply -f "$k8sDir\g9-deployment.yaml"
kubectl apply -f "$k8sDir\g9-service.yaml"

# 7. Apply HPA, Ingress, and ServiceMonitor
Write-Host "`n[8/8] Applying HPA, Ingress and ServiceMonitor..." -ForegroundColor Yellow
kubectl apply -f "$k8sDir\hpa.yaml"
kubectl apply -f "$k8sDir\ingress.yaml"
kubectl apply -f "$k8sDir\servicemonitor.yaml" 2>$null

Write-Host "`n============================================" -ForegroundColor Green
Write-Host " Deployment complete!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host "`nTo check the status of all pods:"
Write-Host "  kubectl get pods -n service-gestion-incidents" -ForegroundColor Cyan
Write-Host "`nTo view logs:"
Write-Host "  kubectl logs -l app=g9-service -n service-gestion-incidents" -ForegroundColor Cyan
Write-Host "`nTo access the API via port-forward:"
Write-Host "  kubectl port-forward svc/g9-service 8089:8089 -n service-gestion-incidents" -ForegroundColor Cyan
