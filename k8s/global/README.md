# SGITU - Kubernetes global

Ce dossier contient une simulation Kubernetes globale pour SGITU :

- G1 a G9 restent internes au cluster avec des `Service` de type `ClusterIP`.
- G10 API Gateway est le seul point d'entree applicatif expose.
- Prometheus et Grafana sont exposes pour l'observabilite.
- Les secrets sont centralises dans `sgitu-secrets`.
- Les URLs internes reprennent les noms Docker/Kubernetes utilises par G10.

## Images locales attendues

Avant `kubectl apply`, les images applicatives doivent exister dans le cluster local.

Exemple avec Docker Desktop Kubernetes :

```powershell
docker build -t service-billetterie:latest ./service-billetterie
docker build -t g3-user-service:1.0 ./service-utilisateur
docker build -t g4-coordination:latest ./service-coordination-transport
docker build -t notification-service:latest ./service-notification
docker build -t sgitu/payment-service:1.0.0 ./service-paiement
docker build -t service-suivi-vehicule:1.0.0 ./service-suivi-vehicule
docker build -t g8-analytics-service:latest ./service-analytique
docker build -t g8-ml-service:latest ./service-analytique/ml-service
docker build -t service-gestion-incidents:latest ./service-gestion-incidents
docker build -t sgitu-g10-api-gateway:latest ./api-gateway
```

Avec Minikube, charger les images apres build :

```powershell
minikube image load service-billetterie:latest
minikube image load g3-user-service:1.0
minikube image load g4-coordination:latest
minikube image load notification-service:latest
minikube image load sgitu/payment-service:1.0.0
minikube image load service-suivi-vehicule:1.0.0
minikube image load g8-analytics-service:latest
minikube image load g8-ml-service:latest
minikube image load service-gestion-incidents:latest
minikube image load sgitu-g10-api-gateway:latest
```

## Deploiement

```powershell
kubectl apply -k k8s/global
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
minikube service api-gateway -n sgitu
minikube service prometheus -n sgitu
minikube service grafana -n sgitu
```

Grafana :

```text
login    : admin
password : admin123
```

## Tests rapides

```powershell
kubectl -n sgitu get pods
kubectl -n sgitu logs deploy/api-gateway
kubectl -n sgitu port-forward svc/api-gateway 8080:8080
```

Puis tester via Postman avec :

```text
baseUrl = http://localhost:8080
```

## Nettoyage

```powershell
kubectl delete -k k8s/global
```

## Remarque

Cette stack est prevue pour une demonstration locale Kubernetes. Pour une production reelle, remplacer les `emptyDir` par des `PersistentVolumeClaim`, externaliser les secrets, et publier les images applicatives dans un registry.

