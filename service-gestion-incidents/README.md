# 🚨 Service Gestion des Incidents (SGITU)

Ce microservice est chargé de l'orchestration, du suivi et de la résolution des incidents au sein du réseau SGITU.

## 🚀 Routes de l'API (Endpoints)

| Méthode | Route | Description | Rôles Autorisés |
| :--- | :--- | :--- | :--- |
| **POST** | `/signaler` | Signaler un nouvel incident | `VOYAGEUR`, `CONDUCTEUR`, `SUPERVISEUR_INCIDENTS` |
| **GET** | `/{id}` | Consulter le détail d'un incident | TOUS |
| **GET** | `/{id}/suivi` | Consulter l'historique d'un incident | `AGENT_INCIDENTS`, `SUPERVISEUR_INCIDENTS` |
| **GET** | `/` | Filtrer les incidents | `AGENT_INCIDENTS`, `SUPERVISEUR_INCIDENTS` |
| **PUT** | `/{id}/statut` | Mettre à jour le statut (ex: `EN_TRAITEMENT`, `RESOLU`) | `AGENT_INCIDENTS`, `SUPERVISEUR_INCIDENTS` |
| **PUT** | `/{id}/cloturer` | Clôturer un incident résolu | `SUPERVISEUR_INCIDENTS` |
| **PUT** | `/{id}/escalader` | Déclencher une escalade d'urgence | `SUPERVISEUR_INCIDENTS` |
| **PUT** | `/{id}/affecter` | Affecter un responsable technique | `SUPERVISEUR_INCIDENTS` |

## 📊 Cycle de Vie (Statuts)
Les statuts autorisés sont :
`NOUVEAU` ➔ `ANALYSE` ➔ `ASSIGNE` ➔ `EN_TRAITEMENT` ➔ `RESOLU` ➔ `CLOTURE`
(Options : `ESCALADE`, `ANNULE`)

## 🛠️ Configuration Technique
- **Port** : 8089
- **Base de données** : MySQL (`sgitu_incidents_db`)
- **Kafka Topics** :
  - `suivi-vehicule.incident.topic` (Consommateur)
  - `incident.notification.topic` (Producteur)
  - `incident.transport.topic` (Producteur)
  - `incident.analytique.topic` (Producteur)
