#!/bin/bash

# Script de déploiement Bash pour SGITU User Service sur Kubernetes
# Usage: bash deploy.sh [--skip-wait] [--with-monitoring]

NAMESPACE="sgitu"
SKIP_WAIT=false
WITH_MONITORING=false

# Résoudre le répertoire du script pour permettre l'exécution depuis un cwd différent
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Parser les arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-wait)
            SKIP_WAIT=true
            shift
            ;;
        --with-monitoring)
            WITH_MONITORING=true
            shift
            ;;
        *)
            shift
            ;;
    esac
done

echo "🚀 Déploiement du User Service SGITU sur Kubernetes"
echo "===================================================="

# Fonction de logging
log_info() {
    echo "[INFO] $1"
}

log_success() {
    echo "[✓] $1"
}

log_warn() {
    echo "[WARN] $1"
}

log_error() {
    echo "[ERROR] $1"
}

# Vérifier kubectl
if ! command -v kubectl &> /dev/null; then
    log_error "kubectl n'est pas installé ou non accessible"
    exit 1
fi

if ! command -v kubectl &> /dev/null; then
    log_error "kubectl n'est pas installé ou non accessible"
    exit 1
fi

# kubectl --short n'est pas disponible partout, essayer --short puis fallback
KV=""
if kubectl version --client --short >/dev/null 2>&1; then
    KV=$(kubectl version --client --short 2>/dev/null)
else
    KV=$(kubectl version --client 2>/dev/null | head -n 1)
fi
log_success "kubectl trouvé : $KV"

# Étape 1 : Créer le namespace
log_info "Étape 1/8 - Création du namespace..."
kubectl apply -f "$SCRIPT_DIR/namespace.yaml"
if [ $? -eq 0 ]; then
    log_success "Namespace $NAMESPACE créé"
else
    log_error "Impossible de créer le namespace"
    exit 1
fi

# Étape 2 : Appliquer ConfigMaps et Secrets
log_info "Étape 2/8 - Application des ConfigMaps et Secrets..."
kubectl apply -f "$SCRIPT_DIR/configmap.yaml"
kubectl apply -f "$SCRIPT_DIR/secrets.yaml"
if [ $? -eq 0 ]; then
    log_success "ConfigMaps et Secrets appliqués"
else
    log_error "Impossible d'appliquer les ConfigMaps/Secrets"
    exit 1
fi

# Étape 3 : Déployer PostgreSQL
log_info "Étape 3/8 - Déploiement de PostgreSQL..."
kubectl apply -f "$SCRIPT_DIR/postgres-pvc.yaml"
kubectl apply -f "$SCRIPT_DIR/postgres-deployment.yaml"
kubectl apply -f "$SCRIPT_DIR/postgres-service.yaml"
log_success "PostgreSQL déployé"

if [ "$SKIP_WAIT" != true ]; then
    log_info "Attente que PostgreSQL soit prêt (timeout 120s)..."
    if kubectl wait --for=condition=ready pod -l app=postgres -n $NAMESPACE --timeout=120s 2>/dev/null; then
        log_success "PostgreSQL est prêt"
    else
        log_warn "PostgreSQL n'est pas prêt après 120s (continuant...)"
    fi
fi

# Étape 4 : Déployer Redis
log_info "Étape 4/8 - Déploiement de Redis..."
kubectl apply -f "$SCRIPT_DIR/redis-deployment.yaml"
kubectl apply -f "$SCRIPT_DIR/redis-service.yaml"
log_success "Redis déployé"

if [ "$SKIP_WAIT" != true ]; then
    log_info "Attente que Redis soit prêt (timeout 60s)..."
    if kubectl wait --for=condition=ready pod -l app=redis -n $NAMESPACE --timeout=60s 2>/dev/null; then
        log_success "Redis est prêt"
    else
        log_warn "Redis n'est pas prêt après 60s (continuant...)"
    fi
fi

# Étape 5 : Déployer Kafka
log_info "Étape 5/8 - Déploiement de Kafka..."
kubectl apply -f "$SCRIPT_DIR/kafka-deployment.yaml"
kubectl apply -f "$SCRIPT_DIR/kafka-service.yaml"
log_success "Kafka déployé"

if [ "$SKIP_WAIT" != true ]; then
    log_info "Attente que Kafka soit prêt (timeout 120s)..."
    if kubectl wait --for=condition=ready pod -l app=kafka -n $NAMESPACE --timeout=120s 2>/dev/null; then
        log_success "Kafka est prêt"
    else
        log_warn "Kafka n'est pas prêt après 120s (continuant...)"
    fi
fi

# Étape 6 : Déployer le User Service
log_info "Étape 6/8 - Déploiement du User Service..."
kubectl apply -f "$SCRIPT_DIR/user-service-deployment.yaml"
kubectl apply -f "$SCRIPT_DIR/user-service-service.yaml"
log_success "User Service déployé"

if [ "$SKIP_WAIT" != true ]; then
    log_info "Attente que le User Service soit prêt (timeout 180s)..."
    if kubectl wait --for=condition=ready pod -l app=user-service -n $NAMESPACE --timeout=180s 2>/dev/null; then
        log_success "User Service est prêt"
    else
        log_warn "User Service n'est pas prêt après 180s (continuant...)"
    fi
fi

# Étape 7 : Déployer Ingress et HPA
log_info "Étape 7/8 - Déploiement de l'Ingress et HPA..."
kubectl apply -f "$SCRIPT_DIR/ingress.yaml"
kubectl apply -f "$SCRIPT_DIR/hpa.yaml"
log_success "Ingress et HPA déployés"

# Étape 8 : Network Policies
log_info "Étape 8/8 - Déploiement des Network Policies..."
kubectl apply -f "$SCRIPT_DIR/network-policy.yaml"
log_success "Network Policies déployées"

# Optionnel : ServiceMonitor
if [ "$WITH_MONITORING" = true ]; then
    log_info "Déploiement du ServiceMonitor (monitoring)..."
    if kubectl apply -f servicemonitor.yaml 2>/dev/null; then
        log_success "ServiceMonitor déployé"
    else
        log_warn "ServiceMonitor non déployé (Prometheus Operator probablement non installé)"
    fi
fi

echo ""
echo "✅ Déploiement terminé!"
echo "===================================================="
echo ""
echo "📊 Vérification des ressources :"
echo ""

log_info "Pods en cours d'exécution :"
kubectl get pods -n $NAMESPACE --no-headers | awk '{print "  " $0}'

echo ""
log_info "Services :"
kubectl get svc -n $NAMESPACE --no-headers | awk '{print "  " $0}'

echo ""
log_info "Deployments :"
kubectl get deployment -n $NAMESPACE --no-headers | awk '{print "  " $0}'

echo ""
echo "🔗 Prochaines étapes :"
echo "  1. Port-forward vers le User Service :"
echo "     kubectl port-forward svc/user-service 8083:8083 -n $NAMESPACE"
echo ""
echo "  2. Tester l'API :"
echo "     curl http://localhost:8083/api/actuator/health"
echo ""
echo "  3. Accéder à Swagger UI :"
echo "     http://localhost:8083/api/swagger-ui.html"
echo ""
echo "  4. Voir les logs :"
echo "     kubectl logs -f deployment/user-service -n $NAMESPACE"
echo ""
