# SGITU - API Gateway G10 - Endpoints REST

Base URL locale : `http://localhost:8080`

Authentification : `Authorization: Bearer <accessToken>`

Format : JSON.

## 1. Authentification publique

| Methode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/auth/register` | Cree un compte et envoie un email de verification via G5 |
| GET | `/auth/verify-email?token=...` | Active le compte apres verification email |
| POST | `/auth/login` | Retourne un access token et un refresh token |
| POST | `/auth/refresh` | Genere un nouvel access token avec un refresh token valide |
| POST | `/auth/forgot-password` | Demande un token de reinitialisation du mot de passe |
| POST | `/auth/reset-password` | Change le mot de passe avec le token recu |

Roles acceptes en inscription publique :

```text
ROLE_USER
ROLE_PASSENGER
ROLE_STUDENT
ROLE_DRIVER
ROLE_OPERATOR
ROLE_TECHNICIAN
ROLE_STAFF
```

`ROLE_ADMIN` ne peut pas etre cree par `/auth/register`. Il est reserve au compte admin initial et aux endpoints admin.

## 2. Authentification protegee

| Methode | Endpoint | Role | Description |
|---------|----------|------|-------------|
| POST | `/auth/logout` | utilisateur authentifie | Revoque le refresh token |

## 3. Administration G10

Ces endpoints gerent les comptes d'authentification stockes par G10. Ils ne remplacent pas le service G3, qui gere les profils metier.

Role obligatoire : `ROLE_ADMIN`.

| Methode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/admin/users` | Lister les comptes auth G10 |
| GET | `/admin/users/{id}` | Consulter un compte auth G10 |
| PUT | `/admin/users/{id}/role` | Changer le role JWT d'un compte |
| PUT | `/admin/users/{id}/status` | Activer ou desactiver un compte |
| PUT | `/admin/users/{id}/email-verification` | Marquer l'email comme verifie ou non verifie |

Compte admin initial cree au demarrage si absent :

```text
email: admin@sgitu.ma
password: Admin123456
role: ROLE_ADMIN
enabled: true
emailVerified: true
```

Variables configurables :

```text
G10_ADMIN_BOOTSTRAP_ENABLED
G10_ADMIN_EMAIL
G10_ADMIN_PASSWORD
```

## 4. Routes Gateway vers microservices

Toutes les routes `/api/**` demandent un JWT valide. La Gateway valide le token, applique les regles de role, ajoute les headers utiles, puis route vers le microservice cible.

| Groupe | Prefixe Gateway | Service Docker | Port | Acces |
|--------|-----------------|----------------|------|-------|
| G1 | `/api/v1/tickets`, `/api/v1/tickets/**` | `service-billetterie` | 8081 | Authentifie |
| G1 Admin | `/api/v1/admin/tickets/**`, `/api/v1/admin/dashboard`, `/api/v1/ticket-types/**` | `service-billetterie` | 8081 | `ROLE_ADMIN` |
| G2 | `/api/abonnements`, `/api/abonnements/**`, `/api/plans`, `/api/plans/**` | `service-abonnement` | 8082 | Authentifie |
| G2 Admin | `/api/abonnements/admin`, `/api/abonnements/admin/**` | `service-abonnement` | 8082 | `ROLE_ADMIN` |
| G3 | `/api/users/**`, `/api/profiles/**` | `user-service` | 8083 | Authentifie |
| G3 Admin | `/api/users/*/roles`, `/api/users/*/deactivate` | `user-service` | 8083 | `ROLE_ADMIN` |
| G4 | `/api/g4/**`, `/api/v1/operator/status` | `coordination-service` | 8084 | Authentifie |
| G5 | `/api/notifications`, `/api/notifications/**` | `notification-service` | 8085 | Authentifie |
| G6 | `/api/payments/**`, `/api/refunds/**`, `/api/payment-accounts/**`, `/api/invoices/**`, `/api/test-cards`, `/api/test-mobile-money-accounts`, `/api/health` | `payment-service` | 8086 | Authentifie |
| G7 | `/api/suivi-vehicules/**` | `g7-suivi-vehicules` | 8087 | Authentifie |
| G8 Ingestion | `/api/v1/ingestion/**` | `analytics-service` | 8088 | Authentifie |
| G8 Analytics/ML | `/api/v1/analytics/**`, `/predict/peak-hours`, `/predict/incidents` | `analytics-service` | 8088 | `ROLE_ADMIN` ou `ROLE_AGENT` |
| G9 | `/api/incidents/**`, `/api/rapports/**` | `service-gestion-incidents` | 8089 | Authentifie |

Headers ajoutes par G10 aux microservices :

```text
X-User-Id: 10
X-User-Email: user@sgitu.ma
X-Roles: ROLE_USER
X-Correlation-Id: 8f2a9c2e-1234-45aa-90bb-abcdef123456
```

## 5. Integration G5 Notifications

G10 utilise G5 pour :

- email de verification lors de l'inscription ;
- email de reset password.

URL locale :

```text
http://localhost:8085/api/notifications/send
```

URL Docker :

```text
http://notification-service:8085/api/notifications/send
```

Format envoye par G10 pour `VERIFY_EMAIL` :

```json
{
  "notificationId": "auth-8f2a9c2e-1234-45aa-90bb-abcdef123456",
  "sourceService": "AUTH",
  "eventType": "VERIFY_EMAIL",
  "channel": "EMAIL",
  "priority": "NORMAL",
  "recipient": {
    "userId": "10",
    "email": "user@sgitu.ma"
  },
  "metadata": {
    "sourceType": "ACCOUNT",
    "sourceId": 10,
    "verificationLink": "http://localhost:8080/auth/verify-email?token=abc"
  }
}
```

Format envoye par G10 pour `RESET_PASSWORD` :

```json
{
  "notificationId": "auth-8f2a9c2e-1234-45aa-90bb-abcdef123456",
  "sourceService": "AUTH",
  "eventType": "RESET_PASSWORD",
  "channel": "EMAIL",
  "priority": "NORMAL",
  "recipient": {
    "userId": "10",
    "email": "user@sgitu.ma"
  },
  "metadata": {
    "sourceType": "ACCOUNT",
    "sourceId": 10,
    "resetLink": "http://localhost:8080/auth/reset-password?token=xyz"
  }
}
```

Regle G5 respectee : `eventType` est toujours a la racine du JSON, jamais dans `metadata`.

Si G5 exige le header `Authorization: Bearer <JWT>` pour les appels service-to-service, renseigner :

```text
G10_NOTIFICATIONS_BEARER_TOKEN=<token_fourni_par_G5>
```

Route Gateway G5 :

```text
Client -> G10: /api/notifications/**
G10 -> G5: /api/notifications/**
```

Contrairement a plusieurs autres routes, G10 ne fait pas de `RewritePath` pour G5, car le service Notifications expose deja ses endpoints avec le prefixe `/api`.

## 6. Integration G8 Analyse & Donnees

G10 route vers G8 sans `RewritePath`, car le contrat G8 expose deja les prefixes complets `/api/v1/...` et `/predict/...`.

Endpoints d'ingestion G8 acceptes par la Gateway :

```text
POST /api/v1/ingestion/tickets
POST /api/v1/ingestion/subscriptions
POST /api/v1/ingestion/payments
POST /api/v1/ingestion/vehicles
POST /api/v1/ingestion/incidents
POST /api/v1/ingestion/users
```

Endpoints analytics et rapports G8 acceptes par la Gateway :

```text
GET  /api/v1/analytics/trips/summary
GET  /api/v1/analytics/revenue/summary
GET  /api/v1/analytics/incidents/stats
GET  /api/v1/analytics/vehicles/activity
GET  /api/v1/analytics/users/stats
GET  /api/v1/analytics/subscriptions/stats
GET  /api/v1/analytics/dashboard
POST /api/v1/analytics/reports/generate
GET  /api/v1/analytics/reports/{id}
```

Endpoints ML G8 acceptes par la Gateway :

```text
POST /predict/peak-hours
POST /predict/incidents
```

Regles d'acces G8 :

```text
/api/v1/ingestion/**     -> JWT valide
/api/v1/analytics/**     -> ROLE_ADMIN ou ROLE_AGENT
/predict/**              -> ROLE_ADMIN ou ROLE_AGENT
```

## 7. Erreurs standard

Format unifie :

```json
{
  "timestamp": "2026-05-06T21:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "code": "UNAUTHORIZED",
  "message": "Authentification requise ou token invalide",
  "path": "/api/v1/tickets/123",
  "correlationId": "8f2a9c2e-1234-45aa-90bb-abcdef123456"
}
```

Codes principaux :

| Status | Code | Signification |
|--------|------|---------------|
| 400 | `BAD_REQUEST` | Donnees invalides |
| 401 | `UNAUTHORIZED` ou `INVALID_TOKEN` | Token absent, invalide ou expire |
| 403 | `FORBIDDEN` | Role insuffisant |
| 404 | `ROUTE_NOT_FOUND` | Aucune route Gateway correspondante |
| 409 | `CONFLICT` | Email deja utilise |
| 503 | `SERVICE_UNAVAILABLE` | Microservice cible indisponible |

## 8. Monitoring

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Etat de sante de la Gateway |
| `GET /actuator/info` | Informations sur l'application |
| `GET /swagger-ui.html` | Interface Swagger UI |
| `GET /v3/api-docs` | Spec OpenAPI JSON |

## 9. Logs

G10 utilise SLF4J avec Logback, fournis par Spring Boot.

Fichier de logs :

```text
logs/api-gateway.log
```

En Docker G10, ce dossier est mappe vers la machine :

```text
api-gateway/logs/api-gateway.log
```

Chaque requete Gateway journalisee contient :

```text
correlationId
method
path
user
roles
status
```

Le header `X-Correlation-Id` est aussi transmis aux microservices et retourne dans la reponse HTTP.
