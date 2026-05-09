# Validation Contractuelle — Microservice G4

Projet: SGITU  
Service: G4 Coordination des Transports  
Statut global: Conforme (avec preconditions d'environnement Kafka/Gateway)

## 1) Contrat endpoints G4 (via G10)

| Domaine | Endpoint | Implémentation | Statut |
|---|---|---|---|
| Lignes | `POST /api/g4/lignes` | `LigneController` | Conforme |
| Lignes | `GET /api/g4/lignes` | `LigneController` | Conforme |
| Lignes | `GET /api/g4/lignes/actives` | `LigneController` | Conforme |
| Lignes | `GET /api/g4/lignes/{ligneId}` | `LigneController` | Conforme |
| Lignes | `PUT /api/g4/lignes/{ligneId}` | `LigneController` | Conforme |
| Lignes | `DELETE /api/g4/lignes/{ligneId}` | `LigneController` | Conforme |
| Lignes | `GET /api/g4/lignes/{ligneId}/trajets` | `LigneController` | Conforme |
| Trajets | `POST /api/g4/trajets` | `TrajetController` | Conforme |
| Trajets | `GET /api/g4/trajets` | `TrajetController` | Conforme |
| Trajets | `GET /api/g4/trajets/{trajetId}` | `TrajetController` | Conforme |
| Trajets | `GET /api/g4/trajets/{trajetId}/arrets` | `TrajetController` | Conforme |
| Trajets | `PUT /api/g4/trajets/{trajetId}` | `TrajetController` | Conforme |
| Trajets | `DELETE /api/g4/trajets/{trajetId}` | `TrajetController` | Conforme |
| Horaires | `POST /api/g4/horaires` | `HoraireController` | Conforme |
| Horaires | `GET /api/g4/horaires` | `HoraireController` | Conforme |
| Horaires | `GET /api/g4/horaires/{horaireId}` | `HoraireController` | Conforme |
| Horaires | `PUT /api/g4/horaires/{horaireId}` | `HoraireController` | Conforme |
| Horaires | `DELETE /api/g4/horaires/{horaireId}` | `HoraireController` | Conforme |
| Arrêts | `POST /api/g4/arrets` | `ArretController` | Conforme |
| Arrêts | `GET /api/g4/arrets` | `ArretController` | Conforme |
| Arrêts | `GET /api/g4/arrets/{arretId}` | `ArretController` | Conforme |
| Arrêts | `PUT /api/g4/arrets/{arretId}` | `ArretController` | Conforme |
| Arrêts | `DELETE /api/g4/arrets/{arretId}` | `ArretController` | Conforme |
| Arrêts | `GET /api/g4/arrets/ligne/{ligneId}` | `ArretController` | Conforme |
| Affectations | `POST /api/g4/affectations` | `AffectationController` | Conforme |
| Affectations | `GET /api/g4/affectations` | `AffectationController` | Conforme |
| Affectations | `GET /api/g4/affectations/{affectationId}` | `AffectationController` | Conforme |
| Affectations | `GET /api/g4/affectations/vehicule/{vehiculeId}` | `AffectationController` | Conforme |
| Affectations | `PUT /api/g4/affectations/{affectationId}` | `AffectationController` | Conforme |
| Affectations | `DELETE /api/g4/affectations/{affectationId}` | `AffectationController` | Conforme |
| Missions | `POST /api/g4/missions` | `MissionController` | Conforme |
| Missions | `GET /api/g4/missions` | `MissionController` | Conforme |
| Missions | `GET /api/g4/missions/actives` | `MissionController` | Conforme |
| Missions | `GET /api/g4/missions/{missionId}` | `MissionController` | Conforme |
| Missions | `GET /api/g4/missions/{missionId}/status` | `MissionController` | Conforme |
| Missions | `PUT /api/g4/missions/{missionId}` | `MissionController` | Conforme |
| Missions | `POST /api/g4/missions/{missionId}/cloturer` | `MissionController` | Conforme |
| Missions | `POST /api/g4/missions/{missionId}/annuler` | `MissionController` | Conforme |
| Events | `POST /api/g4/events` | `CoordinationEventController` | Conforme |
| Events | `GET /api/g4/events` | `CoordinationEventController` | Conforme |
| Events | `GET /api/g4/events/{eventId}` | `CoordinationEventController` | Conforme |
| Events | `GET /api/g4/events/type/{eventType}` | `CoordinationEventController` | Conforme |
| Events | `GET /api/g4/events/status/{status}` | `CoordinationEventController` | Conforme |
| Events | `POST /api/g4/events/detect-delay` | `CoordinationEventController` | Conforme |
| Events | `POST /api/g4/events/detect-deviation` | `CoordinationEventController` | Conforme |
| Events | `POST /api/g4/events/detect-breakdown` | `CoordinationEventController` | Conforme |
| Events | `POST /api/g4/events/detect-incident` | `CoordinationEventController` | Conforme |
| Events | `POST /api/g4/events/cancel-mission` | `CoordinationEventController` | Conforme |
| Notifications | `POST /api/notifications/send` | `NotificationController` | Conforme (`202 Accepted`) |
| Supervision | `GET /api/g4/health` | `G4SupervisionController` | Conforme |
| Supervision | `GET /api/v1/operator/status` | `OperatorStatusController` | Conforme |
| Supervision | `GET /api/g4/logs` | `G4SupervisionController` | Conforme |

## 1 bis) Périmètre conducteurs (G3 — gestion des utilisateurs)

Le **groupe 3** assure la **gestion des utilisateurs** (comptes, profils, rôles). C’est ce périmètre qui est la **source de vérité** pour l’identifiant conducteur côté SI : G4 reçoit typiquement cet id via l’UI ou les intégrations (souvent le même identifiant que celui exposé dans le JWT / annuaire utilisateurs). Dans l’API G4 le champ JSON s’appelle **`chauffeurId`** ; il correspond au **driver id** métier fourni par G3, sans duplication de la fiche utilisateur dans G4.

| Règle | Implémentation | Statut |
|---|---|---|
| Pas de CRUD conducteur dans G4 (fiches chauffeur = domaine G3) | Aucun contrôleur / service métier « chauffeur » | Conforme |
| Référence optionnelle `chauffeurId` sur mission | `MissionRequest` / `MissionResponse` + entité `Mission` | Conforme |
| Référence optionnelle `chauffeurId` sur affectation opérationnelle | `AffectationRequest` / `AffectationResponse` + entité `AffectationVehiculeLigne` | Conforme |
| G4 ne vérifie pas l’existence du conducteur chez G3 (ID opaque côté coordination) | Pas d’intégration client « annuaire conducteurs » dans ce service | Conforme |

## 2) Contrat G4 ↔ G5 (Notifications)

| Exigence | Implémentation | Statut |
|---|---|---|
| Endpoint unique `POST /api/notifications/send` | `NotificationController` | Conforme |
| Réponse `202 Accepted` | `ResponseEntity.status(HttpStatus.ACCEPTED)` | Conforme |
| Payload contractuel (`notificationId`, `sourceService`, `eventType`, `channel`, `recipient`, `metadata`) | `NotificationSendRequest` | Conforme |
| Appel via Gateway G10 | `G5NotificationClient` utilise `g10GatewayUrl` + `g5NotificationPath` | Conforme |
| JWT requis | `SecurityConfig` protège l'endpoint (non public) | Conforme |

## 3) Contrat G4 ↔ G3 (Billetterie)

| Exigence | Implémentation | Statut |
|---|---|---|
| Echange asynchrone Kafka | `G3BilletterieClient` (Kafka producer) | Conforme |
| Topic `missions-lifecycle` | `sgitu.kafka.topic-mission-lifecycle` | Conforme |
| Clé de partition `missionId` | key = `mission.getId()` | Conforme |
| Payload structuré (`notificationId`, `eventType`, `metadata.reason`, `missionDetails`, `metadata.variables`) | `G3MissionLifecycleMessage` | Conforme |
| `missionDetails` : `missionId`, `status`, `horaire`, `trajet` | `MissionService.publishG3Lifecycle` | Conforme |
| `metadata.variables` : uniquement `vehiculeId` (pas `chauffeurId`) | `Map.of("vehiculeId", mission.getVehiculeId())` | Conforme |
| Publication sur transitions mission | `MissionService` (create/update/cloturer/annuler) | Conforme |

## 4) Contrat G4 ↔ G7 (Suivi)

| Exigence | Implémentation | Statut |
|---|---|---|
| `GET /api/v1/lignes` | `G7ReferenceController` | Conforme |
| `GET /api/v1/lignes/{id}/trajet` | `G7ReferenceController` | Conforme |
| `GET /api/v1/lignes/{id}/horaires` | `G7ReferenceController` + `HoraireService.findByLigne` | Conforme |
| `GET /api/v1/arrets` | `G7ReferenceController` | Conforme |
| `GET /api/v1/arrets/{id}` | `G7ReferenceController` | Conforme |
| Kafka topic `vehicule-positions` (G7 -> G4) | `G7VehiclePositionKafkaConsumer` | Conforme |

## 5) Contrat G9 -> G4 (Incidents)

| Exigence | Implémentation | Statut |
|---|---|---|
| Kafka topic `incident.transport.topic` | `G9IncidentKafkaConsumer` | Conforme |
| Mapping du payload JSON G9 | `G9IncidentKafkaMessage` + mapping vers `CoordinationEventRequest` | Conforme |
| Mapping statuts/types vers événements G4 | `mapType` / `mapStatus` | Conforme |

## 6) Paramètres de configuration a fournir en environnement

Pour une conformité runtime complete en integration:

- `SGITU_KAFKA_ENABLED=true`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS=<broker:9092>`
- `SGITU_G10_URL=<url_gateway>`
- `SGITU_G9_INCIDENT_TOPIC=incident.transport.topic` (optionnel, valeur par défaut deja conforme)
- `SGITU_G3_MISSION_TOPIC=missions-lifecycle` (optionnel, valeur par défaut deja conforme)
- `SGITU_G7_POSITIONS_TOPIC=vehicule-positions` (optionnel, valeur par défaut deja conforme)

## 7) Verification technique locale

- Build/tests Maven: `mvn test` -> OK
- Base PostgreSQL: connexion valide sur environnement Docker du projet
- Endpoint de supervision disponible: `/api/g4/health`

