# Guide de Déploiement Kubernetes — G9 Service Gestion des Incidents

## Prérequis

Avant de déployer, assurez-vous d'avoir :

1. **Docker Desktop** installé avec **Kubernetes activé** (Settings → Kubernetes → Enable Kubernetes).
2. **kubectl** accessible dans votre terminal (`kubectl version` pour vérifier).
3. **L'image Docker** construite localement :
   ```powershell
   cd C:\Users\ASSIL\OneDrive\Bureau\SGITU-Gestion-Incidents\service-gestion-incidents
   docker build -t sgitu/service-gestion-incidents:1.0 .
   ```
4. **(Optionnel)** Un Ingress Controller NGINX installé :
   ```powershell
   kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml
   ```

---

## Déploiement

### Option 1 : Script automatique (recommandé)

```powershell
cd service-gestion-incidents\k8s
.\deploy.ps1
```

### Option 2 : Déploiement manuel étape par étape

```powershell
cd service-gestion-incidents\k8s

# 1. Créer le namespace
kubectl apply -f namespace.yaml

# 2. Appliquer la configuration
kubectl apply -f configmap.yaml
kubectl apply -f secrets.yaml

# 3. Déployer MySQL
kubectl apply -f mysql-pvc.yaml
kubectl apply -f mysql-deployment.yaml
kubectl apply -f mysql-service.yaml

# 4. Déployer Kafka
kubectl apply -f kafka-deployment.yaml
kubectl apply -f kafka-service.yaml

# 5. Attendre que MySQL et Kafka soient prêts
kubectl rollout status deployment/mysql-deployment -n service-gestion-incidents --timeout=120s
kubectl rollout status deployment/kafka-deployment -n service-gestion-incidents --timeout=120s

# 6. Déployer le service
kubectl apply -f g9-deployment.yaml
kubectl apply -f g9-service.yaml

# 7. Appliquer l'autoscaling et l'ingress
kubectl apply -f hpa.yaml
kubectl apply -f ingress.yaml
```

---

## Vérification

### 1. Vérifier que tous les pods sont en cours d'exécution

```powershell
kubectl get pods -n service-gestion-incidents
```

Vous devriez voir 3 pods avec le statut `Running` :
- `mysql-deployment-xxxxx`
- `kafka-deployment-xxxxx`
- `g9-service-xxxxx`

### 2. Vérifier les logs de l'application

```powershell
kubectl logs -l app=g9-service -n service-gestion-incidents --tail=50
```

Vous devriez voir les logs Spring Boot habituels indiquant que le service a démarré avec succès.

### 3. Accéder à l'API via port-forward

```powershell
kubectl port-forward svc/g9-service 8089:8089 -n service-gestion-incidents
```

Ensuite, ouvrez votre navigateur ou Postman et testez :
- Swagger UI : `http://localhost:8089/api/incidents/swagger-ui.html`
- Actuator : `http://localhost:8089/api/incidents/actuator/health`

### 4. (Optionnel) Accéder via l'Ingress

Ajoutez cette ligne dans votre fichier `C:\Windows\System32\drivers\etc\hosts` :
```
127.0.0.1 sgitu.local
```

Puis accédez à : `http://sgitu.local/api/incidents/swagger-ui.html`

### 5. Vérifier l'autoscaling

```powershell
kubectl get hpa -n service-gestion-incidents
```

---

## Nettoyage

Pour supprimer tout le déploiement :

### Option 1 : Script automatique

```powershell
cd service-gestion-incidents\k8s
.\cleanup.ps1
```

### Option 2 : Suppression manuelle

```powershell
kubectl delete namespace service-gestion-incidents
```

> **Note :** Supprimer le namespace supprime automatiquement toutes les ressources qu'il contient.

---

## Structure des fichiers

```
k8s/
├── namespace.yaml           # Namespace isolé
├── configmap.yaml           # Variables d'environnement
├── secrets.yaml             # Secrets (DB password, JWT)
├── mysql-pvc.yaml           # Stockage persistant MySQL
├── mysql-deployment.yaml    # Déploiement MySQL
├── mysql-service.yaml       # Service interne MySQL
├── kafka-deployment.yaml    # Déploiement Kafka (KRaft)
├── kafka-service.yaml       # Service interne Kafka
├── g9-deployment.yaml       # Déploiement Spring Boot
├── g9-service.yaml          # Service interne Spring Boot
├── hpa.yaml                 # Autoscaling horizontal
├── ingress.yaml             # Routage externe (NGINX)
├── servicemonitor.yaml      # Intégration Prometheus
├── deploy.ps1               # Script de déploiement
├── cleanup.ps1              # Script de nettoyage
└── DEPLOY_MANUAL.md         # Ce fichier
```
