# Chaos Monkey — scénario soutenance G4

Objectif prof : prouver que G4 **ne plante pas** quand un voisin est indisponible.

## Scénario principal : G5 down

| Étape | Action | Résultat attendu |
|-------|--------|------------------|
| 1 | G4 up (`docker compose up -d`) | `/api/g4/health` → OK |
| 2 | G5 ou G10 arrêté / URL invalide | — |
| 3 | `POST /api/notifications/send` (JWT DISPATCHER) | **202** + `"status": "DEGRADED"` + stockage PENDING |
| 4 | `GET /api/g4/pending-notifications` (JWT admin) | entrée **PENDING** |
| 5 | `GET /api/g4/logs` | `NOTIFICATION_PENDING` |
| 6 | Relancer G5 + retry auto (30s) ou `POST .../retry` | `pendingNotifications=0` dans `/health` |

## Scénario secondaire : G3 validation off

| Config | Comportement |
|--------|--------------|
| `SGITU_G3_VALIDATION_ENABLED=false` | création mission OK sans G3 |
| `SGITU_G3_VALIDATION_STRICT=true` + G3 down | 400 si chauffeur invalide |

## Scénario Kafka : message mal formé

Publier sur `vehicule-positions` un JSON sans `vehiculeId` → log WARN, pas de crash G4.

Exemple valide : `postman/examples/kafka-g7-position.json`.

## Capture à inclure dans le rapport

- Postman : requête notification + réponse DEGRADED
- Terminal : `docker compose ps` avec G5 absent
- `/api/g4/logs` montrant le mode dégradé
