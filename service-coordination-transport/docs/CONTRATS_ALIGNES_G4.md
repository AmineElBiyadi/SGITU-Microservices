# Alignement des contrats inter-groupes — G4

Document de référence pour éviter les divergences **ports**, **topics Kafka** et **JSON** entre producteurs et consommateurs.

## Ports SGITU (référence classe)

| Groupe | Service | Port host | Port Docker (service) |
|--------|---------|-----------|------------------------|
| G10 | API Gateway | 8080 | 8080 |
| G1 | Billetterie | 8081 | 8081 |
| G3 | Utilisateurs | 8083 | 8083 |
| **G4** | **Coordination** | **8084** | **8084** |
| G5 | Notifications | 8085 | 8085 |
| G7 | Suivi véhicules | 8087 | 8087 |
| G9 | Incidents | 8089 | 8089 |
| Kafka | Broker | 9092 | **kafka:9092** (réseau Docker) |
| PostgreSQL G4 | | 5434 (host) | postgres:5432 |

Réseau Docker commun : **`sgitu-network`**.

## Topics Kafka — contrat G4

| Topic | Producteur | Consommateur G4 | Fichier exemple JSON |
|-------|------------|-----------------|----------------------|
| `vehicle.registered` | **G7** | `G7VehicleRegisteredKafkaConsumer` | `postman/examples/kafka-g7-vehicle-registered.json` |
| `vehicule-positions` | **G7** | `G7VehiclePositionKafkaConsumer` | `postman/examples/kafka-g7-position.json` |
| `incident.transport.topic` | **G9** | `G9IncidentKafkaConsumer` | `postman/examples/kafka-g9-incident.json` |
| `missions-lifecycle` | **G4** | **G1** (billetterie) | `postman/examples/g1-mission-lifecycle.json` |
| `sgitu.g4.coordination.events` | **G4** | autres (supervision) | — |

Variables d'environnement : voir `.env.example`.

## Flow véhicule G7 ↔ G4 (affectation + mission)

1. G7 `POST /api/suivi-vehicules/vehicules` → statut `DISPONIBLE`, sans ligne.
2. G7 publie Kafka **`vehicle.registered`** → G4 table `vehicules_referentiel`.
3. G4 `POST /api/g4/affectations` (véhicule + ligne, statut `ACTIF`) → G4 `PUT` G7 `.../vehicules/{id}/statut?statut=EN_SERVICE`.
4. G4 `POST /api/g4/missions` avec le même `vehiculeId` (UUID) et une affectation `ACTIF` sur la ligne.
5. G7 publie **`vehicule-positions`** pendant la mission.

Secours sans Kafka : `POST /api/g4/vehicules/sync-from-g7/{vehiculeId}`.

## JSON entrant G7 → G4 (`vehicle.registered`)

Champs **obligatoires** : `vehiculeId`.

```json
{
  "vehiculeId": "53c31262-591a-44d4-8872-51e84611ac5e",
  "immatriculation": "BUS-G4-001",
  "type": "BUS",
  "statut": "DISPONIBLE",
  "timestamp": "2026-05-20T10:00:00Z"
}
```

## JSON entrant G7 → G4 (`vehicule-positions`)

Champs **obligatoires** : `vehiculeId`, `lat`, `long` (ou `longitude`).

```json
{
  "vehiculeId": "00000000-0000-4000-8000-000000000001",
  "ligneId": "L12",
  "lat": 35.578,
  "long": -5.368,
  "vitesse": 42.5,
  "timestamp": "2026-05-20T14:30:00Z"
}
```

Validation : `KafkaContractValidator.validateG7Position`.

## JSON entrant G9 → G4

Champs **obligatoires** : `referenceIncident`.

```json
{
  "referenceIncident": "INC-2026-042",
  "type": "PANNE_VEHICULE",
  "statut": "NOUVEAU",
  "vehiculeId": "00000000-0000-4000-8000-000000000001",
  "ligneId": "L12",
  "description": "Panne moteur",
  "latitude": 35.57,
  "longitude": -5.36,
  "timestamp": "2026-05-20T14:35:00"
}
```

Stockage G4 : table **`mission_incident_impacts`** (pas `coordination_events`).

## JSON sortant G4 → G1 (missions-lifecycle)

```json
{
  "notificationId": "g4-mission-1-MISSION_PLANIFIED",
  "eventType": "MISSION_PLANIFIED",
  "metadata": {
    "reason": "DEBUT_MISSION",
    "missionDetails": {
      "missionId": "M-1",
      "status": "EN_COURS",
      "horaire": { "depart": "2026-05-20T08:00:00Z" },
      "trajet": { "ligneId": "1" }
    },
    "variables": { "vehiculeId": "00000000-0000-4000-8000-000000000001" }
  }
}
```

## HTTP G4 → G7 (après affectation)

```http
PUT {G7}/api/suivi-vehicules/vehicules/{vehiculeId}/statut?statut=EN_SERVICE
```

## HTTP G3 (validation conducteur)

```http
GET {G3}/api/users/drivers/ids
Authorization: Bearer <JWT>
→ [1, 5, 42]
```

## HTTP G5 via G10

```http
POST {G10}/api/notifications/send
```

Si G5 down → G4 répond **`DEGRADED`** (pas de 500).

## Checklist alignement avant soutenance

- [ ] Même `jwt.secret` G3 / G10 / G4
- [ ] Topics identiques dans `.env` des groupes voisins
- [ ] Test Postman ou Kafka avec exemples JSON ci-dessus
- [ ] Capture validation croisée dans le rapport
