# Modèle G4 — Incident G9 vs événement de coordination

## Deux agrégats distincts

| Concept | Table G4 | Sources |
|---------|----------|---------|
| **Événement de coordination** | `coordination_events` | API G4, détection **G7** (`vehicule-positions`) |
| **Impact incident G9** | `mission_incident_impacts` | Kafka **G9** (`incident.transport.topic`), API `POST /api/g4/incident-impacts` |

L'entité **Incident** complète reste dans le microservice **G9**. G4 stocke uniquement `g9_reference_incident` + lien `mission_id`.

## API

- Événements : `/api/g4/events/**` (plus de `detect-incident` ici)
- Incidents G9 : `/api/g4/incident-impacts`

## Enum `CoordinationEventType`

`RETARD`, `DEVIATION`, `PANNE`, `ANNULATION_MISSION` — **sans** `INCIDENT`.
