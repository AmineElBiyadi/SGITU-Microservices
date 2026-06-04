# G4 — Alignement strict promo (message G5)

## 1. Kafka (G1, G7, G9…)

- Pas de JWT, pas de G10.
- Topics : `missions-lifecycle`, `vehicule-positions`, `vehicle.registered`, `incident.transport.topic`.

## 2. Front → G10 → G4

G10 injecte **uniquement** : `X-User-Id`, `X-User-Email`, `X-Roles`, `X-Correlation-Id`.

G4 : `GatewayHeaderAuthenticationFilter` (pas de Bearer obligatoire si en-têtes présents).

## 3. G4 → autres micros (direct, sans G10)

**Uniquement** : `Authorization: Bearer <jwt_service>` (secret global `SGITU_JWT_SECRET`).

**Pas** de `X-User-*` sur ces appels.

| Cible | URL Docker (compose racine) | Chemin type |
|-------|----------------------------|-------------|
| G3 | `http://g3-user-service:8083` | `/api/users/notification-recipients` |
| G5 | `http://notification-service:8085` | `/api/notifications/send` |
| G7 | `http://g7-service:8087` | `/api/suivi-vehicules/vehicules/...` |
| G9 | `http://g9-incidents:8089` | `/api/internal/incidents/correlation` |
| G1 | Kafka uniquement | topic `missions-lifecycle` |

G10 (`SGITU_G10_URL`) : supervision / tests front uniquement — **pas** utilisé pour G4→G5.
