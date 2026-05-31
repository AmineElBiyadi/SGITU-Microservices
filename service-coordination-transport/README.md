# G4 — Coordination des transports (SGITU)

Microservice **G4** : gestion du réseau (lignes, trajets, arrêts, horaires), de la flotte (missions, affectations) et des événements de coordination (retard, déviation, incident).

## Prérequis

- **Java 17+**
- **Maven 3.9+** (ou `./mvnw`)
- **Docker** + **Docker Compose** (recommandé)

## Démarrage rapide (Docker)

**G4 seul (développement) :**
```bash
docker compose up -d --build
```

**G4 + Prometheus + Grafana (monitoring — sans G5, autre groupe) :**
```bash
docker compose --profile monitoring up -d --build
# ou : .\scripts\start-g4-monitoring.ps1
```

**Guide complet :** `docs/JOUR_INTEGRATION_ET_LIVRAISON_G4.md`

**Simulation Kubernetes (bonus — même image Docker, jour d'intégration = Compose) :**
```powershell
.\scripts\deploy-g4-k8s.ps1
```
Voir `docs/KUBERNETES_G4.md` et manifestes `k8s/`.

**CI/CD (GitHub Actions)** — tests auto + image Docker + déploiement smoke :  
voir `docs/CI_CD_G4.md` (workflows `.github/workflows/g4-ci.yml`, `g4-cd.yml` à la racine du monorepo).

| Service | URL |
|---------|-----|
| API G4 | http://localhost:8084 |
| Swagger | http://localhost:8084/swagger-ui.html |
| Health (sans token) | http://localhost:8084/api/g4/health |
| Logs supervision (sans token) | http://localhost:8084/api/g4/logs |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin) |
| PostgreSQL | localhost:5434 |
| Kafka | localhost:9092 |

## Démarrage local (sans Docker)

1. PostgreSQL sur `localhost:5432`, base `sgitu_g4`, user/password `g4`.
2. Kafka optionnel : `SGITU_KAFKA_ENABLED=true` et `SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092`.
3. Lancer :

```bash
./mvnw spring-boot:run
```

## Tests

```bash
./mvnw test
```

## Authentification (démo)

`POST /api/auth/login` avec JSON :

```json
{ "username": "gestionnaire.flotte", "password": "password" }
```

| Compte | Rôle G3 | Usage |
|--------|---------|--------|
| `gestionnaire.reseau` | G4_OPERATOR (`ROLE_G4_OPERATOR`) | Lignes, trajets, arrêts, horaires |
| `gestionnaire.flotte` | DISPATCHER (`ROLE_DISPATCHER`) | Missions, affectations, événements |
| `admin.technique` | G4_ADMIN (`ROLE_G4_ADMIN`) | Supervision + tous droits |

En-tête des requêtes protégées : `Authorization: Bearer <token>`.

## Postman

Importer : `postman/SGITU-G4-Coordination-Transport.postman_collection.json`  
Exemples JSON : `postman/examples/`

## Intégrations

| Groupe | Mode | Topic / API |
|--------|------|-------------|
| G7 | Kafka consumer | `vehicule-positions` → événements coordination (ex. DEVIATION) |
| G9 | Kafka consumer | `incident.transport.topic` → table `mission_incident_impacts` |
| G4 | REST | `POST /api/g4/incident-impacts` — lien mission ↔ référence incident G9 |
| G1 | Kafka producer | `missions-lifecycle` |
| G5 | HTTP | **Autre groupe** — G4 appelle si URL configurée ; sinon `DEGRADED` |
| G3 | HTTP optionnel | validation `chauffeurId` — voir ci-dessous |

## Validation conducteur G3 (contrat groupe G3)

Endpoint fourni par **G3** :

```http
GET {G3_BASE}/api/users/drivers/ids
Authorization: Bearer <JWT>
```

Réponse : liste d’IDs numériques des utilisateurs `ROLE_DRIVER`, ex. `[1, 5, 42]`.

À la création de mission, si `chauffeurId` est renseigné et validation activée, G4 vérifie que l’id est dans cette liste.

```yaml
sgitu.integration.g3-base-url: http://localhost:8083
sgitu.integration.g3-validation-enabled: true   # quand G3 est démarré
```

**Important :** même `jwt.secret` entre G3 et G4 (ou token G10) pour que le Bearer soit accepté par G3.
Si G3 retourne `[]`, aucun conducteur n’est enregistré avec `ROLE_DRIVER` — créer des comptes chauffeur côté G3 d’abord.

## Règles métier importantes

- **409 Conflict** : deux missions `EN_COURS` pour le même `vehiculeId`.
- **Retard / déviation** : crée un événement ; la mission reste `EN_COURS`.
- Rôles alignés sur **G3** / JWT **G10** — voir `docs/ROLES_G3_G4_ALIGNMENT.md`.
- Accord **G4 ↔ G9** : un seul `ROLE_DISPATCHER` partagé — `docs/ALIGNEMENT_ROLES_G3_G4_G9.md`.

## Documentation

- **3 piliers livraison finale** : `docs/3_PILIERS_LIVRAISON_G4.md`
- Observabilité : `docs/OBSERVABILITE_G4.md`
- Validation croisée : `docs/VALIDATION_CROISEE_G4.md`
- Chaos Monkey : `docs/CHAOS_MONKEY_G4.md`
- Checklist livraison : `docs/CHECKLIST_LIVRAISON_G4.md`

## Variables d'environnement (production)

Ne pas committer de secrets. Utiliser :

- `SGITU_JWT_SECRET`
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
