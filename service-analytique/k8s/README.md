# Kubernetes Deployment - Service Analytique

## 📋 Vue d'ensemble

Configuration Kubernetes complète pour le microservice `service-analytique` avec :
- **2 répliques** pour la haute disponibilité
- **Health checks** avec Spring Boot Actuator
- **Monitoring** Prometheus intégré
- **Ressources limitées** pour l'optimisation
- **NodePort** pour l'accès local sur Mac

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                    │
│                                                     │
│  ┌─────────────┐    ┌─────────────┐                │
│  │   Pod 1     │    │   Pod 2     │                │
│  │ (Replica)    │    │ (Replica)    │                │
│  │ Port: 8088  │    │ Port: 8088  │                │
│  └─────────────┘    └─────────────┘                │
│         │                   │                      │
│         └─────────┬─────────┘                      │
│                   │                                │
│          ┌─────────────┐                          │
│          │  Service   │                          │
│          │ NodePort   │                          │
│          │ 8088:30001 │                          │
│          └─────────────┘                          │
│                   │                                │
│         ┌─────────────┐                           │
│         │   ConfigMap │                           │
│         └─────────────┘                           │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Déploiement rapide

### 1. Prérequis

```bash
# Vérifier que Kubernetes est actif
kubectl cluster-info

# Vérifier les nœuds disponibles
kubectl get nodes

# S'assurer que l'image Docker est disponible
docker images | grep microservice-analyse-multi
```

### 2. Déploiement

```bash
# Créer le namespace (optionnel)
kubectl create namespace service-analytique

# Appliquer la configuration
kubectl apply -f configmap.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml

# Ou tout déployer d'un coup
kubectl apply -f .
```

### 3. Vérification

```bash
# Vérifier les pods
kubectl get pods -l app=service-analytique

# Vérifier les services
kubectl get services -l app=service-analytique

# Vérifier le déploiement
kubectl get deployment service-analytique

# Logs des pods
kubectl logs -l app=service-analytique -f
```

### 4. Accès au service

```bash
# Port-forward pour développement
kubectl port-forward service/service-analytique 8088:8088

# Accès via NodePort (Mac)
curl http://localhost:30001/actuator/health

# Accès Swagger UI
open http://localhost:30001/swagger-ui.html
```

## 📊 Ressources configurées

### Limites par pod
- **CPU**: 500m max, 250m request
- **Mémoire**: 512Mi max, 256Mi request
- **Architecture**: ARM64 optimisée

### Health Checks
- **Liveness**: `/actuator/health/liveness` (60s delay, 30s interval)
- **Readiness**: `/actuator/health/readiness` (30s delay, 10s interval)
- **Startup**: `/actuator/health` (10s delay, 10s interval)

## 🔧 Configuration

### ConfigMap Variables

| Variable | Description | Valeur par défaut |
|----------|-------------|------------------|
| `MONGO_URI` | Chaîne de connexion MongoDB | `mongodb://mongodb-service:27017/g8_analytics` |
| `LOG_LEVEL` | Niveau de logging | `INFO` |
| `SPRING_PROFILES_ACTIVE` | Profile Spring | `kubernetes` |
| `JAVA_OPTS` | Options JVM | `-Xmx512m -Xms256m` |
| `ANALYSIS_TIMEOUT_MINUTES` | Timeout des analyses | `30` |
| `MAX_CONCURRENT_ANALYSES` | Analyses simultanées max | `10` |

## 📈 Monitoring

### Health Checks Endpoints

```bash
# Health général
curl http://localhost:30001/actuator/health

# Liveness probe
curl http://localhost:30001/actuator/health/liveness

# Readiness probe
curl http://localhost:30001/actuator/health/readiness

# Database health
curl http://localhost:30001/actuator/health/db

# Métriques Prometheus
curl http://localhost:30001/actuator/prometheus
```

### Métriques disponibles

- `jvm_memory_used_bytes`
- `jvm_threads_live_threads`
- `http_server_requests_seconds_count`
- `process_cpu_usage`
- `mongodb_connections_active`

## 🛠️ Gestion du déploiement

### Mise à jour (Rolling Update)

```bash
# Mettre à jour l'image
kubectl set image deployment/service-analytique \
  service-analytique=microservice-analyse-multi:v2

# Suivre le rollout
kubectl rollout status deployment/service-analytique

# Historique des rollouts
kubectl rollout history deployment/service-analytique

# Revenir à la version précédente
kubectl rollout undo deployment/service-analytique
```

### Scale horizontal

```bash
# Augmenter les répliques
kubectl scale deployment service-analytique --replicas=5

# Vérifier le scale
kubectl get pods -l app=service-analytique
```

### Debug

```bash
# Entrer dans un pod
kubectl exec -it <pod-name> -- /bin/sh

# Vérifier les ressources utilisées
kubectl top pods -l app=service-analytique

# Events du namespace
kubectl get events --field-selector involvedObject.name=service-analytique
```

## 🔒 Sécurité

### Network Policies (optionnel)

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: service-analytique-netpol
spec:
  podSelector:
    matchLabels:
      app: service-analytique
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8088
```

## 🚨 Dépannage

### Problèmes courants

1. **Pod en CrashLoopBackOff**
   ```bash
   kubectl describe pod <pod-name>
   kubectl logs <pod-name> --previous
   ```

2. **Service inaccessible**
   ```bash
   kubectl get endpoints service-analytique
   kubectl describe service service-analytique
   ```

3. **Health checks failing**
   ```bash
   kubectl describe pod <pod-name> | grep -A 10 Liveness
   kubectl describe pod <pod-name> | grep -A 10 Readiness
   ```

4. **Problèmes de ressources**
   ```bash
   kubectl top nodes
   kubectl describe node <node-name>
   ```

## 📝 Nettoyage

```bash
# Supprimer tous les ressources
kubectl delete -f .

# Supprimer par labels
kubectl delete all -l app=service-analytique
kubectl delete configmap -l app=service-analytique

# Supprimer le namespace
kubectl delete namespace service-analytique
```

## 📚 Références

- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Kubernetes Probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)
- [Resource Management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/)
- [Rolling Updates](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#rolling-update-deployment)
