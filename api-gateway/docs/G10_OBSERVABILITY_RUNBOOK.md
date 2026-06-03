# G10 - Observabilite et Kubernetes

## Docker Compose global avec Prometheus/Grafana

Demarrer l'environnement global avec monitoring :

```powershell
docker compose --env-file .env -f docker-compose.yml --profile monitoring up -d --build api-gateway prometheus grafana redis
```

URLs :

```text
Gateway G10 : http://localhost:8080
Swagger G10 : http://localhost:8080/swagger-ui.html
Prometheus : http://localhost:9090/targets
Grafana : http://localhost:3000
```

Identifiants Grafana :

```text
user: admin
password: admin123
```

Dashboard provisionne :

```text
SGITU / SGITU - G10 API Gateway
```

Verifier les metriques G10 directement :

```powershell
curl http://localhost:8080/actuator/prometheus
```

Voir les logs :

```powershell
docker compose --env-file .env -f docker-compose.yml logs -f api-gateway
docker compose --env-file .env -f docker-compose.yml logs -f prometheus
docker compose --env-file .env -f docker-compose.yml logs -f grafana
```

Arreter :

```powershell
docker compose --env-file .env -f docker-compose.yml --profile monitoring down
```

## Kubernetes local G10 + Redis + Prometheus + Grafana

Construire l'image G10 dans le Docker local :

```powershell
docker build -t sgitu-g10-api-gateway:latest ./api-gateway
```

Appliquer les manifests :

```powershell
kubectl apply -f api-gateway/k8s/api-gateway-deployment.yaml
kubectl apply -f api-gateway/k8s/g10-observability.yaml
```

Verifier les pods :

```powershell
kubectl get pods -n sgitu
kubectl get svc -n sgitu
```

Acceder aux services par port-forward :

```powershell
kubectl port-forward -n sgitu svc/g10-api-gateway 8080:8080
kubectl port-forward -n sgitu svc/prometheus 9090:9090
kubectl port-forward -n sgitu svc/grafana 3000:3000
```

Nettoyer :

```powershell
kubectl delete -f api-gateway/k8s/g10-observability.yaml
kubectl delete -f api-gateway/k8s/api-gateway-deployment.yaml
```
