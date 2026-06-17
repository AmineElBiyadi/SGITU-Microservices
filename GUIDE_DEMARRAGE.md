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


### Étape 1: Créer le Réseau Docker (Première fois seulement)

```bash
# Créer le réseau partagé sgitu-network
docker network create sgitu-network
```

### Étape 2: Configurer les Variables d'Environnement (Optionnel)

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

### Étape 3 : Démarrer le Service

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

### Étape 4 : Vérifier que le Service est Prêt

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

### Étape 5 : Tester l'API

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



### 🔄 Mise à Jour du Code (PROCÉDURE OBLIGATOIRE)



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

