# G4 — Docker, Prometheus, Kafka (vérification stack)

## Deux modes de déploiement

| Mode | Fichier | Contenu |
|------|---------|---------|
| **G4 seul** | `service-coordination-transport/docker-compose.yml` | G4 + Postgres + Kafka (+ profil `monitoring`) |
| **Monorepo** | `SGITU-Microservices/docker-compose.yml` | Bloc `g4-coordination` + `g4-postgres` + Kafka + G7… |

## URLs (host)

| Service | URL |
|---------|-----|
| G4 API | http://localhost:8084 |
| Health | http://localhost:8084/api/g4/health |
| Prometheus (G4 local) | http://localhost:9090/targets — profil `monitoring` |
| Prometheus (racine) | http://localhost:9090/targets |
| Grafana (G4 local) | http://localhost:3000 — admin / admin |

## Dockerfile G4

- Build multi-stage Java 17, user non-root `g4`, port **8084** — OK.

## Kafka — topics G4 (à aligner avec G7 / G9)

| Variable | Valeur par défaut |
|----------|-------------------|
| `SGITU_G7_VEHICLE_REGISTERED_TOPIC` | `vehicle.registered` |
| `SGITU_G7_POSITIONS_TOPIC` | `vehicule-positions` |
| `SGITU_G9_INCIDENT_TOPIC` | `incident.transport.topic` |
| `SGITU_G1_MISSION_TOPIC` | `missions-lifecycle` |

Bootstrap Docker : `kafka:9092` (réseau `sgitu-network`).

## Intégration REST (hostname Docker monorepo)

| Groupe | URL dans compose G4 |
|--------|---------------------|
| G3 | http://g3-user-service:8083 |
| G5 | http://notification-service:8085 |
| G7 | http://g7-service:8087 |
| G10 | http://api-gateway:8080 |

## Prometheus

- **Racine** : `prometheus.yml` → job `g4-coordination` → `g4-coordination:8084/actuator/prometheus`
- **G4 local** : `monitoring/prometheus.yml` — même cible

Hostname du conteneur G4 : **`g4-coordination`** (important pour le scrape).

## Commandes

```bash
# Stack G4 seule
cd service-coordination-transport
docker compose up -d --build
docker compose --profile monitoring up -d

# Monorepo (G4 + G7 + Kafka…)
cd SGITU-Microservices
docker compose up -d --build g4-coordination g4-postgres g7-service kafka
```

## Checklist rapide

- [ ] `GET /api/g4/health` → UP, Kafka enabled
- [ ] Prometheus target `g4-coordination` → **UP**
- [ ] G7 crée véhicule → topic `vehicle.registered` ou `POST /api/g4/vehicules/sync-from-g7/{uuid}`
- [ ] `GET /api/g4/vehicules/disponibles` non vide après enregistrement
