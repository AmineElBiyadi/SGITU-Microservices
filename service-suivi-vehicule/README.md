# 🛰️ Microservice G7 : Suivi des Véhicules

Ce microservice fait partie du projet **SGITU** (Système de Gestion Intégrée des Transports Urbains).  
Il est responsable du **suivi en temps réel des positions GPS** des véhicules, de la **détection automatique des anomalies** de conduite (excès de vitesse, freinage brusque, immobilisation, déviation d'itinéraire, etc.), et de la **publication d'événements** vers Apache Kafka.

---

## 🛠️ Prérequis

- **Java Development Kit (JDK) 17** ou supérieur.
- **Maven 3.8+** (ou utiliser le wrapper `./mvnw` fourni dans le projet).
- **Docker & Docker Compose** (pour démarrer PostgreSQL et Kafka).
- **Python 3.x** (optionnel, pour le simulateur GPS de test).

---

## ⚙️ Configuration

Le microservice est configuré via [`src/main/resources/application.properties`](src/main/resources/application.properties).

Les variables d'environnement Spring Boot reconnues sont les suivantes (avec leur valeur par défaut locale) :

| Variable d'environnement Spring | Description | Valeur par défaut (locale) |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | URL JDBC de la base PostgreSQL | `jdbc:postgresql://localhost:5437/g7_vehicules` |
| `SPRING_DATASOURCE_USERNAME` | Utilisateur PostgreSQL | `g7user` |
| `SPRING_DATASOURCE_PASSWORD` | Mot de passe PostgreSQL | `g7password` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Adresse du broker Kafka | `localhost:29093` |
| `JWT_SECRET` | Clé secrète pour la validation JWT | `sgitu_g7_secret_key_2025_very_long_secret` |
| `G4_INTEGRATION_URL` | URL du service Coordination (G4) | `http://api-gateway:8080/api/g4` |
| `G5_NOTIFICATION_URL` | URL du service Notification (G5) | `http://api-gateway:8080/api/notifications/send` |
| `G9_INTEGRATION_URL` | URL du service Incidents (G9) | `http://localhost:8089/api/v1/incidents` |

> **Note :** Aucune configuration n'est requise pour le lancement local. Les valeurs par défaut ci-dessus s'appliquent automatiquement si les variables d'environnement ne sont pas définies.

---

## 🚀 Démarrage local (Mode Développement)

### Étape 1 — Démarrer les dépendances (PostgreSQL + Kafka)

Depuis la **racine du projet global**, lancez uniquement les conteneurs nécessaires à G7 :

```bash
docker compose up db-g7 kafka -d
```

Attendez que les deux conteneurs soient sains (`healthy`) :

```bash
docker compose ps
```

### Étape 2 — Lancer le microservice G7

Positionnez-vous dans le dossier du microservice :

```bash
cd service-suivi-vehicule
```

Exécutez l'application :

- **Windows (PowerShell / Cmd)** :
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
- **Linux / macOS** :
  ```bash
  chmod +x mvnw
  ./mvnw spring-boot:run
  ```

✅ Le service démarre sur le port **`8087`**.  
Attendez le message `Started G7SuiviVehiculesApplication` dans les logs.

---

## 📦 Construction et Déploiement avec Docker

Le [Dockerfile](Dockerfile) inclus utilise un build **multi-stage** optimisé pour la production.

### 1. Construire l'image

```bash
docker build -t service-suivi-vehicule:latest .
```

### 2. Lancer le conteneur

> ⚠️ La base de données `db-g7` et Kafka doivent être démarrés au préalable (`docker compose up db-g7 kafka -d`).

```bash
docker run -d -p 8087:8087 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5437/g7_vehicules \
  -e SPRING_DATASOURCE_USERNAME=g7user \
  -e SPRING_DATASOURCE_PASSWORD=g7password \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:29093 \
  --name g7-service-container \
  service-suivi-vehicule:latest
```

> Sur **Linux**, remplacez `host.docker.internal` par l'adresse IP de votre machine hôte (ex. `172.17.0.1`).

---

## 🧪 Exécution des Tests

Les tests utilisent une base de données H2 en mémoire et désactivent Kafka automatiquement via le profil `test` :

```bash
# Windows
.\mvnw.cmd test -Dspring.profiles.active=test

# Linux / macOS
./mvnw test -Dspring.profiles.active=test
```

Les rapports de tests sont générés dans `target/surefire-reports/`.

---

## 🚗 Simulateur GPS (Test en Direct)

Un script Python simule le déplacement d'un véhicule et envoie des positions GPS réelles au service.

### 1. Installer la dépendance

```bash
pip install requests
```

### 2. Lancer le simulateur

```bash
python scripts/simulateur_g7.py
```

> ⚠️ Le microservice G7 doit être démarré et accessible sur `http://localhost:8087` avant de lancer ce script.

Le script envoie une position GPS toutes les **3 secondes** depuis Casablanca et déclenche aléatoirement des alertes de niveaux WARN/ERROR/FATAL.

---

## 🔌 Endpoints Principaux

> La documentation complète et interactive est disponible via **Swagger UI** (voir ci-dessous).

### Gestion de la Flotte — `/api/suivi-vehicules/vehicules`

| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/vehicules` | Liste tous les véhicules |
| `GET` | `/vehicules/{id}` | Détail d'un véhicule par UUID |
| `GET` | `/vehicules/actifs` | Véhicules en statut `EN_SERVICE` |
| `GET` | `/vehicules/statut/{statut}` | Filtrer par statut |
| `GET` | `/vehicules/type/{type}` | Filtrer par type (BUS, TRAM…) |
| `GET` | `/vehicules/{id}/snapshot` | Snapshot temps réel d'un véhicule |
| `GET` | `/vehicules/snapshot` | Snapshot de toute la flotte |
| `POST` | `/vehicules` | Créer un nouveau véhicule (**ROLE_ADMIN** requis) |
| `PUT` | `/vehicules/{id}` | Modifier les infos d'un véhicule |
| `PUT` | `/vehicules/{id}/statut` | Changer le statut opérationnel |
| `DELETE` | `/vehicules/{id}` | Désactiver un véhicule (non-destructif → `HORS_SERVICE`) |

### Suivi GPS — `/api/suivi-vehicules/positions`

| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/positions` | Enregistrer une position GPS (déclenche la détection d'anomalies) |
| `GET` | `/positions` | Historique complet, toutes positions |
| `GET` | `/positions/{vehiculeId}` | Dernière position connue d'un véhicule |
| `GET` | `/positions/{vehiculeId}/historique` | Historique des positions d'un véhicule |
| `GET` | `/positions/{vehiculeId}/vitesse-moyenne` | Vitesse moyenne en km/h |
| `GET` | `/positions/{vehiculeId}/retard` | Secondes depuis la dernière position reçue |
| `DELETE` | `/positions/{vehiculeId}/historique` | Purger l'historique GPS (irréversible) |

### Alertes & Anomalies — `/api/suivi-vehicules/alerts`

| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/alerts` | Lister les alertes (filtres : `vehiculeId`, `statut`, `typeAlert`) |
| `GET` | `/alerts/{id}` | Détail d'une alerte par UUID |
| `GET` | `/alerts/active` | Toutes les alertes en cours (`OUVERTE`) |
| `GET` | `/alerts/snapshot` | Snapshot instantané des alertes actives |
| `GET` | `/alerts/vehicule/{vehiculeId}` | Historique des alertes d'un véhicule |
| `GET` | `/alerts/vehicule/{vehiculeId}/active` | Alertes actives d'un véhicule |
| `GET` | `/alerts/stats` | Statistiques agrégées (pour G8) |
| `PUT` | `/alerts/{id}/cancel` | Annuler manuellement une fausse alerte |

### Santé & Monitoring

| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/suivi-vehicules/health` | Health check personnalisé G7 |
| `GET` | `/actuator/health` | Health check Spring Boot Actuator |
| `GET` | `/actuator/prometheus` | Métriques Prometheus |

---

## 📖 Documentation Interactive (Swagger)

Une fois le service démarré, accédez à la documentation complète de l'API :

👉 **[http://localhost:8087/swagger-ui/index.html](http://localhost:8087/swagger-ui/index.html)**

ou via le chemin configuré :

👉 **[http://localhost:8087/swagger-ui.html](http://localhost:8087/swagger-ui.html)**
