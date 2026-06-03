# Guide de Déploiement Manuel - Windows

## Étapes de déploiement

Exécutez ces commandes **dans l'ordre** depuis PowerShell dans le répertoire `k8s/` :

### 1. Créer le namespace

```powershell
kubectl apply -f namespace.yaml
```

### 2. Appliquer la configuration

```powershell
kubectl apply -f configmap.yaml
kubectl apply -f secrets.yaml
```

### 3. Déployer PostgreSQL

```powershell
kubectl apply -f postgres-pvc.yaml
kubectl apply -f postgres-deployment.yaml
kubectl apply -f postgres-service.yaml
```

Attendre que PostgreSQL soit prêt :

```powershell
kubectl wait --for=condition=ready pod -l app=postgres -n sgitu --timeout=120s
```

### 4. Déployer Redis

```powershell
kubectl apply -f redis-deployment.yaml
kubectl apply -f redis-service.yaml
```

Attendre que Redis soit prêt :

```powershell
kubectl wait --for=condition=ready pod -l app=redis -n sgitu --timeout=60s
```

### 5. Déployer Kafka

```powershell
kubectl apply -f kafka-deployment.yaml
kubectl apply -f kafka-service.yaml
```

Attendre que Kafka soit prêt :

```powershell
kubectl wait --for=condition=ready pod -l app=kafka -n sgitu --timeout=120s
```

### 6. Déployer le User Service

```powershell
kubectl apply -f user-service-deployment.yaml
kubectl apply -f user-service-service.yaml
```

Attendre que le User Service soit prêt :

```powershell
kubectl wait --for=condition=ready pod -l app=user-service -n sgitu --timeout=180s
```

### 7. Déployer l'Ingress et l'HPA

```powershell
kubectl apply -f ingress.yaml
kubectl apply -f hpa.yaml
```

### 8. (Optionnel) Network Policies

```powershell
kubectl apply -f network-policy.yaml
```

### 9. (Optionnel) ServiceMonitor

Si Prometheus Operator est installé :

```powershell
kubectl apply -f servicemonitor.yaml
```

Sinon, ignorer cette étape.

---

## Vérification

### Voir tous les pods

```powershell
kubectl get pods -n sgitu
```

Résultat attendu :
```
NAME                            READY   STATUS    RESTARTS   AGE
kafka-xxx                       1/1     Running   0          5m
postgres-xxx                    1/1     Running   0          6m
redis-xxx                       1/1     Running   0          5m
user-service-xxx                1/1     Running   0          3m
user-service-yyy                1/1     Running   0          3m
```

### Voir tous les services

```powershell
kubectl get svc -n sgitu
```

### Voir les logs du User Service

```powershell
kubectl logs -f deployment/user-service -n sgitu
```

---

## Tester l'API

### Port-forward

```powershell
kubectl port-forward svc/user-service 8083:8083 -n sgitu
```

### Tester le health check

Dans un autre terminal :

```powershell
curl http://localhost:8083/api/actuator/health
```

Ou dans un navigateur :
```
http://localhost:8083/api/swagger-ui.html
```

---

## Troubleshooting

### Pod ne démarre pas

```powershell
# Voir les événements
kubectl get events -n sgitu --sort-by='.lastTimestamp'

# Décrire le pod
kubectl describe pod <pod-name> -n sgitu

# Voir les logs
kubectl logs <pod-name> -n sgitu
```

### Redémarrer un deployment

```powershell
kubectl rollout restart deployment/user-service -n sgitu
```

### Vérifier l'HPA

```powershell
kubectl get hpa -n sgitu
```

---

## Nettoyage

### Supprimer toutes les ressources

```powershell
kubectl delete -f network-policy.yaml
kubectl delete -f hpa.yaml
kubectl delete -f ingress.yaml
kubectl delete -f user-service-service.yaml
kubectl delete -f user-service-deployment.yaml
kubectl delete -f kafka-service.yaml
kubectl delete -f kafka-deployment.yaml
kubectl delete -f redis-service.yaml
kubectl delete -f redis-deployment.yaml
kubectl delete -f postgres-service.yaml
kubectl delete -f postgres-deployment.yaml
kubectl delete -f postgres-pvc.yaml
kubectl delete -f secrets.yaml
kubectl delete -f configmap.yaml
```

### Supprimer le namespace

```powershell
kubectl delete namespace sgitu
```

---

## Commandes utiles

### Scaler manuellement

```powershell
kubectl scale deployment user-service --replicas=3 -n sgitu
```

### Voir les métriques

```powershell
kubectl top pods -n sgitu
kubectl top nodes
```

### Exécuter une commande dans un pod

```powershell
kubectl exec -it deployment/user-service -n sgitu -- /bin/sh
```

### Tester PostgreSQL

```powershell
kubectl run -it --rm psql-test --image=postgres:15-alpine --restart=Never -n sgitu -- psql -h postgres-service -U postgres -d users_db
```

### Tester Redis

```powershell
kubectl run -it --rm redis-test --image=redis:7-alpine --restart=Never -n sgitu -- redis-cli -h redis-service ping
```

### Tester Kafka

```powershell
kubectl exec -it deployment/kafka -n sgitu -- /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```
