# SGITU — API Gateway G10 · Documentation des Endpoints REST

**Base URL :** `http://localhost:8080`  
**Authentification :** JWT Bearer Token (header `Authorization: Bearer <accessToken>`)  
**Format :** JSON (`Content-Type: application/json`)

---

## 1. Endpoints Auth — Publics (aucun token requis)

### POST `/auth/register`
Créer un nouveau compte utilisateur. Un email de vérification est envoyé via G5.

**Request Body :**
```json
{
  "email": "user@example.com",
  "password": "MotDePasse123",
  "role": "ROLE_PASSENGER"
}
```
**Roles disponibles :** `ROLE_PASSENGER`, `ROLE_STUDENT`, `ROLE_DRIVER`, `ROLE_OPERATOR`, `ROLE_TECHNICIAN`, `ROLE_STAFF`, `ROLE_ADMIN`

**Réponse 200 :**
```json
{ "message": "Inscription réussie. Veuillez vérifier votre email." }
```
**Erreurs :** `409 CONFLICT` (email déjà utilisé), `400 BAD_REQUEST` (données invalides)

---

### GET `/auth/verify-email?token=<token>`
Activer le compte après réception de l'email de vérification.

**Paramètre :** `token` (String — reçu par email)

**Réponse 200 :**
```json
{ "message": "Email vérifié avec succès. Compte activé." }
```
**Erreurs :** `400 BAD_REQUEST` (token invalide ou expiré)

---

### POST `/auth/login`
Connexion et émission d'un access token + refresh token JWT.

**Request Body :**
```json
{
  "email": "user@example.com",
  "password": "MotDePasse123"
}
```
**Réponse 200 :**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer"
}
```
**Erreurs :** `401 UNAUTHORIZED` (identifiants incorrects), `403 FORBIDDEN` (compte désactivé)

---

### POST `/auth/forgot-password`
Envoyer un email de réinitialisation du mot de passe.

**Request Body :**
```json
{ "email": "user@example.com" }
```
**Réponse 200 :**
```json
{ "message": "Email de réinitialisation envoyé." }
```

---

### POST `/auth/reset-password`
Réinitialiser le mot de passe avec le token reçu par email.

**Request Body :**
```json
{
  "token": "<token_recu_par_email>",
  "newPassword": "NouveauMotDePasse123"
}
```
**Réponse 200 :**
```json
{ "message": "Mot de passe réinitialisé avec succès." }
```
**Erreurs :** `400 BAD_REQUEST` (token invalide ou expiré)

---

### POST `/auth/refresh`
Renouveler l'access token à partir du refresh token.

**Request Body :**
```json
{ "refreshToken": "eyJhbGci..." }
```
**Réponse 200 :**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer"
}
```
**Erreurs :** `401 UNAUTHORIZED` (refresh token invalide ou révoqué)

---

## 2. Endpoints Auth — Protégés (token requis)

### POST `/auth/logout`
Déconnecter l'utilisateur et révoquer le refresh token.

**Header :** `Authorization: Bearer <accessToken>`  
**Request Body :**
```json
{ "refreshToken": "eyJhbGci..." }
```
**Réponse 200 :**
```json
{ "message": "Deconnexion reussie" }
```

---

## 3. Routes Gateway → Microservices (JWT requis)

Toutes les routes `/api/**` nécessitent un `Authorization: Bearer <accessToken>` valide.  
La Gateway valide le JWT, injecte `X-User-Id`, `X-Roles`, `X-Correlation-Id` puis route vers le microservice cible.

| Groupe | Préfixe Gateway | Hôte interne | Port | Accès |
|--------|-----------------|--------------|------|-------|
| G1 | `/api/tickets/**` | `service-billetterie` | 8081 | Authentifié |
| G2 | `/api/abonnements/**`, `/api/plans/**` | `service-abonnement` | 8082 | Authentifié |
| G2 Admin | `/api/abonnements/admin/**` | `service-abonnement` | 8082 | `ROLE_ADMIN` |
| G3 | `/api/users/**`, `/api/profiles/**` | `user-service` | 8083 | Authentifié |
| G3 Admin | `/api/users/*/roles`, `/api/users/*/deactivate` | `user-service` | 8083 | `ROLE_ADMIN` |
| G4 | `/api/coordination/**`, `/api/routes/**`, `/api/schedules/**` | `coordination-service` | 8084 | Authentifié |
| G5 | `/api/notifications/**`, `/api/notify/**` | `notification-service` | 8085 | Authentifié |
| G6 | `/api/payments/**`, `/api/payment-accounts/**`, `/api/invoices/**` | `payment-service` | 8086 | Authentifié |
| G7 | `/api/vehicles/**`, `/api/vehicules/**` | `g7-suivi-vehicules` | 8087 | Authentifié |
| G8 | `/api/analytics/**`, `/api/reports/**` | `analytics-service` | 8088 | `ROLE_ADMIN` ou `ROLE_AGENT` |
| G9 | `/api/incidents/**`, `/api/rapports/**` | `service-gestion-incidents` | 8089 | Authentifié |

### Headers injectés par la Gateway
| Header | Description |
|--------|-------------|
| `X-User-Id` | ID de l'utilisateur extrait du JWT |
| `X-Roles` | Rôle(s) de l'utilisateur |
| `X-Correlation-Id` | Identifiant unique de la requête (UUID) |

---

## 4. Codes d'erreur standards

| Code | Signification | Exemple |
|------|---------------|---------|
| 200 | Succès | Login, refresh, logout |
| 201 | Créé | Register |
| 400 | Données invalides | Token expiré, format incorrect |
| 401 | Non authentifié | Token absent ou invalide |
| 403 | Accès interdit | Rôle insuffisant |
| 404 | Ressource introuvable | Email inexistant |
| 409 | Conflit | Email déjà enregistré |
| 500 | Erreur serveur | Panne base de données |
| 503 | Service indisponible | Microservice cible inaccessible |

**Format d'erreur unifié :**
```json
{
  "timestamp": "2025-01-01T08:00:00Z",
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Authentification requise ou token invalide",
  "path": "/api/tickets/123"
}
```

---

## 5. Health & Monitoring

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | État de santé de la Gateway |
| `GET /actuator/info` | Informations sur l'application |
| `GET /swagger-ui.html` | Interface Swagger UI |
| `GET /v3/api-docs` | Spec OpenAPI JSON |
