# Contrats de Données — Événements entrants G8 Analytics

**Périmètre :** Ce document liste les formats de données JSON que le service analytique G8 accepte de chaque groupe. Il sert de référence de contrat pour les intégrations REST et Kafka.

> **Règle universelle :** Chaque événement, quelle que soit sa source, **doit** contenir `"schemaVersion": 1` et un champ `"timestamp"` au format ISO 8601 (`yyyy-MM-ddTHH:mm:ssZ`). Tout événement manquant ces champs sera rejeté avec `REJECTED`.

---

## Sommaire

- [G1 — Utilisateurs](#g1--utilisateurs)
- [G2 — Billetterie](#g2--billetterie)
- [G3 — Abonnements](#g3--abonnements)
- [G4 — Paiements](#g4--paiements)
- [G6 — Véhicules](#g6--véhicules)
- [G7 — Incidents](#g7--incidents)
- [Règles de validation communes](#règles-de-validation-communes)
- [Format de réponse API](#format-de-réponse-api)

---

## G1 — Utilisateurs

**REST endpoint :** `POST /api/v1/ingestion/users`  
**Kafka topic :** `g1-user-events`

### Champs requis
| Champ | Type | Valeurs acceptées |
|---|---|---|
| `schemaVersion` | `integer` | `1` |
| `timestamp` | `string` (ISO 8601) | ex. `"2026-05-30T08:00:00Z"` |
| `userId` | `string` | identifiant unique utilisateur |
| `action` | `string` | `"active"` ou `"inactive"` |

### Champs optionnels
| Champ | Type | Description |
|---|---|---|
| `deviceOS` | `string` | ex. `"iOS"`, `"Android"` |
| `sessionDuration` | `number` | durée de session en secondes |

### Exemple de payload
```json
[
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T08:00:00Z",
    "userId": "USR-201",
    "action": "active",
    "deviceOS": "iOS"
  },
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T09:15:00Z",
    "userId": "USR-202",
    "action": "active",
    "deviceOS": "Android",
    "sessionDuration": 1240
  },
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T10:00:00Z",
    "userId": "USR-203",
    "action": "inactive"
  }
]
```

### Impact sur les agrégations
- `USR_01` — Utilisateurs actifs journaliers (DAU)
- `USR_02` — Distribution des plateformes (iOS vs Android)
- `USR_03` — Ratio actifs / inactifs
- `USR_04` — Tendance DAU hebdomadaire

---

## G2 — Billetterie

**REST endpoint :** `POST /api/v1/ingestion/tickets`  
**Kafka topic :** `g2-ticketing-events`

### Champs requis
| Champ | Type | Valeurs acceptées |
|---|---|---|
| `schemaVersion` | `integer` | `1` |
| `timestamp` | `string` (ISO 8601) | ex. `"2026-05-30T08:00:00Z"` |
| `userId` | `string` | identifiant de l'utilisateur |
| `status` | `string` | `"validated"` ou `"expired"` |

### Champs optionnels
| Champ | Type | Description |
|---|---|---|
| `ticketId` | `string` | identifiant du billet |
| `line` | `string` | ligne de transport (ex. `"L1"`, `"L2"`) |
| `stationId` | `string` | identifiant de la station (ex. `"ST-05"`) |
| `scanType` | `string` | mode de scan : `"NFC"`, `"QR"`, `"CARD"` |

### Exemple de payload
```json
[
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T08:00:00Z",
    "userId": "USR-001",
    "status": "validated",
    "line": "L1",
    "stationId": "ST-05",
    "ticketId": "TCK-001",
    "scanType": "NFC"
  },
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T17:30:00Z",
    "userId": "USR-002",
    "status": "validated",
    "line": "L2",
    "stationId": "ST-10",
    "ticketId": "TCK-002",
    "scanType": "QR"
  },
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T09:00:00Z",
    "userId": "USR-003",
    "status": "expired",
    "line": "L2",
    "stationId": "ST-10",
    "ticketId": "TCK-003"
  }
]
```

### Impact sur les agrégations
- `FREQ_01` — Total des validations
- `FREQ_02` — Distribution par heure de pointe
- `FREQ_03` — Heures de pointe identifiées
- `FREQ_04` — Passagers journaliers moyens
- `FREQ_05` — Classement d'utilisation des lignes
- `FREQ_06` — Affluence par station
- `FREQ_07` — Ratio week-end / semaine
- `PRED_01` — Score ML heure de pointe

---

## G3 — Abonnements

**REST endpoint :** `POST /api/v1/ingestion/subscriptions`  
**Kafka topic :** `g3-subscription-events`

### Champs requis
| Champ | Type | Valeurs acceptées |
|---|---|---|
| `schemaVersion` | `integer` | `1` |
| `timestamp` | `string` (ISO 8601) | ex. `"2026-05-30T09:00:00Z"` |
| `userId` | `string` | identifiant de l'utilisateur |
| `action` | `string` | `"created"`, `"renewed"` ou `"cancelled"` |

### Champs optionnels
| Champ | Type | Description |
|---|---|---|
| `planType` | `string` | ex. `"MONTHLY_STUDENT"`, `"YEARLY_STANDARD"`, `"MONTHLY_STANDARD"` |
| `reason` | `string` | motif d'annulation (ex. `"PAYMENT_FAILED"`, `"USER_REQUEST"`) |
| `subscriptionId` | `string` | identifiant de l'abonnement |

### Exemple de payload
```json
[
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T09:00:00Z",
    "userId": "USR-101",
    "action": "created",
    "planType": "MONTHLY_STUDENT",
    "subscriptionId": "SUB-001"
  },
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T09:30:00Z",
    "userId": "USR-102",
    "action": "renewed",
    "planType": "YEARLY_STANDARD",
    "subscriptionId": "SUB-002"
  },
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T11:00:00Z",
    "userId": "USR-103",
    "action": "cancelled",
    "planType": "MONTHLY_STANDARD",
    "reason": "PAYMENT_FAILED",
    "subscriptionId": "SUB-003"
  }
]
```

### Impact sur les agrégations
- `SUB_01` — Abonnements actifs
- `SUB_02` — Nouveaux abonnements du jour
- `SUB_03` — Taux de renouvellement
- `SUB_04` — Taux de churn (annulations)
- `SUB_05` — Répartition par type d'abonnement

---

## G4 — Paiements

**REST endpoint :** `POST /api/v1/ingestion/payments`  
**Kafka topic :** `g4-payment-events`

### Champs requis
| Champ | Type | Valeurs acceptées |
|---|---|---|
| `schemaVersion` | `integer` | `1` |
| `timestamp` | `string` (ISO 8601) | ex. `"2026-05-30T08:00:00Z"` |
| `transactionId` | `string` | identifiant unique de transaction |
| `amount` | `number` | montant en dirhams (≥ 0) |
| `status` | `string` | `"completed"` ou `"failed"` |

### Champs optionnels
| Champ | Type | Description |
|---|---|---|
| `line` | `string` | ligne associée à la transaction |
| `method` | `string` | mode de paiement : `"CARD"`, `"CASH"`, `"MOBILE"` |
| `paymentMethod` | `string` | alias de `method` (les deux sont acceptés) |

### Exemple de payload
```json
[
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T08:00:00Z",
    "transactionId": "TXN-001",
    "status": "completed",
    "amount": 25.50,
    "method": "CARD",
    "line": "L1"
  },
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T10:00:00Z",
    "transactionId": "TXN-002",
    "status": "completed",
    "amount": 50.00,
    "method": "CASH"
  },
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T14:00:00Z",
    "transactionId": "TXN-003",
    "status": "failed",
    "amount": 25.50,
    "method": "CARD",
    "line": "L1"
  }
]
```

### Impact sur les agrégations
- `REV_01` — Revenu total
- `REV_02` — Revenu par type de billet
- `REV_03` — Revenu moyen par passager
- `REV_04` — Répartition des modes de paiement
- `REV_05` — Tendance des revenus

---

## G6 — Véhicules

**REST endpoint :** `POST /api/v1/ingestion/vehicles`  
**Kafka topic :** `g6-vehicle-events`

### Champs requis
| Champ | Type | Valeurs acceptées |
|---|---|---|
| `schemaVersion` | `integer` | `1` |
| `timestamp` | `string` (ISO 8601) | ex. `"2026-05-30T08:00:00Z"` |
| `vehicleId` | `string` | identifiant unique du véhicule |
| `status` | `string` | `"in_service"` ou `"out_of_service"` |
| `line` | `string` | ligne sur laquelle circule le véhicule |

### Champs optionnels
| Champ | Type | Contrainte | Description |
|---|---|---|---|
| `speed` | `number` | ≥ 0 | vitesse en km/h |
| `occupancy` | `integer` | ≥ 0 | nombre de passagers à bord |
| `delayMinutes` | `number` | ≥ 0 | retard en minutes (0 = à l'heure) |
| `alertCode` | `string` | — | code d'alerte véhicule (ex. `"ENG_TEMP_HIGH"`) |

### Exemple de payload
```json
[
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T08:00:00Z",
    "vehicleId": "BUS-101",
    "status": "in_service",
    "line": "L1",
    "speed": 42.5,
    "occupancy": 80,
    "delayMinutes": 0
  },
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T08:05:00Z",
    "vehicleId": "BUS-202",
    "status": "in_service",
    "line": "L2",
    "speed": 35.0,
    "occupancy": 120,
    "delayMinutes": 8
  },
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T09:00:00Z",
    "vehicleId": "TRAM-01",
    "status": "out_of_service",
    "line": "T1",
    "speed": 0.0,
    "alertCode": "ENG_TEMP_HIGH"
  }
]
```

### Impact sur les agrégations
- `VEH_01` — Nombre de véhicules actifs
- `VEH_02` — Taux de ponctualité moyen (% sans retard)
- `VEH_03` — Distribution des retards
- `VEH_04` — Taux d'utilisation de la flotte
- `VEH_05` — Vitesse moyenne par ligne

---

## G7 — Incidents

**REST endpoint :** `POST /api/v1/ingestion/incidents`  
**Kafka topic :** `g7-incident-events`

### Champs requis
| Champ | Type | Valeurs acceptées |
|---|---|---|
| `schemaVersion` | `integer` | `1` |
| `timestamp` | `string` (ISO 8601) | ex. `"2026-05-30T10:00:00Z"` |
| `incidentId` | `string` | identifiant unique de l'incident |
| `type` | `string` | `"delay"`, `"breakdown"` ou `"accident"` |
| `severity` | `string` | `"LOW"`, `"MEDIUM"`, `"HIGH"` ou `"CRITICAL"` |
| `latitude` | `number` | coordonnée GPS (entre -90 et 90) |
| `longitude` | `number` | coordonnée GPS (entre -180 et 180) |

### Champs optionnels
| Champ | Type | Contrainte | Description |
|---|---|---|---|
| `line` | `string` | — | ligne concernée par l'incident |
| `description` | `string` | — | description libre de l'incident |
| `resolutionMinutes` | `number` | ≥ 0 | temps de résolution en minutes |

> **Note GPS :** Les coordonnées sont arrondies à 2 décimales pour créer des zones géographiques. Par exemple, `latitude: 33.5731` et `longitude: -7.5898` donnent la zone `"33.57,-7.59"`. Deux incidents dans la même zone font de cette zone une **zone à incidents répétés** (`INC_05`).

### Exemple de payload
```json
[
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T10:00:00Z",
    "incidentId": "INC-001",
    "type": "accident",
    "severity": "HIGH",
    "latitude": 33.5731,
    "longitude": -7.5898,
    "line": "L2",
    "description": "Collision de véhicules près de la gare principale"
  },
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T11:00:00Z",
    "incidentId": "INC-002",
    "type": "breakdown",
    "severity": "CRITICAL",
    "latitude": 33.5800,
    "longitude": -7.6000,
    "line": "L1",
    "description": "Panne moteur bus 404",
    "resolutionMinutes": 30
  },
  {
    "schemaVersion": 1,
    "timestamp": "2026-05-30T14:00:00Z",
    "incidentId": "INC-003",
    "type": "delay",
    "severity": "MEDIUM",
    "latitude": 33.5731,
    "longitude": -7.5898,
    "line": "L2",
    "resolutionMinutes": 15
  }
]
```

> INC-001 et INC-003 partagent la zone `"33.57,-7.59"` → cette zone apparaît dans `INC_05` (zones à incidents répétés).

### Impact sur les agrégations
- `INC_01` — Total incidents (7 derniers jours)
- `INC_02` — Incidents par type
- `INC_03` — Incidents par zone GPS
- `INC_04` — Temps moyen de résolution
- `INC_05` — Zones à incidents répétés (≥ 2 incidents ce mois)
- `PRED_02` — Score ML de risque incident par zone

---

## Règles de validation communes

Les règles suivantes s'appliquent à **toutes les sources** :

| Règle | Détail |
|---|---|
| `schemaVersion` obligatoire | Doit être `1`. Tout autre valeur → `REJECTED`. |
| `timestamp` obligatoire | Format ISO 8601 avec fuseau horaire (`Z` ou `+HH:MM`). |
| `timestamp` non futur | Ne peut pas être à plus de 5 minutes dans le futur. |
| Taille de batch | Maximum **1 000 événements** par requête. |
| Champs numériques | `amount`, `speed`, `delayMinutes`, `resolutionMinutes` doivent être ≥ 0. |
| Coordonnées GPS | Si `latitude` ou `longitude` est présent, **les deux** sont requis. `latitude` ∈ [-90, 90], `longitude` ∈ [-180, 180]. |

---

## Format de réponse API

Toutes les réponses d'ingestion retournent un objet `BatchIngestionResponse` :

```json
{
  "totalReceived": 3,
  "totalAccepted": 2,
  "totalRejected": 1,
  "status": "PARTIAL",
  "rejectedReasons": [
    "Event at index 2: Missing required field: timestamp"
  ]
}
```

| Statut HTTP | `status` JSON | Signification |
|---|---|---|
| `201 Created` | `"SUCCESS"` | Tous les événements acceptés |
| `207 Multi-Status` | `"PARTIAL"` | Certains événements rejetés |
| `400 Bad Request` | `"REJECTED"` | Tous les événements rejetés |
| `503 Service Unavailable` | — | Erreur MongoDB |

---

## Endpoints pour le Groupe Gateway (G10)

Le service G8 Analytics expose les endpoints suivants pour l'ingestion des événements. Le Gateway doit router les requêtes vers **`http://g8-analytics-service:8088`** (port interne Docker) ou **`localhost:8088`** (si en développement local).

### Endpoints d'ingestion REST

| Service | Endpoint | Méthode | Port | Description |
|---------|----------|--------|------|-------------|
| **G1** (Utilisateurs) | `POST /api/v1/ingestion/users` | `POST` | `8088` | Ingérer événements utilisateurs (activité) |
| **G2** (Billetterie) | `POST /api/v1/ingestion/tickets` | `POST` | `8088` | Ingérer validations de billets |
| **G3** (Abonnements) | `POST /api/v1/ingestion/subscriptions` | `POST` | `8088` | Ingérer événements d'abonnement |
| **G4** (Paiements) | `POST /api/v1/ingestion/payments` | `POST` | `8088` | Ingérer transactions de paiement |
| **G6** (Véhicules) | `POST /api/v1/ingestion/vehicles` | `POST` | `8088` | Ingérer événements de flotte |
| **G7** (Incidents) | `POST /api/v1/ingestion/incidents` | `POST` | `8088` | Ingérer incidents routiers |

### Health Check & Monitoring

| Endpoint | Méthode | Description | Réponse | Code |
|----------|---------|-------------|---------|------|
| `/health` | `GET` | État du service | `{"status":"UP"}` | `200` |
| `/swagger-ui.html` | `GET` | Documentation API interactif | Page HTML | `200` |

### Exemples de routing (côté Gateway)

```yaml
# Gateway routing rules for G8
routes:
  - path: /api/v1/ingestion/users
    service: g8-analytics-service
    port: 8088
    method: POST
    
  - path: /api/v1/ingestion/tickets
    service: g8-analytics-service
    port: 8088
    method: POST
    
  - path: /api/v1/ingestion/subscriptions
    service: g8-analytics-service
    port: 8088
    method: POST
    
  - path: /api/v1/ingestion/payments
    service: g8-analytics-service
    port: 8088
    method: POST
    
  - path: /api/v1/ingestion/vehicles
    service: g8-analytics-service
    port: 8088
    method: POST
    
  - path: /api/v1/ingestion/incidents
    service: g8-analytics-service
    port: 8088
    method: POST
```

### Notes pour Gateway

- **Authentification :** Les endpoints acceptent actuellement les requêtes sans JWT. À aligner avec la stratégie d'authentification globale du projet.
- **Rate limiting :** Pas de rate limit côté G8; configurer au niveau Gateway si nécessaire.
- **Batch size :** Maximum **1 000 événements** par requête. Le Gateway doit fragmenter les payloads > 1 000.
- **Timeouts :** G8 peut traiter ~100–500 événements/s selon la charge MongoDB. Prévoir timeout ≥ 10s.
- **CORS :** Non configuré; activer si clients front-end appellent directement G8.
