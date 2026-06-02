# Documentation Technique - Microservice G6 (Paiement)

Ce fichier est destiné à l'équipe backend pour comprendre comment démarrer, tester et intégrer le microservice de Paiement (G6) dans l'écosystème SGITU.

## 🚀 1. Démarrage et Déploiement
Le microservice G6 a été entièrement dockerisé et intégré au fichier `docker-compose.yml` global.
Pour tester G6 indépendamment des autres groupes lourds, exécutez la commande suivante à la racine du projet :

```bash
docker compose up -d payment-service g6-payment-db kafka prometheus grafana
```

- **payment-service** : Le backend Spring Boot du G6 (Port 8086).
- **g6-payment-db** : La base de données MySQL du paiement (Port 3316 en local, 3306 dans Docker).
- **kafka** : Le broker d'événements pour communiquer avec G5.

## 🔒 2. Sécurité : TLS (HTTPS) et JWT
Nous avons implémenté les exigences strictes de sécurité du professeur :

### A. Chiffrement TLS (HTTPS)
- Le microservice écoute **uniquement** en HTTPS via un certificat auto-signé (`keystore.p12` généré).
- URL de base : `https://localhost:8086` (remarquez bien le **https**).
- **Important lors des tests** : Votre navigateur va afficher un avertissement de sécurité (car le certificat est auto-signé). Vous devez cliquer sur "Paramètres avancés" puis "Continuer vers localhost".

### B. Authentification JWT
- Chaque requête (sauf Swagger et `/health`) exige un token JWT valide dans le Header.
- Secret utilisé pour tester : `SGITU_G6_JWT_SECRET_KEY_CHANGE_ME_IN_PRODUCTION_256BITS!!`
- G6 ne génère pas les JWT, il valide ceux générés par G10 ou l'API Gateway.

## 🧪 3. Comment tester avec Swagger ?
Toute l'API est documentée avec OpenAPI 3.

1. Accédez à Swagger : **[https://localhost:8086/swagger-ui.html](https://localhost:8086/swagger-ui.html)**
2. Acceptez le risque de sécurité (lié au HTTPS auto-signé).
3. Cliquez sur le bouton **Authorize** en haut à droite.
4. Entrez un token JWT de test (vous pouvez en générer un via jwt.io avec le secret mentionné plus haut). 

## 🔌 4. Relations avec les autres Microservices (Contrats)

### 📢 Groupe 5 (Notifications) - Via KAFKA
Conformément au contrat G5 v3.1, nous n'utilisons plus OpenFeign.
- **Protocole** : Kafka (Topics : `payment.notification`).
- **Événements publiés** : `PAYMENT_METHOD_OTP`, `PAYMENT_SUCCESS`, `PAYMENT_FAILED`, `PAYMENT_CANCELLED`, `INVOICE_GENERATED`.
- **Structure** : Le DTO `NotificationRequest` a été adapté pour que `eventType` soit à la RACINE, et les données brutes dans l'objet `metadata`.

### 🎟️ Groupe 1 (Billetterie) - Callbacks REST (RestTemplate)
- **Remboursement** : Lorsqu'un remboursement de type `TICKET` (sourceType) est validé, G6 appelle `POST http://g1-user-service:8081/tickets/remboursement/confirmation` (via RestTemplate) pour prévenir G1.

### 📅 Groupe 2 (Abonnement) - Callbacks REST (RestTemplate)
- **Confirmation Paiement** : Lorsqu'un paiement de `SUBSCRIPTION` passe en SUCCESS, G6 notifie G2 via `POST http://service-abonnement:8082/abonnements/paiement/confirmation`.
- **Remboursement** : G6 notifie G2 via `POST http://service-abonnement:8082/abonnements/remboursement/confirmation`.

## 📦 5. Liste des Endpoints Principaux (Disponibles dans Swagger)

- **Comptes de test (Simulation Bancaire)** :
  - `GET /test-cards` : Liste des cartes bancaires de test (avec PIN/CVV).
  - `GET /test-mobile-money-accounts` : Liste des comptes mobiles de test.
- **Paiements** :
  - `POST /payments` : Effectue un paiement (utilise le JWT, génère un Invoice si SUCCESS, appelle G2 si Subscription).
  - `GET /payments/{id}` : Vérifie l'état d'un paiement.
- **Remboursements** :
  - `POST /payments/{id}/refund` : Demande un remboursement (appelle G1 ou G2 en fonction du `sourceType`).
- **Facturation** :
  - `GET /invoices/{id}` : Récupère une facture.
