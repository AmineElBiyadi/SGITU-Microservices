# G10 - Kubernetes local

Ce dossier deploie uniquement les composants du groupe 10 :

- API Gateway G10
- Redis pour blacklist JWT
- Prometheus pour scraper `/actuator/prometheus`
- Grafana avec datasource et dashboard G10 preconfigures

Les microservices metier G1 a G9 restent externes a ce dossier. En Kubernetes, la Gateway attend les noms internes suivants si les autres groupes sont deployes dans le meme namespace :

```text
user-service:8083
service-billetterie:8081
abonnement-service:8082
g4-coordination:8084
notification-service:8085
payment-service:8086
g7-suivi-vehicules:8087
g8-analytics:8088
ml-service:5000
service-gestion-incidents:8089
```

## Build image G10

Depuis le dossier `api-gateway` :

```powershell
mvn clean package -DskipTests
docker build -t sgitu-g10-api-gateway:latest .
```

Avec Minikube :

```powershell
minikube image load sgitu-g10-api-gateway:latest
```

## Deploiement

Depuis le dossier `api-gateway` :

```powershell
kubectl apply -k k8s
kubectl get pods -n sgitu
kubectl get svc -n sgitu
```

## Acces local

Docker Desktop Kubernetes :

```text
Gateway    : http://localhost:30080
Prometheus : http://localhost:30090
Grafana    : http://localhost:30300
```

Minikube :

```powershell
minikube service g10-api-gateway -n sgitu
minikube service prometheus -n sgitu
minikube service grafana -n sgitu
```

Grafana :

```text
login    : admin
password : admin123
```

## Verification

```powershell
kubectl -n sgitu logs deploy/g10-api-gateway
kubectl -n sgitu port-forward svc/g10-api-gateway 8080:8080
```

Puis tester :

```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/prometheus
```

## Nettoyage

```powershell
kubectl delete -k k8s
```

