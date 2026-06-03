# cleanup.ps1 — Remove G9 Service Gestion des Incidents from Kubernetes
# Usage: .\cleanup.ps1

Write-Host "============================================" -ForegroundColor Red
Write-Host " Cleaning up G9 - Service Gestion Incidents" -ForegroundColor Red
Write-Host "============================================" -ForegroundColor Red

$k8sDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Remove in reverse order
Write-Host "`n[1/6] Removing ServiceMonitor, Ingress, and HPA..." -ForegroundColor Yellow
kubectl delete -f "$k8sDir\servicemonitor.yaml" 2>$null
kubectl delete -f "$k8sDir\ingress.yaml" 2>$null
kubectl delete -f "$k8sDir\hpa.yaml" 2>$null

Write-Host "`n[2/6] Removing G9 Service..." -ForegroundColor Yellow
kubectl delete -f "$k8sDir\g9-service.yaml" 2>$null
kubectl delete -f "$k8sDir\g9-deployment.yaml" 2>$null

Write-Host "`n[3/6] Removing Kafka..." -ForegroundColor Yellow
kubectl delete -f "$k8sDir\kafka-service.yaml" 2>$null
kubectl delete -f "$k8sDir\kafka-deployment.yaml" 2>$null

Write-Host "`n[4/6] Removing MySQL..." -ForegroundColor Yellow
kubectl delete -f "$k8sDir\mysql-service.yaml" 2>$null
kubectl delete -f "$k8sDir\mysql-deployment.yaml" 2>$null
kubectl delete -f "$k8sDir\mysql-pvc.yaml" 2>$null

Write-Host "`n[5/6] Removing ConfigMap and Secrets..." -ForegroundColor Yellow
kubectl delete -f "$k8sDir\secrets.yaml" 2>$null
kubectl delete -f "$k8sDir\configmap.yaml" 2>$null

Write-Host "`n[6/6] Removing Namespace..." -ForegroundColor Yellow
kubectl delete -f "$k8sDir\namespace.yaml" 2>$null

Write-Host "`n============================================" -ForegroundColor Green
Write-Host " Cleanup complete!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
