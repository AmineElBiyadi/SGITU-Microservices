#!/bin/bash

# Deploy User Service to Kubernetes
# Compatible with Git Bash on Windows

echo "=========================================="
echo "🚀 Déploiement du Service Utilisateur"
echo "=========================================="
echo ""

# Navigate to k8s directory
cd "$(dirname "$0")"

# Create namespace
echo "📦 Création du namespace..."
kubectl create namespace sgitu --dry-run=client -o yaml | kubectl apply -f -
echo ""

# Apply configurations in order
echo "⚙️  Application des ConfigMaps et Secrets..."
kubectl apply -f configmap.yaml
kubectl apply -f secrets.yaml
echo ""

echo "🗄️  Déploiement de PostgreSQL..."
kubectl apply -f postgres-pvc.yaml
kubectl apply -f postgres-deployment.yaml
kubectl apply -f postgres-service.yaml
echo ""

echo "💾 Déploiement de Redis..."
kubectl apply -f redis-deployment.yaml
kubectl apply -f redis-service.yaml
echo ""

echo "📨 Déploiement de Kafka..."
kubectl apply -f kafka-deployment.yaml
kubectl apply -f kafka-service.yaml
echo ""

echo "⏳ Attente du démarrage des dépendances (30s)..."
sleep 30
echo ""

echo "👤 Déploiement du Service Utilisateur..."
kubectl apply -f user-service-deployment.yaml
kubectl apply -f user-service-service.yaml
echo ""

echo "🌐 Application des NetworkPolicies..."
kubectl apply -f networkpolicy.yaml 2>/dev/null || echo "⚠️  NetworkPolicies non supportées (ignoré)"
echo ""

echo "📊 Application du HPA (Horizontal Pod Autoscaler)..."
kubectl apply -f hpa.yaml 2>/dev/null || echo "⚠️  Metrics Server non installé (ignoré)"
echo ""

echo "🔍 Application du ServiceMonitor (Prometheus)..."
kubectl apply -f servicemonitor.yaml 2>/dev/null || echo "⚠️  Prometheus Operator non installé (ignoré)"
echo ""

echo "🌍 Application de l'Ingress..."
kubectl apply -f ingress.yaml 2>/dev/null || echo "⚠️  Ingress Controller non installé (ignoré)"
echo ""

echo "=========================================="
echo "✅ Déploiement terminé !"
echo "=========================================="
echo ""

echo "📊 Statut des pods :"
kubectl get pods -n sgitu
echo ""

echo "📋 Statut des services :"
kubectl get svc -n sgitu
echo ""

echo "⏳ Attente que tous les pods soient prêts..."
kubectl wait --for=condition=ready pod -l app=user-service -n sgitu --timeout=300s 2>/dev/null || echo "⚠️  Timeout - vérifiez manuellement avec: kubectl get pods -n sgitu"
echo ""

echo "=========================================="
echo "🎉 Déploiement réussi !"
echo "=========================================="
echo ""
echo "Pour accéder au service :"
echo "  kubectl port-forward svc/user-service 8083:8083 -n sgitu"
echo ""
echo "Pour voir les logs :"
echo "  kubectl logs -f deployment/user-service -n sgitu"
echo ""
echo "Pour voir tous les pods :"
echo "  kubectl get pods -n sgitu"
echo ""
