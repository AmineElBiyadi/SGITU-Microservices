# Documentation Technique — Microservice G6 Paiement
### Système de Gestion Intelligente des Transports Urbains (SGITU)

**Version** : 1.0.0 — Rapport Technique Final  
**Groupe** : G6 — Paiement  
**Port** : 8086 (HTTPS)  
**Base de données** : `payment_db` (MySQL)  
**Enseignante** : Pr. Besri — AU 2025–2026

---

## Table des matières

1. [Démarrage et déploiement](#1-démarrage-et-déploiement)
2. [Sécurité : TLS et JWT](#2-sécurité--tls-et-jwt)
3. [Tests via Swagger](#3-tests-via-swagger)
4. [Intégrations inter-microservices](#4-intégrations-inter-microservices)
5. [Référence des endpoints](#5-référence-des-endpoints)
6. [Formats JSON](#6-formats-json)
7. [Gestion des erreurs](#7-gestion-des-erreurs)
8. [Observabilité](#8-observabilité)

---

## 1. Démarrage et déploiement

Le microservice G6 est entièrement conteneurisé et intégré au fichier `docker-compose.yml` global du projet. Il peut être démarré indépendamment des autres groupes avec la commande suivante, exécutée à la racine du projet :

```bash
docker compose up -d payment-service g6-payment-db kafka prometheus grafana
```

### Services démarrés

| Service | Description | Port local | Port interne |
|---|---|---|---|
| `payment-service` | Backend Spring Boot G6 | 8086 | 8086 |
| `g6-payment-db` | Base de données MySQL (`payment_db`) | 3316 | 3306 |
| `kafka` | Broker d'événements (communication avec G5) | — | — |
| `prometheus` | Collecte des métriques | 9090 | 9090 |
| `grafana` | Dashboard de monitoring | 3000 | 3000 |

### Vérification du démarrage

```bash
# Vérifier que les conteneurs sont actifs
docker compose ps

# Consulter les logs du service
docker compose logs -f payment-service

# Tester l'état de santé
curl -k https://localhost:8086/health
```

### Variables d'environnement (docker-compose.yml)

```yaml
payment-service:
  build: ./service-paiement
  container_name: payment-service
  ports:
    - "8086:8086"
  environment:
    SERVER_PORT: 8086
    SPRING_DATASOURCE_URL: jdbc:mysql://g6-payment-db:3306/payment_db
    SPRING_DATASOURCE_USERNAME: root
    SPRING_DATASOURCE_PASSWORD: root
    NOTIFICATION_SERVICE_URL: http://notification-service:8085
  depends_on:
    - g6-payment-db
    - kafka
```

---

## 2. Sécurité : TLS et JWT

### 2.1 Chiffrement TLS (HTTPS)

Le microservice écoute **exclusivement** en HTTPS. Un certificat auto-signé au format PKCS12 est utilisé (`keystore.p12`, inclus dans les ressources du projet).

**URL de base** : `https://localhost:8086`

Configuration Spring Boot appliquée :

```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=payment-service
```

Spring Security force également un canal sécurisé sur toutes les requêtes :

```java
.requiresChannel(channel -> channel.anyRequest().requiresSecure())
```

**Note lors des tests navigateur** : un avertissement de sécurité s'affiche en raison du certificat auto-signé. Cliquer sur "Paramètres avancés" puis "Continuer vers localhost" pour accéder à Swagger ou à l'API.

Pour les appels `curl`, utiliser le flag `-k` pour ignorer la vérification du certificat :

```bash
curl -k https://localhost:8086/health
```

### 2.2 Authentification JWT

Toutes les requêtes vers les endpoints métier doivent inclure un token JWT valide dans le header HTTP suivant :

```
Authorization: Bearer <token>
```

**Paramètres JWT de test** :

| Paramètre | Valeur |
|---|---|
| Secret | `SGITU_G6_JWT_SECRET_KEY_CHANGE_ME_IN_PRODUCTION_256BITS!!` |
| Algorithme | HS256 |
| Émetteur attendu | G10 API Gateway |

**G6 ne génère pas les tokens JWT.** Il valide uniquement les tokens émis par G10 (API Gateway & Sécurité). Pour les tests isolés, un token peut être généré manuellement sur [jwt.io](https://jwt.io) avec le secret ci-dessus.

**Claims attendus dans le payload JWT** :

| Claim | Description |
|---|---|
| `sub` | Email de l'utilisateur |
| `role` | Rôle : `ROLE_USER`, `ROLE_ADMIN` ou `ROLE_SERVICE` |
| `iat` | Date de création |
| `exp` | Date d'expiration |

### 2.3 Endpoints publics (sans JWT)

Les endpoints suivants restent accessibles sans authentification :

```
/v3/api-docs/**
/swagger-ui/**
/swagger-ui.html
/health
/actuator/**
```

Tous les autres endpoints sont protégés par la règle `.anyRequest().authenticated()`.

---

## 3. Tests via Swagger

L'intégralité de l'API est documentée avec OpenAPI 3 / Swagger UI.

**URL** : `https://localhost:8086/swagger-ui.html`

### Procédure de test

1. Ouvrir l'URL ci-dessus dans un navigateur et accepter l'avertissement de sécurité lié au certificat auto-signé.
2. Cliquer sur le bouton **Authorize** en haut à droite de l'interface.
3. Saisir le token JWT de test dans le champ `BearerAuth` sous la forme : `Bearer <token>`.
4. Confirmer en cliquant sur **Authorize**, puis **Close**.
5. Les requêtes effectuées depuis Swagger incluront automatiquement le header d'authentification.

### Controllers documentés dans Swagger

| Controller | Périmètre |
|---|---|
| `PaymentController` | Gestion des transactions de paiement |
| `RefundController` | Gestion des remboursements |
| `InvoiceController` | Gestion des factures |
| `PaymentAccountController` | Gestion des moyens de paiement |
| `TestDataController` | Données de simulation (cartes et comptes Mobile Money fictifs) |

### Obtenir un token JWT de test (jwt.io)

1. Aller sur [https://jwt.io](https://jwt.io).
2. Sélectionner l'algorithme `HS256`.
3. Dans le payload, saisir :
```json
{
  "sub": "client@example.com",
  "role": "ROLE_USER",
  "iat": 1700000000,
  "exp": 9999999999
}
```
4. Dans le champ "Your 256-bit secret", coller : `SGITU_G6_JWT_SECRET_KEY_CHANGE_ME_IN_PRODUCTION_256BITS!!`
5. Copier le token encodé généré et l'utiliser dans Swagger ou Postman.

---

## 4. Intégrations inter-microservices

### 4.1 G5 Notifications — Kafka (asynchrone)

La communication avec G5 est entièrement asynchrone via Kafka, conformément au contrat G5 v3.1. OpenFeign n'est plus utilisé pour cette intégration.

| Paramètre | Valeur |
|---|---|
| Protocole | Apache Kafka |
| Topic | `payment.notification` |
| Direction | G6 → Kafka → G5 |
| Fiabilité | `acks=all`, `retries=3` |

**Événements publiés par G6** :

| Événement (`eventType`) | Déclencheur |
|---|---|
| `PAYMENT_METHOD_OTP` | Ajout d'un moyen de paiement (envoi du code OTP par email) |
| `PAYMENT_SUCCESS` | Paiement validé avec succès |
| `PAYMENT_FAILED` | Paiement refusé (solde insuffisant, token invalide, etc.) |
| `PAYMENT_CANCELLED` | Paiement annulé par le client |
| `INVOICE_GENERATED` | Facture générée après un paiement réussi |
| `REFUND_SUCCESS` | Remboursement traité avec succès |

**Structure du message Kafka** (contrat G5 v3.1) :

```json
{
  "notificationId": "uuid-pay-001",
  "sourceService": "PAYMENT",
  "eventType": "PAYMENT_SUCCESS",
  "channel": "EMAIL",
  "priority": "NORMAL",
  "recipient": {
    "userId": "10",
    "email": "client@example.com"
  },
  "metadata": {
    "paymentId": 100,
    "amount": 150.0,
    "paymentMethod": "CARD",
    "sourceType": "SUBSCRIPTION",
    "sourceId": 42
  }
}
```

> **Contrainte importante** : `eventType` doit être placé à la **racine** du message, et non dans l'objet `metadata`. Toute déviation de cette structure entraîne un rejet silencieux côté G5.

### 4.2 G1 Billetterie — Callback REST (RestTemplate)

Lorsqu'un remboursement de type `TICKET` est validé, G6 notifie G1 via un appel REST synchrone.

| Paramètre | Valeur |
|---|---|
| Protocole | HTTP REST (RestTemplate) |
| Direction | G6 → G1 |
| Méthode | `POST` |
| URL | `http://g1-user-service:8081/tickets/remboursement/confirmation` |
| Déclencheur | Remboursement avec `sourceType = TICKET` passé en statut `REFUNDED` |

### 4.3 G2 Abonnements — Callbacks REST (RestTemplate)

G6 notifie G2 dans deux scénarios distincts :

| Scénario | Méthode | URL | Déclencheur |
|---|---|---|---|
| Confirmation de paiement | `POST` | `http://service-abonnement:8082/abonnements/paiement/confirmation` | Paiement `SUBSCRIPTION` passé en `SUCCESS` |
| Confirmation de remboursement | `POST` | `http://service-abonnement:8082/abonnements/remboursement/confirmation` | Remboursement `SUBSCRIPTION` passé en `REFUNDED` |

### 4.4 G10 API Gateway — JWT entrant

G10 route toutes les requêtes vers G6 sur le port 8086 et transmet le token JWT dans le header `Authorization`. G6 valide ce token à chaque requête via `JwtAuthenticationFilter`.

---

## 5. Référence des endpoints

### 5.1 Paiements

| Méthode | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/payments` | Créer et traiter un paiement | JWT requis |
| `GET` | `/payments/{paymentId}` | Consulter un paiement par identifiant | JWT requis |
| `GET` | `/payments/user/{userId}` | Historique des paiements d'un utilisateur | JWT requis |
| `PUT` | `/payments/{paymentId}/cancel` | Annuler un paiement en statut `PENDING` | JWT requis |

### 5.2 Remboursements

| Méthode | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/payments/{paymentId}/refund` | Traiter un remboursement | JWT requis |
| `GET` | `/refunds/{refundId}` | Consulter un remboursement par identifiant | JWT requis |
| `GET` | `/refunds/payment/{paymentId}` | Remboursements liés à un paiement | JWT requis |
| `GET` | `/refunds/user/{userId}` | Remboursements d'un utilisateur | JWT requis |

### 5.3 Factures

| Méthode | Endpoint | Description | Auth |
|---|---|---|---|
| `GET` | `/invoices/{invoiceId}` | Consulter une facture par identifiant | JWT requis |
| `GET` | `/invoices/number/{invoiceNumber}` | Consulter une facture par numéro | JWT requis |
| `GET` | `/payments/{paymentId}/invoice` | Facture liée à un paiement | JWT requis |
| `GET` | `/invoices/user/{userId}` | Factures d'un utilisateur | JWT requis |

### 5.4 Moyens de paiement

| Méthode | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/payment-accounts/card` | Ajouter une carte bancaire fictive | JWT requis |
| `POST` | `/payment-accounts/mobile-money` | Ajouter un compte Mobile Money fictif | JWT requis |
| `POST` | `/payment-accounts/{id}/verify-otp` | Vérifier le code OTP | JWT requis |
| `GET` | `/payment-accounts/user/{userId}` | Lister les moyens de paiement d'un utilisateur | JWT requis |
| `GET` | `/payment-accounts/id/{id}` | Consulter un moyen de paiement par identifiant | JWT requis |
| `DELETE` | `/payment-accounts/id/{id}` | Supprimer un moyen de paiement | JWT requis |

### 5.5 Simulation et monitoring

| Méthode | Endpoint | Description | Auth |
|---|---|---|---|
| `GET` | `/test-cards` | Lister les cartes bancaires de test (avec token, CVV, solde) | Public |
| `GET` | `/test-mobile-money-accounts` | Lister les comptes Mobile Money de test | Public |
| `GET` | `/health` | Vérifier l'état de santé du service | Public |

---

## 6. Formats JSON

### 6.1 Requête — Créer un paiement (`POST /payments`)

```json
{
  "userId": 10,
  "email": "client@example.com",
  "sourceType": "SUBSCRIPTION",
  "sourceId": 42,
  "amount": 150.0,
  "paymentMethod": "CARD",
  "savedPaymentToken": "CARD-TOKEN-001",
  "description": "Paiement abonnement mensuel"
}
```

Valeurs possibles pour `sourceType` : `TICKET`, `SUBSCRIPTION`  
Valeurs possibles pour `paymentMethod` : `CARD`, `MOBILE_MONEY`

### 6.2 Réponse — Paiement validé

```json
{
  "paymentId": 100,
  "transactionToken": "PAY-TOKEN-100",
  "status": "SUCCESS",
  "message": "Paiement valide avec succes",
  "invoiceId": 50,
  "failureReason": null
}
```

### 6.3 Réponse — Paiement refusé

```json
{
  "paymentId": 101,
  "transactionToken": "PAY-TOKEN-101",
  "status": "FAILED",
  "message": "Solde insuffisant",
  "invoiceId": null,
  "failureReason": "INSUFFICIENT_BALANCE"
}
```

### 6.4 Requête — Ajouter une carte bancaire (`POST /payment-accounts/card`)

```json
{
  "userId": 10,
  "cardNumber": "4532015112830366",
  "cardHolderName": "TEST USER 0366",
  "cvv": "123",
  "expiryMonth": 12,
  "expiryYear": 2027,
  "email": "client@example.com"
}
```

### 6.5 Requête — Vérifier un OTP (`POST /payment-accounts/{id}/verify-otp`)

```json
{
  "paymentAccountId": 1,
  "otpCode": "847291",
  "userId": 10
}
```

---

## 7. Gestion des erreurs

Toutes les erreurs sont retournées dans un format JSON uniforme avec un code HTTP approprié.

### Codes HTTP utilisés

| Code | Signification | Exemple de cas |
|---|---|---|
| 200 | Requête traitée avec succès | Consultation d'un paiement ou d'une facture |
| 201 | Ressource créée | Ajout d'un moyen de paiement |
| 400 | Requête invalide | OTP invalide, paiement non annulable, champ manquant |
| 401 | Non authentifié | Token JWT absent ou expiré |
| 403 | Accès refusé | Rôle insuffisant pour l'opération |
| 404 | Ressource introuvable | Paiement, facture ou moyen de paiement inexistant |
| 409 | Conflit métier | Moyen de paiement déjà enregistré pour cet utilisateur |
| 500 | Erreur interne | Erreur technique contrôlée côté service |

### Raisons d'échec de paiement (`failureReason`)

| Valeur | Description |
|---|---|
| `INSUFFICIENT_BALANCE` | Solde insuffisant sur le compte |
| `ACCOUNT_NOT_ACTIVE` | Moyen de paiement non encore vérifié (OTP en attente) |
| `ACCOUNT_EXPIRED` | Carte ou compte Mobile Money expiré |
| `INVALID_TOKEN` | Token de paiement non reconnu |
| `CARD_BLOCKED` | Carte bancaire bloquée |
| `UNAUTHORIZED_TOKEN` | Token ne correspond pas à l'utilisateur |
| `INVALID_CVV` | Code CVV incorrect |
| `MAX_ATTEMPTS_REACHED` | Nombre maximal de tentatives OTP dépassé |

---

## 8. Observabilité

### Endpoints Actuator

| URL | Description |
|---|---|
| `https://localhost:8086/actuator/health` | État de santé du service et de ses dépendances |
| `https://localhost:8086/actuator/metrics` | Liste des métriques disponibles |
| `https://localhost:8086/actuator/prometheus` | Métriques au format Prometheus (scraping) |

### Prometheus et Grafana

Prometheus collecte automatiquement les métriques exposées par `/actuator/prometheus`. Grafana visualise ces données via un dashboard préconfigéré accessible à l'adresse `http://localhost:3000`.

Métriques suivies : état du service, volume de requêtes HTTP, temps de réponse moyen, taux d'erreurs, consommation mémoire JVM, uptime.

### Collection Postman

Une collection Postman est disponible dans le dossier `docs/` du dépôt :

```
docs/G6-Paiement.postman_collection.json
```

Elle couvre l'ensemble des scénarios fonctionnels du service et peut être importée directement dans Postman pour rejouer les tests sans configuration manuelle des requêtes.

---

*Document produit par le groupe G6 — Paiement — ENSA Tétouan — AU 2025–2026*