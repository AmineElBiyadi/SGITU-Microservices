# Fiche oral — Chaos Monkey (soutenance 1er juin)

**Contexte prof :** « Éteignez un microservice et montrez-moi comment le système réagit. »

**Phrase d’accroche à dire :**

> « G4 reste **UP** quand un voisin tombe. On dégrade proprement — pas de 500 — et on trace tout dans `/api/g4/health` et `/api/g4/logs`. Notre scénario le plus abouti : **G5 Notifications**. »

---

## Avant la soutenance (5 min)

```bash
cd service-coordination-transport
docker compose --profile monitoring up -d --build
```

Vérifier :

```bash
curl http://localhost:8084/api/g4/health
docker ps --format "table {{.Names}}\t{{.Status}}"
```

Ouvrir dans le navigateur / Postman :

- http://localhost:8084/api/g4/health
- http://localhost:8084/api/g4/logs
- Collection Postman → dossier **« 100 — Chaos Monkey »**

Compte Postman : `gestionnaire.flotte` / `password` (DISPATCHER) + `admin.technique` / `password` (ADMIN_G4).

---

## Tableau de réponse par service éteint

| Service éteint | Commande Docker | G4 continue ? | Comportement attendu | Preuve à montrer |
|----------------|-----------------|:-------------:|----------------------|------------------|
| **G5 Notifications** ⭐ | `docker stop g5-notification-service` | **Oui** | `POST /api/notifications/send` → **202** + `"status":"DEGRADED"` ; notification **stockée** (PENDING) ; retry auto 30 s | Postman + `/health` (`pendingNotifications` > 0) + `/logs` |
| **G3 Users** | `docker stop g3-user-service` | **Oui** | Missions/lignes OK si validation G3 **désactivée** (défaut Docker). Si strict : 400 explicite | `GET /api/g4/missions` → 200 ; expliquer config |
| **G7 Véhicules** | `docker stop g7-service` | **Oui** | Appel REST G7 → statut mock `INCONNU`. Kafka positions : plus de nouveaux messages, G4 ne crash pas | `/api/v1/operator/status` → G7 `DOWN` |
| **Kafka** | `docker stop sgitu-kafka` | **Oui** | Consommation/production Kafka en pause ; CRUD missions/lignes **OK** | `/health` → `kafka: DISABLED` ou logs WARN |
| **PostgreSQL G4** | `docker stop sgitu-g4-postgres` | **Non** (normal) | `/health` → `database: DOWN`, `status: DEGRADED` | Montrer que seule **notre** DB bloque G4 |
| **G4 lui-même** | `docker stop g4-coordination-service` | — | Les autres groupes continuent ; G4 est remplaçable | Redémarrer : `docker start g4-coordination-service` |

⭐ **Scénario à proposer au prof en premier** — c’est celui documenté dans le rapport (Resilience4j + file d’attente).

---

## Script démo G5 (2 minutes) — à répéter à l’oral

### 1. État nominal

```bash
curl http://localhost:8084/api/g4/health
# → status UP, pendingNotifications: 0
```

### 2. Le prof (ou vous) éteint G5

```bash
docker stop g5-notification-service
```

### 3. Envoyer une notification (Postman)

`POST http://localhost:8084/api/notifications/send`  
JWT : `gestionnaire.flotte`

Body (dossier Postman « 100 — Chaos Monkey »).

**Réponse attendue :**

```json
{
  "status": "DEGRADED",
  "correlationId": "NOTIF-CHAOS-001",
  "detail": "Service G5 injoignable — notification stockée localement (PENDING), renvoi automatique"
}
```

→ **Pas de 500.** C’est le point clé.

### 4. Prouver la résilience

```bash
curl http://localhost:8084/api/g4/health
# → pendingNotifications: "1"
```

```bash
curl http://localhost:8084/api/g4/logs
# → NOTIFICATION_PENDING
```

Postman (admin) : `GET /api/g4/pending-notifications`

### 5. Relancer G5 et récupérer

```bash
docker start g5-notification-service
```

Attendre 30 s **ou** Postman : `POST /api/g4/pending-notifications/retry`

```bash
curl http://localhost:8084/api/g4/health
# → pendingNotifications: "0"
```

**Phrase de clôture :**

> « La commande métier n’a pas échoué : la notification est en attente et repart dès que G5 revient. C’est exactement le pattern **fallback + retry** du challenge Chaos Monkey. »

---

## Si le prof choisit un autre service

### G3 down

> « G4 est **autonome** pour le cœur métier. La validation conducteur G3 est **optionnelle** : en prod on l’active ; en démo sans G3, les missions se créent quand même. »

Montrer : `POST /api/g4/missions` → **201** (sans `chauffeurId` ou validation off).

### G7 down

> « G7 envoie normalement le GPS via **Kafka** (`vehicule-positions`). Si G7 REST est down, G4 utilise un **statut mock** et continue. Les missions restent EN_COURS. »

Montrer : `GET /api/v1/operator/status` → intégrations.

### Kafka down

> « Kafka est **asynchrone** : G4 reste disponible en REST. Les événements G7/G9/G1 sont bufferisés côté producteurs ou loggés en WARN — pas de crash. »

---

## Ce qu’il ne faut PAS faire à l’oral

| Erreur | Pourquoi |
|--------|----------|
| Paniquer si G5 → DEGRADED | C’est le **comportement voulu** |
| Éteindre Postgres G4 sans explication | Normal que G4 soit DOWN — ce n’est pas un voisin |
| Oublier de relancer G5 après la démo | Montrer le **retry** pour marquer des points |
| Dire « on n’a pas implémenté la résilience » | Si Resilience4j + pending_notifications sont en place |

---

## Raccourcis Docker (noms conteneurs stack full)

```bash
docker stop g5-notification-service    # G5
docker start g5-notification-service
docker stop g4-coordination-service    # G4
docker stop sgitu-kafka                # Kafka
docker stop sgitu-g4-postgres          # DB G4 (éviter sauf pour montrer health DOWN)
docker compose --profile monitoring ps
```

---

## Slides — 1 slide Chaos Monkey

**Titre :** Résilience G4 — appel critique G5

1. Circuit breaker Resilience4j
2. Réponse 202 DEGRADED (jamais 500)
3. File locale `pending_notifications`
4. Retry automatique + endpoint manuel
5. Preuve : `/health`, `/logs`, Grafana (circuit breaker)

---

## Checklist jour J

- [ ] Stack Docker démarrée 15 min avant
- [ ] Postman collection importée, token pré-chargé
- [ ] Onglets ouverts : health, logs, Grafana (optionnel)
- [ ] Terminal prêt avec commandes `docker stop/start`
- [ ] Même `JWT_SECRET` G3/G4 si validation croisée demandée aussi
