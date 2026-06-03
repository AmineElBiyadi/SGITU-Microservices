# 🛰️ Microservice G7 : Suivi des Véhicules

Ce microservice fait partie du projet **SGITU** (Système de Gestion Intégrée des Transports Urbains). Il est responsable du suivi en temps réel des positions des véhicules, de la détection des anomalies de conduite (excès de vitesse, freinages brusques, etc.), et de la publication de ces événements vers le bus de messages Kafka.

---

## 🛠️ Prérequis

Pour exécuter ce microservice localement en mode développement, vous aurez besoin de :

- **Java Development Kit (JDK) 17** ou supérieur.
- **Maven 3.8+** (ou utiliser le wrapper `./mvnw` fourni).
- **Docker & Docker Compose** (pour exécuter la base de données PostgreSQL et le broker Kafka).
- **Python 3.x** (optionnel, uniquement pour exécuter le simulateur GPS de test).

---

## ⚙️ Configuration

Le microservice est configuré via le fichier [`src/main/resources/application.properties`](file:///c:/Users/Public/Documents/Microservice-suivi-vehicule/service-suivi-vehicule/src/main/resources/application.properties).

Les principales variables d'environnement nécessaires pour son fonctionnement sont :

| Variable | Description | Valeur par défaut locale |
| :--- | :--- | :--- |
| `suivi_vehicule_DB_NAME` | Nom de la base de données PostgreSQL | `g7_vehicules` |
| `suivi_vehicule_DB_USER` | Utilisateur PostgreSQL | `postgres` |
| `suivi_vehicule_DB_PASSWORD` | Mot de passe PostgreSQL | `password` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Serveur de messagerie Kafka | `localhost:29093` |

---

## 🚀 Démarrage local (Développement)

### Étape 1 : Lancer les dépendances (PostgreSQL & Kafka)
Depuis la racine du projet, lancez uniquement la base de données dédiée à G7 ainsi que le broker Kafka :
```bash
docker compose up db-g7 kafka -d
```

### Étape 2 : Lancer le microservice G7
Positionnez-vous dans le dossier de ce microservice :
```bash
cd service-suivi-vehicule
```

Exécutez l'application en mode développement :
- **Sur Windows (PowerShell/Cmd)** :
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
- **Sur Linux/macOS** :
  ```bash
  chmod +x mvnw
  ./mvnw spring-boot:run
  ```

L'application démarre sur le port **`8087`**.

---

## 📦 Construction et Exécution avec Docker (Production)

Le dossier contient un [**Dockerfile**](file:///c:/Users/Public/Documents/Microservice-suivi-vehicule/service-suivi-vehicule/Dockerfile) multi-stage optimisé pour la production.

1. **Construire l'image Docker** :
   ```bash
   docker build -t service-suivi-vehicule:latest .
   ```

2. **Lancer le conteneur** :
   ```bash
   docker run -d -p 8087:8087 \
     -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5437/g7_vehicules \
     -e SPRING_DATASOURCE_USERNAME=postgres \
     -e SPRING_DATASOURCE_PASSWORD=password \
     -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:29093 \
     --name g7-service-container \
     service-suivi-vehicule:latest
   ```

---

## 🚗 Utilisation du Simulateur de Positions GPS

Pour tester la réception des données télémétriques et la détection d'anomalies en direct, un script de simulation Python est fourni dans le dossier `scripts/` :

1. **Installer la dépendance de requêtes HTTP** :
   ```bash
   pip install requests
   ```

2. **Exécuter la simulation** :
   ```bash
   python scripts/simulateur_g7.py
   ```

Le script va envoyer des coordonnées GPS en boucle (Casablanca) toutes les 3 secondes et simuler des alertes.

---

## 🧪 Points d'entrée (Endpoints) et Documentation

- **Swagger UI (Documentation interactive des APIs)** :
  [http://localhost:8087/swagger-ui/index.html](http://localhost:8087/swagger-ui/index.html)
- **Actuator Health (État de santé du service)** :
  [http://localhost:8087/actuator/health](http://localhost:8087/actuator/health)
- **Métriques Prometheus** :
  [http://localhost:8087/actuator/prometheus](http://localhost:8087/actuator/prometheus)

---

## 🧪 Exécution des Tests

Le microservice contient des tests unitaires et d'intégration (utilisant une base de données en mémoire H2 et un broker Kafka de test) :

```bash
# Sur Windows
.\mvnw.cmd test

# Sur Linux/macOS
./mvnw test
```

---

## 🔌 API Principale (Aperçu)

| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| **POST** | `/api/suivi-vehicules/positions` | Envoie une position GPS de capteur (déclenche les alertes). |
| **GET** | `/api/suivi-vehicules/positions` | Récupère l'historique complet de toutes les positions. |
| **GET** | `/api/suivi-vehicules/positions/{vehiculeId}` | Récupère la dernière position connue du véhicule. |
| **GET** | `/api/suivi-vehicules/positions/{vehiculeId}/historique` | Historique des positions du véhicule spécifié. |
| **GET** | `/api/suivi-vehicules/vehicules` | Liste tous les véhicules enregistrés en base. |
| **GET** | `/api/suivi-vehicules/alertes` | Liste les anomalies/alertes détectées par le système. |

