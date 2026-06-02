#!/bin/bash

# Script de nettoyage Bash pour Windows/Linux/Mac
# Usage: bash cleanup.sh [-f] ou ./cleanup.sh [-f]

NAMESPACE="sgitu"
FORCE=false

# Parser les arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--force)
            FORCE=true
            shift
            ;;
        *)
            shift
            ;;
    esac
done

echo "🗑️  Nettoyage du User Service sur Kubernetes"
echo "============================================="

# Fonction de logging
log_info() {
    echo "[INFO] $1"
}

log_warn() {
    echo "[WARN] $1"
}

# Confirmation si pas de -f
if [ "$FORCE" != true ]; then
    read -p "Cette action va supprimer tous les composants du namespace $NAMESPACE. Êtes-vous sûr ? (yes/no) " confirmation
    if [ "$confirmation" != "yes" ]; then
        log_info "Annulation du nettoyage"
        exit 0
    fi
fi

log_info "Suppression des ressources Kubernetes..."

# Supprimer dans l'ordre inverse
log_info "Suppression des Network Policies..."
kubectl delete -f network-policy.yaml --ignore-not-found=true 2>/dev/null

log_info "Suppression du ServiceMonitor..."
kubectl delete -f servicemonitor.yaml --ignore-not-found=true 2>/dev/null

log_info "Suppression de l'HPA..."
kubectl delete -f hpa.yaml --ignore-not-found=true 2>/dev/null

log_info "Suppression de l'Ingress..."
kubectl delete -f ingress.yaml --ignore-not-found=true 2>/dev/null

log_info "Suppression du User Service..."
kubectl delete -f user-service-service.yaml --ignore-not-found=true 2>/dev/null
kubectl delete -f user-service-deployment.yaml --ignore-not-found=true 2>/dev/null

log_info "Suppression de Kafka..."
kubectl delete -f kafka-service.yaml --ignore-not-found=true 2>/dev/null
kubectl delete -f kafka-deployment.yaml --ignore-not-found=true 2>/dev/null

log_info "Suppression de Redis..."
kubectl delete -f redis-service.yaml --ignore-not-found=true 2>/dev/null
kubectl delete -f redis-deployment.yaml --ignore-not-found=true 2>/dev/null

log_info "Suppression de PostgreSQL..."
kubectl delete -f postgres-service.yaml --ignore-not-found=true 2>/dev/null
kubectl delete -f postgres-deployment.yaml --ignore-not-found=true 2>/dev/null
kubectl delete -f postgres-pvc.yaml --ignore-not-found=true 2>/dev/null

log_info "Suppression des ConfigMaps et Secrets..."
kubectl delete -f secrets.yaml --ignore-not-found=true 2>/dev/null
kubectl delete -f configmap.yaml --ignore-not-found=true 2>/dev/null

# Supprimer le namespace si -f
if [ "$FORCE" = true ]; then
    log_warn "Suppression du namespace $NAMESPACE..."
    kubectl delete namespace $NAMESPACE --ignore-not-found=true 2>/dev/null
else
    log_info "Namespace $NAMESPACE conservé (utilisez -f pour le supprimer)"
fi

log_info "✓ Nettoyage terminé"

# Vérifier qu'il ne reste rien
remaining=$(kubectl get all -n $NAMESPACE 2>/dev/null)
if [ -n "$remaining" ]; then
    log_warn "Il reste des ressources dans le namespace:"
    kubectl get all -n $NAMESPACE
else
    log_info "Toutes les ressources ont été supprimées"
fi
