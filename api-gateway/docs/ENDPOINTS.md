# G10 - Contrat Gateway & Securite

## 1. Principe corrige

G10 ne possede plus la base officielle des comptes utilisateurs.

- G3 Gestion des utilisateurs est la source de verite pour les comptes, profils, roles, permissions, mots de passe et emission JWT.
- G10 valide les JWT emis par G3.
- G10 applique les regles d'acces transverses.
- G10 route les requetes vers G1-G9.
- G10 ajoute les headers utilisateur aux microservices.

## 2. URL Gateway

```text
http://localhost:8080
```

Avec Docker :

```text
http://api-gateway:8080
```

## 3. JWT attendu par G10

Le JWT est emis par G3 et valide par G10.

Dans l'etat actuel du projet, G3 signe les tokens en HS256 avec un secret partage par variable d'environnement. G10 utilise ce secret uniquement pour verifier la signature. Une evolution finale possible est RS256 : G3 garderait la cle privee et G10 verifierait avec la cle publique.

Claims minimaux :

```json
{
  "sub": "user@sgitu.ma",
  "roles": ["ROLE_USER"],
  "userId": 10,
  "iat": 1715000000,
  "exp": 1715003600,
  "jti": "uuid"
}
```

G10 accepte aussi l'ancien format `"role": "ROLE_USER"` pour rester compatible pendant l'integration.

`userId` peut aussi s'appeler `id`. Si l'id est absent, G10 transmet seulement email, roles et correlation id.

Header client :

```http
Authorization: Bearer <JWT_EMIS_PAR_G3>
```

## 4. Headers transmis aux microservices

Apres validation du JWT, G10 ajoute :

```http
X-User-Id: 10
X-User-Email: user@sgitu.ma
X-Roles: ROLE_USER
X-Correlation-Id: 8f2a9c2e-1234-45aa-90bb-abcdef123456
```

## 5. Endpoints d'authentification

Ces endpoints sont **routes vers G3**, pas traites localement par G10.

| Methode | Path | Responsable |
| --- | --- | --- |
| POST | `/api/users` | G3 |
| POST | `/auth/login` | G3 |
| POST | `/auth/refresh` | G3 |
| POST | `/auth/logout` | G3, protege par G10 avec JWT valide |
| POST | `/auth/forgot-password` | G3 |
| POST | `/auth/reset-password` | G3 |
| GET | `/auth/verify-email?token=...` | G3 |
| GET | `/api/users` | G3, protege par G10 avec `ROLE_ADMIN` |
| GET | `/api/users/roles/{roleName}` | G3, protege par G10 avec `ROLE_SUPERVISOR` ou `ROLE_DISPATCHER` |
| GET | `/api/users/drivers/ids` | G3, protege par G10 avec JWT valide |
| PUT | `/api/users/{id}/roles` | G3, protege par G10 avec `ROLE_ADMIN` |
| PUT | `/api/users/{id}/deactivate` | G3, protege par G10 avec `ROLE_ADMIN` |

## 6. Routes microservices

| Groupe | Prefixes exposes via G10 | Service cible | Port logique | Securite G10 |
| --- | --- | --- | --- | --- |
| G1 Billetterie | `/api/v1/tickets/**`, `/api/v1/admin/**`, `/api/v1/ticket-types/**` | `service-billetterie` | 8081 | JWT, admin pour `/api/v1/admin/**` et ticket-types |
| G2 Abonnements | `/api/abonnements/**`, `/api/plans/**` | `service-abonnement` | 8082 | GET public selon contrat G2, JWT pour souscription/actions, `ROLE_ADMIN_G2` pour `/api/plans/**` en ecriture et `/api/abonnements/admin/**` |
| G3 Utilisateurs/Auth | `/auth/**`, `/api/users/**`, `/api/profiles/**` | `user-service` | 8083 | Public pour auth publique, admin pour modification roles/desactivation, JWT pour `/api/**` |
| G4 Coordination | `/api/g4/**`, `/api/v1/operator/status` | `coordination-service` | 8084 | Health/logs publics, lecture avec `ROLE_G4_OPERATOR`/`ROLE_DISPATCHER`/`ROLE_G4_ADMIN`, ecriture reseau avec `ROLE_G4_OPERATOR` ou `ROLE_G4_ADMIN`, ecriture flotte avec `ROLE_DISPATCHER` ou `ROLE_G4_ADMIN`, supervision avec `ROLE_G4_ADMIN` |
| G5 Notifications | `/api/notifications/**` | `notification-service` | 8085 | JWT, retry admin selon endpoint |
| G6 Paiement | `/api/payments/**`, `/api/refunds/**`, `/api/payment-accounts/**`, `/api/invoices/**`, `/api/test-cards`, `/api/health` | `payment-service` | 8086 | JWT |
| G7 Suivi vehicules | `/api/suivi-vehicules/**` | `g7-suivi-vehicules` | 8087 | Lecture avec `ROLE_ADMIN_G7`/`ROLE_OPERATOR`/`ROLE_TECHNICIAN`, administration vehicules avec `ROLE_ADMIN_G7`, positions avec `ROLE_DRIVER` ou `ROLE_ADMIN_G7`, modification alertes avec `ROLE_OPERATOR` ou `ROLE_ADMIN_G7` |
| G8 Ingestion/Analytics | `/api/v1/ingestion/**`, `/api/v1/analytics/**` | `g8-analytics` | 8088 | JWT, admin/operator/staff pour analytics |
| G8 ML Predictions | `/predict/peak-hours`, `/predict/incidents` | `ml-service` | 5000 | JWT, admin/operator/staff |
| G9 Incidents | `/api/incidents/**`, `/api/rapports/**` | `service-gestion-incidents` | 8089 | JWT |

## 7. Codes d'erreur G10

| Code | Code applicatif | Cas |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | JWT absent sur une route protegee |
| 401 | `INVALID_TOKEN` | JWT invalide, expire ou sans claim role/roles |
| 401 | `TOKEN_REVOKED` | JWT revoque apres logout et present dans la blacklist Redis G3 |
| 403 | `FORBIDDEN` | Role insuffisant |
| 404 | `ROUTE_NOT_FOUND` | Aucune route Gateway ne correspond |
| 503 | `SERVICE_UNAVAILABLE` | Service cible indisponible |
| 504 | `GATEWAY_TIMEOUT` | Delai depasse lors de l'appel du microservice cible |

Format :

```json
{
  "timestamp": "2026-05-07T18:00:00",
  "status": 401,
  "code": "UNAUTHORIZED",
  "message": "Authentification requise ou token invalide",
  "path": "/api/payments/1",
  "correlationId": "test-123"
}
```

## 8. Swagger

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

Swagger de G10 documente la Gateway et le schema Bearer JWT. Les endpoints metier restent documentes par chaque groupe.

## 9. Logs

G10 utilise SLF4J + Logback :

```text
logs/api-gateway.log
```

Les logs contiennent :

- methode HTTP ;
- path ;
- status ;
- utilisateur extrait du JWT ;
- roles ;
- `X-Correlation-Id`.

## 10. Resilience minimale Gateway

G10 configure des timeouts HTTP pour eviter qu'une panne d'un microservice bloque indefiniment le client :

```text
GATEWAY_CONNECT_TIMEOUT_MS=3000
GATEWAY_RESPONSE_TIMEOUT=5s
```

Si le service cible est eteint ou introuvable, G10 retourne une reponse JSON standardisee `503 SERVICE_UNAVAILABLE`. Si le service ne repond pas dans le delai, G10 retourne `504 GATEWAY_TIMEOUT`.

## 11. Blacklist logout

Apres logout, G3 enregistre le JWT revoque dans Redis. Quand `TOKEN_BLACKLIST_ENABLED=true`, G10 consulte Redis avant d'accepter le token. Un token present dans la blacklist est refuse avec `401 TOKEN_REVOKED`.

## 12. Ce que G10 ne fait plus

G10 ne fait plus :

- creation des comptes ;
- stockage des utilisateurs ;
- stockage des roles ;
- stockage des refresh tokens ;
- envoi direct des emails de verification ou reset ;
- modification officielle d'un role.

Ces responsabilites appartiennent a G3. G10 peut bloquer ou autoriser l'acces a partir du JWT, mais il n'est pas source de verite utilisateur.
