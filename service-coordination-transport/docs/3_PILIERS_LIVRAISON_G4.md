# Les 3 piliers du rendu final — G4 Coordination Transport

Document de référence pour la soutenance (29 mai 2026).

---

## Pilier 1 — Intégration « sans couture »

### Objectif prof

Tous les microservices tournent sur le **même réseau Docker** `sgitu-network`, lancés par un compose coordonné.

### Ce que G4 fournit

| Fichier | Rôle |
|---------|------|
| `../docker-compose.yml` (racine monorepo) | Stack globale SGITU : sections des autres groupes + **bloc G4** uniquement (pas de stack G5 ajoutée par G4) |
| `docker-compose.yml` | Stack G4 seule (dev rapide) |
| `docker-compose.yml` + profil `monitoring` | **Stack G4** : G4 + Kafka + Postgres + **Prometheus + Grafana** (sans G5) |

### Démarrage recommandé (monorepo)

```bash
cd SGITU-Microservices
cp .env.example .env
docker compose up -d --build g4-coordination g4-postgres kafka
# G5/G3/G7 : lancer les services de chaque groupe dans le compose racine quand ils sont ajoutés
```

### Démarrage autonome G4 (démo groupe)

```bash
cd service-coordination-transport
docker compose --profile monitoring up -d --build
```

### Vérification réseau

```bash
docker network inspect sgitu-network
docker compose ps
curl http://localhost:8084/api/g4/health
```

### Hostnames Docker (contrats alignés)

| Service | Hostname | Port |
|---------|----------|------|
| G3 Users | `g3-user-service` | 8083 |
| G4 Coordination | `g4-coordination` | 8084 |
| G5 Notifications | `notification-service` | 8085 |
| G7 Véhicules | `g7-service` | 8087 |
| Kafka | `kafka` | 9092 |

Voir aussi : `docs/CONTRATS_ALIGNES_G4.md`

---

## Pilier 2 — Observabilité & monitoring

### Objectif prof

Prouver que le service est **vivant** : health, logs, métriques.

### Endpoints G4 (sans JWT)

| URL | Usage |
|-----|--------|
| `GET /api/g4/health` | État DB, Kafka, notifications en attente |
| `GET /api/g4/logs` | Journal structuré des opérations |
| `GET /actuator/health` | Health Spring Boot |
| `GET /actuator/prometheus` | Métriques Prometheus |

### Dashboard Grafana

- URL : http://localhost:3000 (admin / admin)
- Dashboard : **SGITU G4 — Coordination Transport**
- Source : Prometheus http://localhost:9090

### Logs structurés

Format console : `group=G4 service=g4-coordination` (voir `application.yml`).

### Captures à inclure dans le rapport

1. `GET /api/g4/health` → JSON avec `status: UP`
2. `GET /api/g4/logs` → entrées récentes
3. Grafana dashboard avec courbe `up=1`
4. Prometheus targets → G4 **UP**

Voir : `docs/OBSERVABILITE_G4.md`

---

## Pilier 3 — Validation croisée

### Objectif prof

Preuve Postman qu’un **JWT valide** permet d’appeler un autre groupe.

### Scénarios G4

| # | Scénario | Preuve |
|---|----------|--------|
| A | Login G3 → appel G4 avec Bearer | Capture Postman |
| B | Login G4 → `GET /api/g4/missions` 200 | Capture Postman |
| C | Token G4 rejeté sur endpoint G3 (403/401) | Optionnel |
| D | G7 → Kafka → G4 consomme position | Log G4 `G7_POSITION` |

Collection Postman : dossier **« 99 — Validation croisée »**.

Voir : `docs/VALIDATION_CROISEE_G4.md`  
Dossier captures : `rapport/captures/`

---

## Bonus — Chaos Monkey Challenge

### Énoncé prof

« Que se passe-t-il si un service tombe ? »

### Stratégie G4 (appel critique : **G5 Notifications**)

1. G4 appelle G5 via Resilience4j `@CircuitBreaker`
2. Si G5 down → réponse **202** + `"status": "DEGRADED"` (pas de 500)
3. Notification **stockée localement** (`pending_notifications`, statut PENDING)
4. **Renvoi automatique** toutes les 30 s quand G5 remonte
5. Supervision : `GET /api/g4/health` → `pendingNotifications`, `GET /api/g4/logs`

### Démo soutenance (5 min)

```bash
# 1. Stack up
docker compose --profile monitoring up -d

# 2. Couper G5
docker stop g5-notification-service

# 3. Postman : POST /api/notifications/send (JWT DISPATCHER)
#    → status DEGRADED + message PENDING

# 4. GET /api/g4/pending-notifications (JWT admin.technique)
#    → liste non vide

# 5. Relancer G5
docker start g5-notification-service

# 6. Attendre 30s ou POST /api/g4/pending-notifications/retry
#    → pendingNotifications = 0 dans /health
```

Voir : `docs/CHAOS_MONKEY_G4.md`, `docs/RESILIENCE_G4.md`

---

## Checklist livraison ZIP

- [ ] PDF rapport (3 piliers + Chaos Monkey)
- [ ] Slides soutenance
- [ ] Captures dans `rapport/captures/`
- [ ] Collection Postman exportée
- [ ] `docker compose up` OK
- [ ] Archive `G4_SGITU_Final.zip`
