# Kubernetes Manifests - User Service (G3)

Ce répertoire contient tous les manifests Kubernetes nécessaires pour déployer le service utilisateur SGITU.

## 📁 Structure des fichiers

```
k8s/
├── namespace.yaml              # Namespace sgitu
├── configmap.yaml              # Configuration de l'application
├── secrets.yaml                # Secrets (JWT, DB passwords)
├── postgres-pvc.yaml           # PersistentVolumeClaim pour PostgreSQL
├── postgres-deployment.yaml    # Déploiement PostgreSQL
├── postgres-service.yaml       # Service PostgreSQL
├── redis-deployment.yaml       # Déploiement Redis
├── redis-service.yaml          # Service Redis
├── kafka-deployment.yaml       # Déploiement Kafka
├── kafka-service.yaml          # Service Kafka
├── user-service-deployment.yaml # Déploiement User Service
├── user-service-service.yaml   # Service User Service
├── ingress.yaml                # Ingress (optionnel)
├── hpa.yaml                    # HorizontalPodAutoscaler
├── servicemonitor.yaml         # Prometheus ServiceMonitor
└── network-policy.yaml         # Network Policies (sécurité)
```

## 🚀 Déploiement rapide

### Prérequis

```bash
# Vérifier kubectl
kubectl version --client

# Vérifier le cluster
kubectl cluster-info
```

### Déploiement complet

```bash
# Déployer tous les manifests
kubectl apply -f k8s/

# Vérifier le déploiement
kubectl get all -n sgitu
```

### Déploiement étape par étape

```bash
# 1. Namespace
kubectl apply -f namespace.yaml

# 2. Configuration
kubectl apply -f configmap.yaml
kubectl apply -f secrets.yaml

# 3. PostgreSQL
kubectl apply -f postgres-pvc.yaml
kubectl apply -f postgres-deployment.yaml
kubectl apply -f postgres-service.yaml

# 4. Redis
kubectl apply -f redis-deployment.yaml
kubectl apply -f redis-service.yaml

# 5. Kafka
kubectl apply -f kafka-deployment.yaml
kubectl apply -f kafka-service.yaml

# 6. Attendre que les dépendances soient prêtes
kubectl wait --for=condition=ready pod -l app=postgres -n sgitu --timeout=120s
kubectl wait --for=condition=ready pod -l app=redis -n sgitu --timeout=60s
kubectl wait --for=condition=ready pod -l app=kafka -n sgitu --timeout=120s

# 7. User Service
kubectl apply -f user-service-deployment.yaml
kubectl apply -f user-service-service.yaml

# 8. (Optionnel) Ingress et HPA
kubectl apply -f ingress.yaml
kubectl apply -f hpa.yaml

# 9. (Optionnel) Monitoring
kubectl apply -f servicemonitor.yaml

# 10. (Production) Network Policies
kubectl apply -f network-policy.yaml
```

## ✅ Vérification

### Vérifier les pods

```bash
kubectl get pods -n sgitu
kubectl get pods -n sgitu -w  # Watch mode
```

### Vérifier les services

```bash
kubectl get svc -n sgitu
```

### Vérifier les logs

```bash
# Logs du User Service
kubectl logs -f deployment/user-service -n sgitu

# Logs de PostgreSQL
kubectl logs -f deployment/postgres -n sgitu

# Logs de tous les pods
kubectl logs -l app=user-service -n sgitu --tail=100
```

### Tester l'API

```bash
# Port-forward
kubectl port-forward svc/user-service 8083:8083 -n sgitu

# Tester le health check
curl http://localhost:8083/api/actuator/health

# Tester Swagger
open http://localhost:8083/api/swagger-ui.html
```

## 🔧 Commandes utiles

### Scaling

```bash
# Scaler manuellement
kubectl scale deployment user-service --replicas=5 -n sgitu

# Vérifier l'HPA
kubectl get hpa -n sgitu
kubectl describe hpa user-service-hpa -n sgitu
```

### Debugging

```bash
# Décrire un pod
kubectl describe pod <pod-name> -n sgitu

# Exécuter une commande dans un pod
kubectl exec -it <pod-name> -n sgitu -- /bin/sh

# Vérifier les événements
kubectl get events -n sgitu --sort-by='.lastTimestamp'
```

### Tester les connexions

```bash
# Tester PostgreSQL
kubectl run -it --rm psql-test --image=postgres:15-alpine --restart=Never -n sgitu -- \
  psql -h postgres-service -U postgres -d users_db

# Tester Redis
kubectl run -it --rm redis-test --image=redis:7-alpine --restart=Never -n sgitu -- \
  redis-cli -h redis-service ping

# Tester Kafka
kubectl exec -it deployment/kafka -n sgitu -- \
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

### Redémarrer

```bash
# Redémarrer le User Service
kubectl rollout restart deployment/user-service -n sgitu

# Vérifier le rollout
kubectl rollout status deployment/user-service -n sgitu

# Historique des rollouts
kubectl rollout history deployment/user-service -n sgitu
```

## 🗑️ Nettoyage

```bash
# Supprimer tout le namespace
kubectl delete namespace sgitu

# Ou supprimer individuellement
kubectl delete -f k8s/
```

## 📊 Monitoring

### Métriques Prometheus

```bash
# Port-forward Prometheus (si installé)
kubectl port-forward svc/prometheus-operated 9090:9090 -n monitoring

# Accéder à Prometheus
open http://localhost:9090
```

### Grafana

```bash
# Port-forward Grafana (si installé)
kubectl port-forward svc/grafana 3000:3000 -n monitoring

# Accéder à Grafana
open http://localhost:3000
```

## 🔐 Sécurité

### Secrets en production

⚠️ **Important** : Ne jamais commiter `secrets.yaml` avec des valeurs réelles !

Utiliser Sealed Secrets :

```bash
# Installer Sealed Secrets
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml

# Créer un sealed secret
kubectl create secret generic user-service-secrets \
  --from-literal=JWT_SECRET='your-production-secret' \
  --dry-run=client -o yaml | \
  kubeseal -o yaml > sealed-secrets.yaml

# Appliquer
kubectl apply -f sealed-secrets.yaml
```

### Network Policies

Les Network Policies sont définies dans `network-policy.yaml` et limitent :
- Ingress : Seul l'API Gateway peut accéder au User Service
- Egress : Le User Service peut uniquement accéder à PostgreSQL, Redis, Kafka et DNS

## 📝 Notes

- **Replicas** : Par défaut 2 replicas pour haute disponibilité
- **Resources** : Ajuster selon votre environnement
- **Storage** : PostgreSQL utilise 5Gi par défaut
- **HPA** : Scale entre 2 et 10 replicas basé sur CPU/Memory
- **Probes** : Liveness, Readiness et Startup configurées

## 🔗 Liens utiles

- [Documentation complète](../K8S_DEPLOYMENT.md)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)
