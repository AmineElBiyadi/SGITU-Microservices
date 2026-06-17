# 🚀 Guide de Démarrage - Service Utilisateur (G3)

## 📋 Table des Matières
- [Vue d'ensemble](#-vue-densemble)
- [Prérequis](#-prérequis)
- [Option 1 : Démarrage avec Docker Compose](#-option-1--démarrage-avec-docker-compose)
- [Option 2 : Démarrage avec Kubernetes](#-option-2--démarrage-avec-kubernetes)
- [Vérification du Déploiement](#-vérification-du-déploiement)
- [Tests de l'API](#-tests-de-lapi)
- [Dépannage](#-dépannage)

---

## 🎯 Vue d'ensemble

Le **Service Utilisateur** (Groupe G3) est le microservice responsable de la gestion des utilisateurs et de l'authentification dans le système SGITU. 

### Fonctionnalités Principales
- ✅ Inscription et connexion des utilisateurs
- ✅ Authentification JWT (JSON Web Tokens)
- ✅ Authentification à deux facteurs (2FA)
- ✅ Gestion des profils utilisateurs
- ✅ Gestion des rôles (USER, ADMIN, DRIVER, etc.)
- ✅ Cache Redis pour les performances
- ✅ Intégration Kafka pour la communication inter-services

### Architecture Technique
- **Framework** : Spring Boot 3.x avec Java 21
- **Base de données** : PostgreSQL 15
- **Cache** : Redis 7
- **Messagerie** : Apache Kafka
- **Port** : 8083

---

## ✅ Prérequis

### Pour Docker Compose
- **Docker** : version 20.10 ou supérieure
- **Docker Compose** : version 2.0 ou supérieure
- **Git** : pour cloner le dépôt
- **Minimum 4 GB RAM** recommandé pour le service utilisateur seul
- **5 GB d'espace disque** disponible

### Pour Kubernetes
- **kubectl** : installé et configuré
- **Cluster Kubernetes** : Minikube, Docker Desktop, ou cluster distant
- **Git Bash** (Windows) : pour exécuter les scripts `.sh`
- **Maven 3.9+** et **Java 21** : si vous voulez construire localement

### Vérification des installations

```bash
# Vérifier Docker
docker --version
docker compose version

# Vérifier Kubernetes
kubectl version --client
kubectl cluster-info

# Vérifier Java et Maven (optionnel)
java -version
mvn -version
```

---

## 🐳 Option 1 : Démarrage avec Docker Compose

### Étape 1 : Naviguer vers le Service Utilisateur

```bash
# Cloner le dépôt si ce n'est pas déjà fait
git clone https://github.com/AmineElBiyadi/SGITU-Microservices.git
cd SGITU-Microservices/service-utilisateur
```

### Étape 2 : Créer le Réseau Docker (Première fois seulement)

```bash
# Créer le réseau partagé sgitu-network
docker network create sgitu-network
```

### Étape 3 : Configurer les Variables d'Environnement (Optionnel)

Le service utilise des valeurs par défaut, mais vous pouvez les personnaliser :

```bash
# Créer un fichier .env dans service-utilisateur/
cat > .env << EOF
JWT_SECRET=SGITU_G3_JWT_SECRET_KEY_CHANGE_ME_IN_PRODUCTION_256BITS!!
JWT_EXPIRATION=86400
G8_BASE_URL=
EOF
```

**Variables disponibles :**
- `JWT_SECRET` : Clé secrète pour les tokens JWT (minimum 256 bits)
- `JWT_EXPIRATION` : Durée de validité du token en secondes (86400 = 24h)
- `G8_BASE_URL` : URL du service analytique (optionnel)

### Étape 4 : Démarrer le Service

```bash
# Démarrer tous les conteneurs en arrière-plan
docker compose up -d

# Voir les logs en temps réel (optionnel)
docker compose logs -f
```

**⏱️ Temps de démarrage estimé : 2-3 minutes**

Le système va :
1. Construire l'image Docker du service utilisateur (première fois : ~3-5 min)
2. Démarrer PostgreSQL et attendre qu'il soit prêt
3. Démarrer Redis pour le cache
4. Démarrer Kafka pour la messagerie
5. Démarrer le service utilisateur
6. Initialiser automatiquement le schéma de la base de données

### Étape 5 : Vérifier que le Service est Prêt

```bash
# Vérifier l'état de tous les conteneurs
docker compose ps

# Tous les conteneurs doivent afficher "Up" ou "healthy"
# Exemple de sortie attendue :
# NAME            STATUS                   PORTS
# user-service    Up 2 minutes            0.0.0.0:8083->8083/tcp
# users-db        Up 2 minutes (healthy)  0.0.0.0:5432->5432/tcp
# users-redis     Up 2 minutes            0.0.0.0:6379->6379/tcp
# kafka           Up 2 minutes            0.0.0.0:9092->9092/tcp
```

### Étape 6 : Tester l'API

```bash
# Test de santé
curl http://localhost:8083/api/actuator/health

# Réponse attendue : {"status":"UP"}
```

### Commandes Utiles Docker Compose

```bash
# Voir les logs d'un conteneur spécifique
docker compose logs -f user-service
docker compose logs -f users-db

# Arrêter tous les services
docker compose down

# Arrêter et supprimer les volumes (⚠️ efface les données)
docker compose down -v

# Redémarrer le service après modification du code
docker compose restart user-service

# Reconstruire l'image après modification du code
docker compose up -d --build user-service

# Voir les statistiques des ressources
docker stats
```

### Accès aux Composants

| Composant | URL | Credentials |
|-----------|-----|-------------|
| **Service Utilisateur (API)** | http://localhost:8083 | - |
| **PostgreSQL** | localhost:5432 | postgres / secret |
| **Redis** | localhost:6379 | - |
| **Kafka** | localhost:9092 | - |

---

## ☸️ Option 2 : Démarrage avec Kubernetes

### 🔴 IMPORTANT : Suppression des Pods Existants

**Avant de déployer une nouvelle version du code, il est OBLIGATOIRE de supprimer les anciens pods pour forcer Kubernetes à utiliser les nouvelles images.**

```bash
# Supprimer les anciens pods
kubectl delete pods -l app=user-service -n sgitu

# OU supprimer tout le déploiement
kubectl delete deployment user-service -n sgitu
```

### Étape 1 : Naviguer vers le Dossier Kubernetes

```bash
cd service-utilisateur/k8s
```

### Étape 2 : Vérifier la Connexion au Cluster

```bash
# Vérifier que kubectl est connecté
kubectl cluster-info

# Vérifier les contextes disponibles
kubectl config get-contexts

# Changer de contexte si nécessaire
kubectl config use-context <nom-du-contexte>
```

### Étape 3 : Construire l'Image Docker

**⚠️ CRITIQUE : Construisez l'image Docker AVANT le déploiement**

```bash
# Option 1 : Depuis le dossier service-utilisateur
cd ..  # Revenir au dossier service-utilisateur
docker build -t g3-user-service:1.0 .

# Option 2 : Si vous utilisez Minikube, utilisez son daemon Docker
eval $(minikube docker-env)
docker build -t g3-user-service:1.0 .
```

### Étape 4 : Déployer avec les Scripts Automatisés

Le projet fournit deux scripts de déploiement selon votre système d'exploitation :

#### 🪟 Pour Windows (PowerShell)

```powershell
# Dans le dossier service-utilisateur\k8s
.\deploy.ps1
```

#### 🐧 Pour Git Bash / Linux / macOS

```bash
# Dans le dossier service-utilisateur/k8s
# Rendre le script exécutable (première fois seulement)
chmod +x deploy.sh

# Exécuter le script
./deploy.sh
```

### Étape 5 : Que Font les Scripts de Déploiement ?

Les scripts automatisent les étapes suivantes dans l'ordre :

1. ✅ **Vérification** : kubectl et connexion au cluster
2. ✅ **Namespace** : Création du namespace `sgitu`
3. ✅ **Configuration** : Application des ConfigMaps et Secrets
4. ✅ **PostgreSQL** : 
   - Création du PVC (Persistent Volume Claim) pour le stockage
   - Déploiement de PostgreSQL
   - Création du service PostgreSQL
5. ✅ **Redis** : Déploiement et service
6. ✅ **Kafka** : Déploiement et service
7. ✅ **Attente** : Les scripts attendent que PostgreSQL, Redis et Kafka soient prêts (max 120s)
8. ✅ **Service Utilisateur** : 
   - Déploiement du service utilisateur
   - Création du service Kubernetes (LoadBalancer/NodePort)
9. ✅ **Ingress** : Configuration de l'Ingress (si Ingress Controller disponible)
10. ✅ **HPA** : Horizontal Pod Autoscaler (si Metrics Server disponible)
11. ✅ **Network Policies** : Politiques de sécurité réseau
12. ✅ **ServiceMonitor** : Pour Prometheus (si Prometheus Operator disponible)

### Étape 6 : Surveiller le Déploiement

```bash
# Vérifier l'état des pods
kubectl get pods -n sgitu

# Sortie attendue (après quelques minutes) :
# NAME                            READY   STATUS    RESTARTS   AGE
# user-service-xxxxxxxxxx-xxxxx   1/1     Running   0          2m
# postgres-xxxxxxxxxx-xxxxx       1/1     Running   0          3m
# redis-xxxxxxxxxx-xxxxx          1/1     Running   0          3m
# kafka-xxxxxxxxxx-xxxxx          1/1     Running   0          3m

# Voir les logs en temps réel
kubectl logs -f deployment/user-service -n sgitu

# Voir tous les services
kubectl get svc -n sgitu

# Vérifier les événements en cas de problème
kubectl get events -n sgitu --sort-by='.lastTimestamp'
```

### Étape 7 : Accéder au Service

#### Méthode 1 : Port Forwarding (Développement - Recommandé)

```bash
# Rediriger le port 8083 vers votre machine locale
kubectl port-forward svc/user-service 8083:8083 -n sgitu

# Le service est maintenant accessible sur http://localhost:8083
# Testez avec : curl http://localhost:8083/api/actuator/health
```

#### Méthode 2 : NodePort (Minikube)

```bash
# Pour Minikube
minikube service user-service -n sgitu

# Cela ouvrira automatiquement le service dans votre navigateur
```

#### Méthode 3 : Via Ingress (Production)

Si un Ingress Controller est installé :

```bash
# Voir l'adresse IP de l'Ingress
kubectl get ingress -n sgitu

# Accéder via : http://<INGRESS-IP>/api/users/...
```

### 🔄 Mise à Jour du Code (PROCÉDURE OBLIGATOIRE)

**Pour déployer une nouvelle version après modification du code, suivez ces étapes :**

#### Méthode 1 : Suppression et Reconstruction (Recommandée)

```bash
# 1. Supprimer les pods existants (OBLIGATOIRE)
kubectl delete pods -l app=user-service -n sgitu

# 2. Reconstruire l'image Docker
cd service-utilisateur
docker build -t g3-user-service:1.0 .

# Pour Minikube :
eval $(minikube docker-env)
docker build -t g3-user-service:1.0 .

# 3. Forcer le redéploiement
kubectl rollout restart deployment/user-service -n sgitu

# 4. Vérifier le statut du déploiement
kubectl rollout status deployment/user-service -n sgitu

# 5. Vérifier que les nouveaux pods sont créés
kubectl get pods -n sgitu -w
```

#### Méthode 2 : Suppression Complète et Redéploiement

```bash
# 1. Supprimer le déploiement complet
kubectl delete deployment user-service -n sgitu

# 2. Reconstruire l'image
cd service-utilisateur
docker build -t g3-user-service:1.0 .

# 3. Réexécuter le script de déploiement
cd k8s
./deploy.sh  # ou .\deploy.ps1 sur Windows
```

#### Méthode 3 : Avec un Registry Docker (Production)

```bash
# 1. Tag et push vers un registry
docker tag g3-user-service:1.0 myregistry.com/g3-user-service:1.0
docker push myregistry.com/g3-user-service:1.0

# 2. Mettre à jour le déploiement
kubectl set image deployment/user-service user-service=myregistry.com/g3-user-service:1.0 -n sgitu

# 3. Vérifier
kubectl rollout status deployment/user-service -n sgitu
```

### Commandes Kubernetes Utiles

```bash
# Voir tous les pods avec détails
kubectl get pods -n sgitu -o wide

# Décrire un pod (diagnostic détaillé)
kubectl describe pod <nom-du-pod> -n sgitu

# Entrer dans un pod pour débuguer
kubectl exec -it <nom-du-pod> -n sgitu -- /bin/sh

# Voir les logs avec timestamp
kubectl logs -f deployment/user-service -n sgitu --timestamps

# Voir les logs des 100 dernières lignes
kubectl logs --tail=100 deployment/user-service -n sgitu

# Voir l'utilisation des ressources
kubectl top pods -n sgitu
kubectl top nodes

# Voir la configuration du déploiement
kubectl get deployment user-service -n sgitu -o yaml

# Scaler manuellement les pods
kubectl scale deployment user-service --replicas=3 -n sgitu

# Supprimer tout le namespace (⚠️ efface tout)
kubectl delete namespace sgitu
```

### Nettoyage Complet (Scripts Fournis)

Le projet inclut des scripts de nettoyage :

#### Windows PowerShell
```powershell
cd service-utilisateur\k8s
.\cleanup.ps1
```

#### Linux / macOS / Git Bash
```bash
cd service-utilisateur/k8s
chmod +x cleanup.sh
./cleanup.sh
```

Ces scripts suppriment tous les déploiements, services et configurations Kubernetes du service utilisateur.

---

## ✅ Vérification du Déploiement

### Tests de Base

#### 1. Tester l'API de Santé (Health Check)

```bash
# Docker Compose
curl http://localhost:8083/api/actuator/health

# Kubernetes (après port-forward)
kubectl port-forward svc/user-service 8083:8083 -n sgitu
curl http://localhost:8083/api/actuator/health
```

**Réponse attendue :**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

#### 2. Vérifier les Métriques Actuator

```bash
# Informations sur l'application
curl http://localhost:8083/api/actuator/info

# Métriques Prometheus
curl http://localhost:8083/api/actuator/prometheus
```

#### 3. Vérifier la Base de Données PostgreSQL

**Docker Compose :**
```bash
# Se connecter à PostgreSQL
docker exec -it users-db psql -U postgres -d users_db

# Lister les tables
\dt

# Voir la table des utilisateurs
SELECT * FROM users LIMIT 5;

# Quitter
\q
```

**Kubernetes :**
```bash
# Se connecter à PostgreSQL
kubectl exec -it deployment/postgres -n sgitu -- psql -U postgres -d users_db

# Lister les tables
\dt

# Quitter
\q
```

#### 4. Vérifier Redis

**Docker Compose :**
```bash
# Se connecter à Redis
docker exec -it users-redis redis-cli

# Tester
PING
# Réponse : PONG

# Voir toutes les clés
KEYS *

# Quitter
exit
```

**Kubernetes :**
```bash
kubectl exec -it deployment/redis -n sgitu -- redis-cli PING
```

#### 5. Vérifier Kafka

**Docker Compose :**
```bash
# Voir les topics Kafka
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

**Kubernetes :**
```bash
kubectl exec -it deployment/kafka -n sgitu -- kafka-topics --list --bootstrap-server localhost:9092
```

---

## 🧪 Tests de l'API

### 1. Inscription d'un Utilisateur

```bash
curl -X POST http://localhost:8083/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123!",
    "phoneNumber": "+212600000000",
    "firstName": "Test",
    "lastName": "User"
  }'
```

**Réponse attendue :**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "testuser",
  "email": "test@example.com",
  "roles": ["USER"]
}
```

### 2. Connexion

```bash
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123!"
  }'
```

**Réponse attendue :**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "testuser",
  "email": "test@example.com",
  "roles": ["USER"]
}
```

### 3. Obtenir le Profil (avec token)

```bash
# Remplacez <TOKEN> par le token obtenu lors de la connexion
curl -X GET http://localhost:8083/api/users/profile \
  -H "Authorization: Bearer <TOKEN>"
```

**Réponse attendue :**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "firstName": "Test",
  "lastName": "User",
  "phoneNumber": "+212600000000",
  "roles": ["USER"],
  "twoFactorEnabled": false,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### 4. Activer 2FA (Authentification à Deux Facteurs)

```bash
curl -X POST http://localhost:8083/api/users/2fa/enable \
  -H "Authorization: Bearer <TOKEN>"
```

**Réponse attendue :**
```json
{
  "qrCodeUrl": "data:image/png;base64,iVBORw0KG...",
  "secret": "JBSWY3DPEHPK3PXP",
  "message": "Scannez le QR code avec Google Authenticator"
}
```

### 5. Lister Tous les Utilisateurs (Admin uniquement)

```bash
curl -X GET http://localhost:8083/api/users \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

### Script de Test Complet

Créez un fichier `test-api.sh` :

```bash
#!/bin/bash

API_URL="http://localhost:8083/api"

echo "=== Test 1 : Health Check ==="
curl -s $API_URL/actuator/health | jq
echo ""

echo "=== Test 2 : Inscription ==="
REGISTER_RESPONSE=$(curl -s -X POST $API_URL/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "demo'$(date +%s)'",
    "email": "demo'$(date +%s)'@test.com",
    "password": "Demo123!",
    "phoneNumber": "+212600000000",
    "firstName": "Demo",
    "lastName": "User"
  }')
echo $REGISTER_RESPONSE | jq
TOKEN=$(echo $REGISTER_RESPONSE | jq -r '.token')
echo "Token obtenu : $TOKEN"
echo ""

echo "=== Test 3 : Obtenir le profil ==="
curl -s -X GET $API_URL/users/profile \
  -H "Authorization: Bearer $TOKEN" | jq
echo ""

echo "=== Tests terminés ==="
```

Rendre exécutable et lancer :
```bash
chmod +x test-api.sh
./test-api.sh
```

---

## 🔧 Dépannage

### Problème : Le conteneur user-service redémarre en boucle (Docker)

**Symptômes :** `docker compose ps` montre "Restarting" pour user-service

**Solution :**
```bash
# 1. Voir les logs pour identifier l'erreur
docker compose logs user-service

# Erreurs courantes et solutions :

# Erreur : "Connection refused to PostgreSQL"
# → Attendre que PostgreSQL soit prêt
docker compose logs users-db
# Si PostgreSQL n'est pas healthy, redémarrer :
docker compose restart users-db

# Erreur : "JWT_SECRET is null or empty"
# → Vérifier le fichier .env ou les variables d'environnement
cat .env

# 2. Redémarrer proprement
docker compose down
docker compose up -d

# 3. Si le problème persiste, reconstruire
docker compose down -v  # ⚠️ efface les données
docker compose up -d --build
```

### Problème : Erreur de connexion à PostgreSQL

**Symptômes :** Logs montrent "Connection refused" ou "password authentication failed"

**Vérifications :**
```bash
# 1. Vérifier que PostgreSQL est démarré
docker compose ps users-db

# 2. Vérifier le health check
docker inspect users-db | grep -A 5 Health

# 3. Tester la connexion manuellement
docker exec -it users-db psql -U postgres -d users_db -c "SELECT 1;"

# 4. Vérifier les credentials dans docker-compose.yml
cat docker-compose.yml | grep -A 5 POSTGRES
```

**Solution :**
```bash
# Si les credentials sont incorrects, recréer la base
docker compose down -v
docker compose up -d
```

### Problème : Kubernetes - Pods en état ImagePullBackOff

**Symptômes :** `kubectl get pods -n sgitu` montre "ImagePullBackOff"

**Cause :** L'image Docker n'existe pas localement ou dans le registry

**Solution :**
```bash
# 1. Vérifier que l'image existe
docker images | grep user-service

# Si l'image n'existe pas :
# 2. Construire l'image
cd service-utilisateur
docker build -t g3-user-service:1.0 .

# 3. Pour Minikube, utiliser son daemon Docker
eval $(minikube docker-env)
docker build -t g3-user-service:1.0 .

# 4. Vérifier à nouveau
docker images | grep user-service

# 5. Supprimer le pod pour qu'il se recrée
kubectl delete pod -l app=user-service -n sgitu

# 6. Surveiller le nouveau pod
kubectl get pods -n sgitu -w
```

### Problème : Kubernetes - Pods ne démarrent pas après modification du code

**Symptômes :** Les modifications du code ne sont pas reflétées dans le service

**Cause :** Kubernetes utilise l'ancienne image en cache

**Solution (PROCÉDURE OBLIGATOIRE) :**
```bash
# Méthode 1 : Suppression forcée des pods
kubectl delete pods -l app=user-service -n sgitu
kubectl rollout restart deployment/user-service -n sgitu

# Méthode 2 : Supprimer et reconstruire
# 1. Supprimer le déploiement
kubectl delete deployment user-service -n sgitu

# 2. Reconstruire l'image (IMPORTANT)
cd service-utilisateur
eval $(minikube docker-env)  # Si Minikube
docker build -t g3-user-service:1.0 .

# 3. Redéployer
cd k8s
./deploy.sh  # ou .\deploy.ps1

# Méthode 3 : Forcer avec imagePullPolicy
# Modifier user-service-deployment.yaml :
# imagePullPolicy: Always  → imagePullPolicy: Never (pour images locales)
kubectl apply -f user-service-deployment.yaml
```

### Problème : Kubernetes - CrashLoopBackOff

**Symptômes :** Le pod redémarre constamment

**Solution :**
```bash
# 1. Voir les logs du pod qui crash
kubectl logs -f deployment/user-service -n sgitu --previous

# 2. Voir les événements
kubectl describe pod <nom-pod> -n sgitu

# Causes fréquentes :

# A. Base de données non prête
kubectl get pods -n sgitu | grep postgres
# → Attendre que PostgreSQL soit Running

# B. Erreur de configuration
kubectl get configmap -n sgitu
kubectl describe configmap user-service-config -n sgitu
# → Vérifier les variables d'environnement

# C. Ressources insuffisantes
kubectl top nodes
kubectl describe node
# → Augmenter les ressources du cluster
```

### Problème : Port déjà utilisé (Docker)

**Symptômes :** "Bind for 0.0.0.0:8083 failed: port is already allocated"

**Solution Windows :**
```powershell
# Trouver le processus qui utilise le port
netstat -ano | findstr :8083

# Tuer le processus (remplacer <PID>)
taskkill /PID <PID> /F
```

**Solution Linux/Mac :**
```bash
# Trouver le processus
lsof -i :8083

# Tuer le processus
kill -9 <PID>
```

**Alternative :** Changer le port dans docker-compose.yml :
```yaml
ports:
  - "8084:8083"  # Utiliser 8084 au lieu de 8083
```

### Problème : Manque de mémoire (Docker)

**Symptômes :** Conteneurs lents ou qui crashent

**Solution :**
```bash
# 1. Vérifier l'utilisation
docker stats

# 2. Augmenter la mémoire Docker Desktop
# Settings > Resources > Memory (recommandé : 4-8 GB)

# 3. Libérer de l'espace
docker system prune -a --volumes

# 4. Supprimer les images inutilisées
docker image prune -a
```

### Problème : Manque de mémoire (Kubernetes/Minikube)

**Solution :**
```bash
# Augmenter les ressources Minikube
minikube config set memory 8192
minikube config set cpus 4

# Redémarrer Minikube
minikube stop
minikube delete
minikube start

# Vérifier
minikube config view
```

### Problème : JWT Secret invalide

**Symptômes :** Erreurs "JWT signature does not match" ou "Invalid token"

**Solution Docker Compose :**
```bash
# 1. Générer un secret sécurisé (256 bits minimum)
openssl rand -base64 32

# 2. Mettre à jour dans le fichier .env ou docker-compose.yml
JWT_SECRET=<votre-nouveau-secret>

# 3. Redémarrer le service
docker compose down
docker compose up -d
```

**Solution Kubernetes :**
```bash
# 1. Mettre à jour le secret
kubectl edit secret user-service-secrets -n sgitu

# Ou recréer le secret :
kubectl delete secret user-service-secrets -n sgitu
kubectl create secret generic user-service-secrets \
  --from-literal=jwt-secret=$(openssl rand -base64 32) \
  -n sgitu

# 2. Redémarrer le déploiement
kubectl rollout restart deployment/user-service -n sgitu
```

### Problème : Impossible de se connecter à Redis

**Symptômes :** Logs montrent "Unable to connect to Redis"

**Solution Docker :**
```bash
# Vérifier que Redis est démarré
docker compose ps redis

# Tester Redis
docker exec -it users-redis redis-cli PING
# Doit retourner : PONG

# Redémarrer Redis
docker compose restart redis
```

**Solution Kubernetes :**
```bash
# Vérifier le pod Redis
kubectl get pods -n sgitu | grep redis

# Tester Redis
kubectl exec -it deployment/redis -n sgitu -- redis-cli PING

# Voir les logs
kubectl logs deployment/redis -n sgitu
```

### Problème : Kafka ne démarre pas

**Symptômes :** Le conteneur Kafka redémarre

**Solution :**
```bash
# Voir les logs
docker compose logs kafka

# Kafka nécessite plus de mémoire - augmenter dans Docker Desktop

# Supprimer le volume Kafka et recommencer
docker compose down -v
docker compose up -d
```

### Problème : Erreur "Network sgitu-network not found" (Docker)

**Solution :**
```bash
# Créer le réseau
docker network create sgitu-network

# Puis redémarrer
docker compose up -d
```

### Problème : Les tables PostgreSQL ne sont pas créées

**Symptômes :** Erreur "relation 'users' does not exist"

**Cause :** Hibernate n'a pas créé les tables

**Solution :**
```bash
# 1. Vérifier les logs de démarrage
docker compose logs user-service | grep -i "hibernate"

# 2. Vérifier la configuration
# Dans docker-compose.yml ou application.yml :
# spring.jpa.hibernate.ddl-auto devrait être "update" ou "create"

# 3. Forcer la recréation
docker compose down -v
docker compose up -d

# 4. Vérifier que les tables existent
docker exec -it users-db psql -U postgres -d users_db -c "\dt"
```

---

## 📚 Documentation Complémentaire

### Fichiers de Documentation du Service Utilisateur

- **2FA Testing Guide** : `/service-utilisateur/2FA_TESTING_GUIDE.md`
- **Implementation Status** : `/service-utilisateur/IMPLEMENTATION_STATUS.md`
- **Kafka Configuration** : `/service-utilisateur/KAFKA_KRAFT_EXPLANATION.md`
- **Kubernetes Validation** : `/service-utilisateur/K8S_VALIDATION_RAPPORT.md`
- **Kubernetes Manual Deployment** : `/service-utilisateur/k8s/DEPLOY_MANUAL.md`
- **Kubernetes README** : `/service-utilisateur/k8s/README.md`

### Endpoints Principaux de l'API

| Endpoint | Méthode | Description | Auth Requise |
|----------|---------|-------------|--------------|
| `/api/actuator/health` | GET | Health check | Non |
| `/api/auth/register` | POST | Inscription | Non |
| `/api/auth/login` | POST | Connexion | Non |
| `/api/users/profile` | GET | Profil utilisateur | Oui |
| `/api/users` | GET | Liste utilisateurs | Oui (Admin) |
| `/api/users/{id}` | GET | Détails utilisateur | Oui |
| `/api/users/2fa/enable` | POST | Activer 2FA | Oui |
| `/api/users/2fa/verify` | POST | Vérifier 2FA | Oui |

### Variables d'Environnement

| Variable | Description | Valeur par Défaut | Requis |
|----------|-------------|-------------------|--------|
| `SERVER_PORT` | Port du service | 8083 | Non |
| `SPRING_DATASOURCE_URL` | URL PostgreSQL | jdbc:postgresql://... | Oui |
| `SPRING_DATASOURCE_USERNAME` | User PostgreSQL | postgres | Oui |
| `SPRING_DATASOURCE_PASSWORD` | Password PostgreSQL | secret | Oui |
| `SPRING_REDIS_HOST` | Host Redis | redis | Oui |
| `SPRING_REDIS_PORT` | Port Redis | 6379 | Non |
| `JWT_SECRET` | Secret pour JWT | - | Oui |
| `JWT_EXPIRATION` | Expiration JWT (sec) | 86400 | Non |
| `KAFKA_BOOTSTRAP_SERVERS` | Serveurs Kafka | kafka:9092 | Oui |
| `G8_BASE_URL` | URL service analytique | - | Non |

---

## 🤝 Support et Aide

### En cas de problème non résolu :

1. **Vérifier les logs** :
   - Docker : `docker compose logs user-service`
   - Kubernetes : `kubectl logs -f deployment/user-service -n sgitu`

2. **Consulter la section Dépannage** ci-dessus

3. **Vérifier les prérequis** : Docker, Kubernetes, ports disponibles

4. **Vérifier les issues GitHub** du projet

5. **Ressources utiles** :
   - Documentation Spring Boot : https://spring.io/projects/spring-boot
   - Documentation Kubernetes : https://kubernetes.io/docs/
   - Documentation Docker : https://docs.docker.com/

---

## 📝 Notes Importantes

### Pour Docker Compose ✅

- Le service démarre automatiquement les dépendances (PostgreSQL, Redis, Kafka)
- Les données persistent dans des volumes Docker
- Le réseau `sgitu-network` doit être créé la première fois
- Temps de build initial : 3-5 minutes
- Temps de démarrage : 2-3 minutes

### Pour Kubernetes ⚠️

- **OBLIGATOIRE** : Construire l'image Docker AVANT le déploiement
- **OBLIGATOIRE** : Supprimer les anciens pods pour déployer du nouveau code
- Les scripts `deploy.ps1` (Windows) et `deploy.sh` (Linux/Mac) automatisent tout
- Les PVC (stockage) persistent après suppression des pods
- Pour Minikube : utilisez `eval $(minikube docker-env)` avant de builder
- Vérifier que kubectl est connecté au bon cluster avant de déployer

### Sécurité 🔒

- **Ne jamais** utiliser les secrets par défaut en production
- Générer un JWT_SECRET unique : `openssl rand -base64 32`
- Changer les mots de passe PostgreSQL par défaut
- En production, utiliser un gestionnaire de secrets (Vault, AWS Secrets Manager, etc.)

---

## 🎓 Commandes Récapitulatives

### Docker Compose - Démarrage Rapide

```bash
# Créer le réseau (première fois)
docker network create sgitu-network

# Démarrer
cd service-utilisateur
docker compose up -d

# Vérifier
docker compose ps
curl http://localhost:8083/api/actuator/health

# Voir les logs
docker compose logs -f user-service

# Arrêter
docker compose down
```

### Kubernetes - Démarrage Rapide

```bash
# Windows PowerShell
cd service-utilisateur
docker build -t g3-user-service:1.0 .
cd k8s
.\deploy.ps1

# Linux / Mac / Git Bash
cd service-utilisateur
docker build -t g3-user-service:1.0 .
cd k8s
chmod +x deploy.sh
./deploy.sh

# Vérifier
kubectl get pods -n sgitu
kubectl port-forward svc/user-service 8083:8083 -n sgitu
curl http://localhost:8083/api/actuator/health
```

### Mise à Jour du Code - Kubernetes (OBLIGATOIRE)

```bash
# 1. SUPPRIMER les anciens pods
kubectl delete pods -l app=user-service -n sgitu

# 2. Reconstruire l'image
cd service-utilisateur
eval $(minikube docker-env)  # Si Minikube
docker build -t g3-user-service:1.0 .

# 3. Redémarrer le déploiement
kubectl rollout restart deployment/user-service -n sgitu

# 4. Vérifier le statut
kubectl rollout status deployment/user-service -n sgitu
kubectl get pods -n sgitu -w
```

---

**✨ Bon démarrage avec le Service Utilisateur SGITU ! ✨**

Pour toute question, consultez la documentation dans `/service-utilisateur/docs/` ou les fichiers Markdown à la racine du service.
