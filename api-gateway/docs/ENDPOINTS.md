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
| G1 | `/api/v1/tickets/**`, `/api/v1/admin/tickets/**`, `/api/v1/ticket-types/**` | `service-billetterie` | 8081 | Authentifié |
| G2 | `/api/abonnements/**`, `/api/plans/**` | `service-abonnement` | 8082 | Authentifié |
| G2 Admin | `/api/abonnements/admin/**` | `service-abonnement` | 8082 | `ROLE_ADMIN` |
| G3 | `/api/users/**`, `/api/profiles/**` | `user-service` | 8083 | Authentifié |
| G3 Admin | `/api/users/*/roles`, `/api/users/*/deactivate` | `user-service` | 8083 | `ROLE_ADMIN` |
| G4 | `/api/g4/**`, `/api/v1/operator/**` | `coordination-service` | 8084 | Authentifié |
| G5 | `/api/notifications/**`, `/api/notify/**` | `notification-service` | 8085 | Authentifié |
| G6 | `/api/payments/**`, `/api/payment-accounts/**`, `/api/invoices/**` | `payment-service` | 8086 | Authentifié |
| G7 | `/api/vehicles/**`, `/api/vehicules/**` | `g7-suivi-vehicules` | 8087 | Authentifié |
| G8 | `/api/analytics/**`, `/api/reports/**` | `analytics-service` | 8088 | `ROLE_ADMIN` ou `ROLE_AGENT` |
| G9 | `/api/incidents/**`, `/api/rapports/**` | `service-gestion-incidents` | 8089 | Authentifié |

---

## 3.bis Détails des endpoints par microservice

> **Note :** G3 (Utilisateurs) et G9 (Gestion des Incidents) n'ont pas encore finalisé leur documentation d'endpoints. Ces sections seront ajoutées dès réception.



### G1 — Billetterie (v2 — mise à jour)

**Tickets (passager) :**
- `POST   /api/v1/tickets` — Acheter un nouveau ticket (body : `userId`, `tripId`, `ticketTypeId`, `tokenType`)
- `GET    /api/v1/tickets/{ticketId}` — Détails d'un ticket
- `GET    /api/v1/tickets/user/{userId}` — Historique des tickets d'un utilisateur
- `POST   /api/v1/tickets/{ticketId}/validate` — Valider à la borne
- `POST   /api/v1/tickets/{ticketId}/cancel` — Annuler un ticket
- `POST   /api/v1/tickets/{ticketId}/refund` — Demander un remboursement

**Transferts :**
- `POST   /api/v1/tickets/{ticketId}/transfer` — Transférer à un autre passager
- `POST   /api/v1/tickets/{ticketId}/transfer/accept` — Accepter un transfert
- `POST   /api/v1/tickets/{ticketId}/transfer/reject` — Refuser un transfert
- `DELETE /api/v1/tickets/{ticketId}/transfer` — Annuler un transfert initié

**Administration (ROLE_ADMIN) :**
- `GET    /api/v1/admin/tickets/flagged` — Tickets flaggés
- `GET    /api/v1/admin/tickets/{ticketId}/flagged` — Examiner un flag
- `PUT    /api/v1/admin/tickets/{ticketId}/flag/resolve` — Résoudre (adminId extrait du JWT par G10)
- `PUT    /api/v1/admin/tickets/{ticketId}/flag/confirmfraud` — Confirmer fraude
- `DELETE /api/v1/admin/tickets/{ticketId}` — Annuler manuellement
- `POST   /api/v1/admin/tickets/{ticketId}/forcerefund` — Forcer remboursement
- `GET    /api/v1/admin/dashboard` — Tableau de bord statistique
- `GET    /api/v1/admin/tickets/{ticketId}/audit` — Audit trail

**Types de tickets :**
- `GET    /api/v1/ticket-types`, `POST /api/v1/ticket-types`, `PUT /api/v1/ticket-types/{typeId}`, `DELETE /api/v1/ticket-types/{typeId}`

---

### G2 — Abonnements

**Souscriptions `/abonnements` :**
- `POST   /abonnements/souscrire?userId=&planId=` — Créer une souscription
- `GET    /abonnements/{id}` — Détails
- `GET    /abonnements/utilisateur/{userId}` — Liste par utilisateur
- `GET    /abonnements/utilisateur/{userId}/actif` — Abonnement actif courant
- `POST   /abonnements/{id}/annuler` — Demande d'annulation
- `POST   /abonnements/{id}/desactiver?jours=` — Désactiver pour N jours

**Plans `/plans` :**
- `GET    /plans` — Lister les plans
- `GET    /plans/{id}` — Détails d'un plan
- `POST   /plans` — Créer un plan (Admin)
- `PUT    /plans/{id}` — Modifier (Admin)
- `DELETE /plans/{id}` — Supprimer (Admin)

**Administration `/abonnements/admin` (ROLE_ADMIN) :**
- `POST   /abonnements/admin/{id}/suspendre?motif=` — Suspendre
- `POST   /abonnements/admin/{id}/forcer-annulation?motif=` — Forcer l'annulation
- `POST   /abonnements/admin/{id}/forcer-renouvellement` — Forcer le renouvellement

**Webhooks paiement :**
- `POST   /abonnements/paiement/confirmation?transactionId=` — Confirmation paiement
- `POST   /abonnements/remboursement/confirmation?transactionId=` — Confirmation remboursement

---

### G4 — Coordination des transports

**Lignes :**
- `POST /api/g4/lignes`, `GET /api/g4/lignes`, `GET /api/g4/lignes/actives`
- `GET/PUT/DELETE /api/g4/lignes/{ligneId}`
- `GET /api/g4/lignes/{ligneId}/trajets`

**Trajets :**
- `POST/GET /api/g4/trajets`
- `GET/PUT/DELETE /api/g4/trajets/{trajetId}`
- `GET /api/g4/trajets/{trajetId}/arrets`

**Horaires :**
- `POST/GET /api/g4/horaires`, `GET/PUT/DELETE /api/g4/horaires/{horaireId}`

**Arrêts :**
- `POST/GET /api/g4/arrets`, `GET/PUT/DELETE /api/g4/arrets/{arretId}`
- `GET /api/g4/arrets/ligne/{ligneId}`

**Affectations véhicule/ligne :**
- `POST/GET /api/g4/affectations`
- `GET/PUT/DELETE /api/g4/affectations/{affectationId}`
- `GET /api/g4/affectations/vehicule/{vehiculeId}`

**Missions :**
- `POST/GET /api/g4/missions`, `GET /api/g4/missions/actives`
- `GET/PUT /api/g4/missions/{missionId}`, `GET /api/g4/missions/{missionId}/status`
- `POST /api/g4/missions/{missionId}/cloturer`, `POST /api/g4/missions/{missionId}/annuler`

**Événements de coordination (faits métier : retard, déviation, panne, incident, annulation) :**
- `POST/GET /api/g4/events`, `GET /api/g4/events/{eventId}`
- `GET /api/g4/events/type/{eventType}`, `GET /api/g4/events/status/{status}`
- `POST /api/g4/events/detect-delay`, `POST /api/g4/events/detect-deviation`
- `POST /api/g4/events/detect-breakdown`, `POST /api/g4/events/detect-incident`
- `POST /api/g4/events/cancel-mission`

**Supervision :**
- `GET /api/g4/health`, `GET /api/v1/operator/status`, `GET /api/g4/logs`

---

### G5 — Notifications

**API REST (point d'entrée unique async via Kafka) :**
- `POST   /api/notifications/send` — Réception, validation, création async d'une notification
- `GET    /api/notifications/{notificationId}` — Statut & détail d'une notification
- `GET    /api/notifications?userId=&status=&sourceService=&page=&size=` — Liste filtrée paginée
- `POST   /api/notifications/{notificationId}/retry` — Relance manuelle d'un échec
- `GET    /api/notifications/health` — Health-check (DB, providers, Kafka)

**Canaux supportés :** EMAIL (SMTP/SendGrid), SMS (Twilio), PUSH (FCM)  
**Mode :** asynchrone via Kafka — les services appellent `POST /send` puis G5 traite en arrière-plan

---

### G6 — Paiement

**Paiements / Transactions :**
- `POST   /payments` — Initier un paiement
- `GET    /payments/{paymentId}` — Détails d'un paiement
- `GET    /payments/user/{userId}` — Historique des paiements d'un utilisateur
- `PUT    /payments/{paymentId}/cancel` — Annuler un paiement

**Remboursements :**
- `POST   /payments/{paymentId}/refund` — Demander un remboursement
- `GET    /refunds/{refundId}` — Détails d'un remboursement
- `GET    /refunds/payment/{paymentId}` — Remboursements liés à un paiement
- `GET    /refunds/user/{userId}` — Historique des remboursements d'un utilisateur

**Moyens de paiement :**
- `POST   /payment-accounts/card` — Enregistrer une carte
- `POST   /payment-accounts/mobile-money` — Enregistrer un compte mobile money
- `POST   /payment-accounts/{paymentAccountId}/verify-otp` — Vérifier OTP
- `GET    /payment-accounts/user/{userId}` — Comptes d'un utilisateur
- `GET    /payment-accounts/{paymentAccountId}` — Détails d'un compte
- `DELETE /payment-accounts/{paymentAccountId}` — Supprimer un compte

**Factures :**
- `GET    /invoices/{invoiceId}` — Détails d'une facture
- `GET    /payments/{paymentId}/invoice` — Facture liée à un paiement
- `GET    /invoices/user/{userId}` — Factures d'un utilisateur

**Tests / Simulation :**
- `GET    /test-cards`, `GET /test-mobile-money-accounts`, `GET /health`

**Endpoint optionnel pour G8 Analytics :**
- `GET    /payments/analytics?startDate=&endDate=` — Stats de paiements

---

### G7 — Suivi des Véhicules

**Véhicules `/api/suivi-vehicules/vehicules` :**
- `POST   /api/suivi-vehicules/vehicules` — Ajouter un véhicule
- `GET    /api/suivi-vehicules/vehicules` — Liste tous les véhicules
- `GET    /api/suivi-vehicules/vehicules/{id}` — Détail d'un véhicule
- `PUT    /api/suivi-vehicules/vehicules/{id}` — Modifier un véhicule
- `DELETE /api/suivi-vehicules/vehicules/{id}` — Désactiver un véhicule
- `GET    /api/suivi-vehicules/vehicules/actifs` — Véhicules en service
- `GET    /api/suivi-vehicules/vehicules/statut/{statut}` — Filtrer par statut
- `PUT    /api/suivi-vehicules/vehicules/{id}/statut` — Changer le statut
- `GET    /api/suivi-vehicules/vehicules/type/{type}` — Filtrer par type

**Positions GPS `/api/suivi-vehicules/positions` :**
- `POST   /api/suivi-vehicules/positions` — Enregistrer une position GPS
- `GET    /api/suivi-vehicules/positions` — Positions de tous les véhicules
- `GET    /api/suivi-vehicules/positions/{vehiculeId}` — Position actuelle
- `GET    /api/suivi-vehicules/positions/{vehiculeId}/historique` — Historique des trajets
- `GET    /api/suivi-vehicules/positions/{vehiculeId}/vitesse-moyenne` — Vitesse moyenne
- `DELETE /api/suivi-vehicules/positions/{vehiculeId}/historique` — Supprimer historique

**Télémétrie `/api/suivi-vehicules/telemetrie` (IoT) :**
- `POST   /api/suivi-vehicules/telemetrie` — Enregistrer télémétrie IoT
- `GET    /api/suivi-vehicules/telemetrie/{vehiculeId}` — Dernière télémétrie
- `GET    /api/suivi-vehicules/telemetrie/{vehiculeId}/historique` — Historique
- `GET    /api/suivi-vehicules/telemetrie/{vehiculeId}/carburant` — Niveau carburant
- `GET    /api/suivi-vehicules/telemetrie/{vehiculeId}/temperature` — Température moteur
- `GET    /api/suivi-vehicules/telemetrie/{vehiculeId}/etat` — État véhicule

**Alertes `/api/suivi-vehicules/alerts` :**
- `GET    /api/suivi-vehicules/alerts` — Liste (filtres : vehiculeId, statut, typeAlert)
- `GET    /api/suivi-vehicules/alerts/{id}` — Détails d'une alerte
- `GET    /api/suivi-vehicules/alerts/active` — Alertes OUVERTES
- `GET    /api/suivi-vehicules/alerts/vehicule/{vehiculeId}` — Historique pour un véhicule
- `GET    /api/suivi-vehicules/alerts/vehicule/{vehiculeId}/active` — Alertes ouvertes par véhicule
- `GET    /api/suivi-vehicules/alerts/stats` — Statistiques (pour G8)
- `PUT    /api/suivi-vehicules/alerts/{id}/cancel` — Annuler manuellement (opérateur)

**Santé & Monitoring :**
- `GET    /api/suivi-vehicules/health`, `GET /actuator/health`, `GET /actuator/info`

---

### G8 — Analyse & Données

**Ingestion d'événements (depuis les microservices sources) :**
- `POST   /api/v1/ingestion/tickets` — Événements billetterie (depuis G1)
- `POST   /api/v1/ingestion/subscriptions` — Événements abonnements (depuis G2)
- `POST   /api/v1/ingestion/payments` — Événements paiement (depuis G6)
- `POST   /api/v1/ingestion/vehicles` — Télémétrie & statut véhicules (depuis G7)
- `POST   /api/v1/ingestion/incidents` — Rapports d'incidents (depuis G9)
- `POST   /api/v1/ingestion/users` — Activité utilisateurs (depuis G3)

**Analytics (snapshots agrégés) :**
- `GET    /api/v1/analytics/trips/summary` — Snapshots trajets (FREQ)
- `GET    /api/v1/analytics/revenue/summary` — Snapshots revenus (REV)
- `GET    /api/v1/analytics/incidents/stats` — Snapshots incidents (INC)
- `GET    /api/v1/analytics/vehicles/activity` — Snapshots activité véhicules (VEH)
- `GET    /api/v1/analytics/users/stats` — Statistiques utilisateurs
- `GET    /api/v1/analytics/subscriptions/stats` — Statistiques abonnements (SUB)
- `GET    /api/v1/analytics/dashboard` — Tous les snapshots du tableau de bord

**Rapports :**
- `POST   /api/v1/analytics/reports/generate` — Déclencher la génération
- `GET    /api/v1/analytics/reports/{id}` — Récupérer un rapport généré

**Machine Learning (Prédiction) :**
- `POST   /predict/peak-hours` — Prédire les heures de pointe futures
- `POST   /predict/incidents` — Prédire les zones à risque d'incidents

---

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
