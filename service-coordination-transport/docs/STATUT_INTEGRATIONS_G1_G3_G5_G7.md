# Intégrations G4 — G1, G3, G5, G7 (version production)

## Configuration par défaut (`application.yml`)

| Paramètre | Défaut |
|-----------|--------|
| `sgitu.kafka.enabled` | **true** |
| `sgitu.g5-notification.post-on-event-enabled` | **true** |
| `sgitu.integration.g3-notification-recipients-enabled` | **true** |
| `sgitu.integration.g3-validation-enabled` | **true** |
| `sgitu.integration.g7-flow-enabled` | **true** |

## G5 + G3 — notifications

- **Un POST G5 par utilisateur**, chaque payload inclut **`recipient`**.
- Emails fournis par G3 : `GET /api/users/notification-recipients?page=0&size=100`.
- Implémentation : `G5RecipientBroadcastService`, `G3UserClient`.

Sans réponse G3 (liste vide) : aucun POST G5 (log ERROR supervision) — pas d’envoi sans `recipient`.

### G3 — livraison équipe (PR en revue)

- URL : `GET /api/users/notification-recipients?page=0&size=100`
- Auth : Bearer JWT avec **`ROLE_G4_OPERATOR`**
- Réponse : `userId` (nombre), `email`, pagination `page` / `size` / `total`

**Alertes Kafka (sans utilisateur connecté)** : variable d’environnement

`SGITU_G3_SERVICE_BEARER_TOKEN` = JWT obtenu via `POST /api/auth/login` (compte `ROLE_G4_OPERATOR`).

## G7

- Kafka `vehicle.registered`, `vehicule-positions`
- REST statut véhicule `EN_SERVICE` / `DISPONIBLE`
- Référentiel + API `/api/v1/*` pour G7

## G1

- Kafka producteur `missions-lifecycle` sur transitions mission **et** sur retard / déviation
- 6 `eventType` : `MISSION_PLANIFIED`, `ON_GOING`, `MISSION_CLOSED`, `MISSION_CANCELLED`, `DELAY_ALERT`, `ROUTE_DEVIATION`

## Tests unitaires

Le profil `test` désactive Kafka et les appels G3/G5 pour l’isolation Maven (`application-test.yml`).
