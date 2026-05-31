# Remarques prof (11 mai 2026) — statut d’implémentation G4

## A. Validé par la prof

| Point | Statut |
|-------|--------|
| Stade satisfaisant | OK — à rappeler en soutenance |
| Jonction lignes / services (G1, G3, G5, G7, G9, G10) | Diagramme `docs/diagrams/architecture-externe-g3-g7.puml` |
| PostgreSQL | OK — inchangé |

## B. Les 10 remarques

| # | Sujet | Statut code / doc |
|---|--------|-------------------|
| 1 | Codes HTTP + **409** véhicule en mission | **Fait** — `ConflictException`, `MissionService`, Swagger `MissionController` |
| 2 | Exemples JSON | **Fait** — `postman/examples/`, exemples Swagger detect-delay/deviation/mission |
| 3 | Retards / déviations sans bloquer mission | **Déjà OK** — événements seulement |
| 4 | Résilience Resilience4j | **Évolution** — phrase rapport |
| 5 | Rôles G3 / G10 / G4 | **Fait** — `docs/ROLES_G3_G4_ALIGNMENT.md`, `SecurityConfig` |
| 6 | `/health` + `/logs` sans token | **Fait** — `SecurityConfig` permitAll sur `/api/g4/logs` |
| 7 | Docker Kafka | **Fait** — `apache/kafka:3.7.0`, `KAFKA_ADVERTISED_LISTENERS: kafka:9092` |
| 8 | Diagramme Mission ↔ événements | **Fait** — `docs/diagrams/mission-coordination-events.puml` |
| 9 | Flèche G4 → G3 | **Fait** — `G3UserClient` (optionnel) + diagramme + `README.md` |
| 10 | Positions G7 Kafka | **Déjà OK** — `G7VehiclePositionKafkaConsumer`, topic `vehicule-positions` |

## C. Notes rapport (image « en cours / perspectives »)

### En cours / à consolider

- Tests **inter-groupes** en environnement partagé (réseau Docker `sgitu-network`, Kafka commun).
- Harmonisation finale **authentification** Users (G3) / Gateway (G10) / JWT sur tous les MS.
- Enrichissement **monitoring** : `/health`, `/logs` publics + scénarios de charge.

### Perspectives

- Déploiement **Kubernetes**
- Pipeline **CI/CD**

## D. Synthèse rapport (copier-coller)

Suite aux remarques de la séance du 11 mai 2026, nous avons aligné les codes HTTP OpenAPI sur le comportement API (conflit véhicule/mission en 409), enrichi les exemples JSON, conservé les retards et déviations comme événements sans blocage des missions, synchronisé les rôles avec G3/G10, ouvert les endpoints de monitoring sans authentification, corrigé Docker/Kafka (`apache/kafka:3.7.0`, `kafka:9092`), complété les diagrammes Mission–événements et G4→G3, et documenté la consommation `vehicule-positions` (G7). La résilience (Resilience4j) et la validation live conducteur G3 restent des évolutions ; les tests inter-groupes, l’auth harmonisée et le monitoring avancé sont en consolidation, avec perspectives Kubernetes et CI/CD.
