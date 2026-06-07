# SGITU — Service Gestion des Incidents (G9)

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.x-brightgreen.svg)
![Apache Kafka](https://img.shields.io/badge/Kafka-3.7.0-black.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)

Microservice du projet **SGITU** (Système de Gestion Intelligente des Transports Urbains) responsable de la gestion complète du cycle de vie des incidents sur le réseau de transport urbain.

---

## 📋 Prérequis

Avant de démarrer, assurez-vous d'avoir installé sur votre machine :

| Outil | Version minimale | Vérification |
|---|---|---|
| **Java (JDK)** | 21 | `java -version` |
| **Maven** | 3.9+ | `mvn -version` |
| **Docker Desktop** | 4.x | `docker -v` |
| **Docker Compose** | 2.x | `docker compose version` |

---

## 🚀 Option 1 — Lancer avec Docker (Recommandé)

Cette option démarre **tout l'environnement** (MySQL + Kafka + le service) en une seule commande.

```bash
# 1. Lancer tous les conteneurs
docker compose up --build -d

# 2. Vérifier que tout est bien démarré
docker compose ps

# 3. Suivre les logs du service
docker compose logs -f gestion-incidents
```

**Services démarrés :**

| Service | URL locale |
|---|---|
| **API REST** | http://localhost:8089/api/incidents |
| **Swagger UI** | http://localhost:8089/swagger-ui.html |
| **Prometheus** | http://localhost:9090 |
| **Grafana** | http://localhost:3000 (admin / admin123) |
| **MySQL** | localhost:3306 — base: `sgitu_incidents` |
| **Kafka** | localhost:9092 |

**Arrêter l'environnement :**
```bash
docker compose down
# Pour supprimer aussi les volumes (base de données)
docker compose down -v
```

---

## 🛠️ Option 2 — Lancer en mode Développement (IDE / IntelliJ)

Dans ce mode, vous lancez le service directement sur votre machine, mais les dépendances (MySQL + Kafka) doivent tourner dans Docker.

### Étape 1 — Démarrer uniquement MySQL et Kafka

```bash
cd service-gestion-incidents

# Démarrer uniquement les dépendances (sans le service lui-même)
docker compose up mysql-db kafka -d

# Vérifier que MySQL est prêt
docker compose logs mysql-db
# Attendez de voir : "ready for connections"
```

### Étape 2 — Configurer `application.properties`

Le fichier est déjà pré-configuré pour le développement local.  
Vérifiez que les valeurs suivantes dans `src/main/resources/application.properties` correspondent à votre environnement :

```properties
# Base de données
spring.datasource.url=jdbc:mysql://localhost:3306/sgitu_incidents?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=          # Laisser vide si MySQL sans mot de passe

# Kafka
spring.kafka.bootstrap-servers=localhost:9092

# URL du service utilisateur (G3)
microservices.utilisateur.url=http://localhost:8083
```

### Étape 3 — Lancer le service

**Avec Maven (terminal) :**
```bash
./mvnw spring-boot:run
```

**Sur Windows :**
```cmd
mvnw.cmd spring-boot:run
```

**Avec IntelliJ IDEA :**
- Ouvrir `ServiceGestionIncidentsApplication.java`
- Cliquer sur le bouton ▶️ Run

### Étape 4 — Vérifier le démarrage

```bash
# L'API doit répondre sur :
curl http://localhost:8089/api/incidents/tous \
  -H "X-User-Id: 1" \
  -H "X-User-Role: ROLE_SUPERVISOR"
```

---

## ⚙️ Variables d'environnement

En production ou dans Docker, ces variables surchargent `application.properties` :

| Variable | Valeur par défaut | Description |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/...` | URL JDBC de la base de données |
| `DB_USERNAME` | `root` | Utilisateur MySQL |
| `DB_PASSWORD` | *(vide)* | Mot de passe MySQL |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Adresse du broker Kafka |
| `JWT_SECRET` | `SGITU_G3_JWT_SECRET_KEY...` | Clé secrète JWT (à changer en prod !) |
| `SPRING_PROFILES_ACTIVE` | *(non défini)* | Profil Spring (`docker`, etc.) |

---

## 🔐 Authentification pour les tests

Ce service utilise l'authentification par headers injectés par la Gateway. Pour les tests directs via Postman ou `curl`, ajoutez toujours ces deux headers :

```
X-User-Id: <id_de_l_utilisateur>
X-User-Role: <ROLE_PASSENGER | ROLE_DRIVER | ROLE_TECHNICIAN | ROLE_DISPATCHER | ROLE_SUPERVISOR | ROLE_SECURITY | ROLE_MEDIC | ROLE_CLEANER>
```

Pour les endpoints qui contactent le `service-utilisateur` (ex: disponibilité des agents), un **token JWT Bearer** est également requis :
```
Authorization: Bearer <votre_jwt_token>
```

**Obtenir un token via le service-utilisateur (port 8083) :**
```bash
curl -X POST http://localhost:8083/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "dispatcher@sgitu.com", "password": "Password123!"}'
```

---

## 📬 Collection Postman

Une collection Postman complète est incluse dans le projet :

```
SGITU-Incidents.postman_collection.json
```

**Importer dans Postman :**
1. Ouvrir Postman → `File` → `Import`
2. Sélectionner `SGITU-Incidents.postman_collection.json`
3. Définir la variable de collection `baseUrl` → `http://localhost:8089/api/incidents`
4. Exécuter les requêtes dans l'ordre numéroté (1.1 → 1.2 → ...)

---

## 🗂️ Structure du Projet

```
service-gestion-incidents/
├── src/main/java/com/sgitu/servicegestionincidents/
│   ├── controller/         # Endpoints REST (IncidentController, RapportController)
│   ├── service/            # Logique métier (IncidentServiceImpl, RapportServiceImpl)
│   ├── repository/         # Accès base de données (Spring Data JPA)
│   ├── model/
│   │   ├── entity/         # Entités JPA (Incident, Action, Renfort, Preuve...)
│   │   └── enums/          # Enums (StatutIncident, NiveauGravite, TypeIncident...)
│   ├── dto/
│   │   ├── request/        # DTOs de requête (SignalementRequestDTO...)
│   │   └── response/       # DTOs de réponse (IncidentResponseDTO...)
│   ├── messaging/
│   │   ├── event/          # Événements Kafka (IncidentTransportEvent...)
│   │   ├── producer/       # Producteurs Kafka (TransportProducer, NotificationProducer...)
│   │   └── consumer/       # Consommateur Kafka (VehiculeConsumer)
│   ├── config/             # Configuration Spring (Security, Feign, ModelMapper...)
│   ├── client/             # Clients Feign vers d'autres services
│   ├── scheduler/          # Tâches planifiées (SLA, escalades automatiques)
│   └── exception/          # Gestion des erreurs
├── src/main/resources/
│   ├── application.properties          # Configuration locale
│   └── application-docker.yml          # Configuration Docker
├── docker-compose.yml      # Infrastructure locale complète
├── Dockerfile              # Image de production (multi-stage)
└── SGITU-Incidents.postman_collection.json
```

---

## 📡 Endpoints Principaux

| Méthode | Endpoint | Rôles requis | Description |
|---|---|---|---|
| `POST` | `/signaler` | Tous | Signaler un nouvel incident |
| `GET` | `/{id}` | Tous | Consulter un incident |
| `GET` | `/{id}/suivi` | Agents + | Historique des actions |
| `GET` | `` | Agents + | Filtrer les incidents |
| `PUT` | `/{id}/statut` | Agents + | Changer le statut |
| `PUT` | `/{id}/affecter` | Dispatcher + | Affecter un responsable |
| `GET` | `/agents/disponibilite?role=ROLE_TECHNICIAN` | Dispatcher + | Voir la disponibilité des agents |
| `PUT` | `/{id}/demander-escalade` | Techniciens | Soumettre une demande d'escalade |
| `PUT` | `/{id}/escalader` | Dispatcher + | Confirmer une escalade |
| `PUT` | `/{id}/annuler` | Dispatcher + | Annuler (fausse alerte) |
| `PUT` | `/{id}/cloturer` | Dispatcher + | Clôturer définitivement |
| `GET` | `/rapports/generer?periode=mois` | Supervisor | Générer un rapport |
| `GET` | `/rapports/dashboard` | Dispatcher + | Tableau de bord temps réel |

---

