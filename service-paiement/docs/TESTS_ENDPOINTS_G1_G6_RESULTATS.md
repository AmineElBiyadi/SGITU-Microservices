# Resultats des tests G6 Paiement et integration G1

Date de test : 02/06/2026

## 1. Environnement teste

Service G6 teste :

```text
http://localhost:18086
```

Cette instance locale utilise le code modifie de `service-paiement`.

Dependances utilisees :
- MySQL Docker `payment-db` sur le port `3316`.
- Kafka Docker `kafka-payment` sur le port `29093`.
- Zookeeper Docker `zookeeper-payment`.

Etat verifie :
- DB MySQL : `UP`.
- Kafka : `UP`.
- Service G6 local : `UP`.

Le conteneur Docker existant `payment-service` sur `8086` n'a pas ete utilise pour les tests finaux, car il tourne avec une ancienne image et reste marque `unhealthy`.

## 2. Resume global

Resultat final :

```text
35 tests passes / 35 tests executes
0 echec
```

Rapports generes pour G6 :

```text
target/test-run/endpoint-test-results.json
target/test-run/endpoint-test-results.md
```

Rapports generes pour la liaison reelle G1 -> G6 :

```text
target/test-run/g1-link-test-results.json
target/test-run/g1-link-test-results.md
```

## 3. Endpoints G6 testes avec succes

### Health et DB

| Endpoint | Resultat |
|---|---:|
| `GET /health` | 200 |
| `GET /actuator/health` | 200 |

Le health actuator confirme la connexion MySQL :

```text
db.status = UP
database = MySQL
```

### Donnees de test

| Endpoint | Resultat |
|---|---:|
| `GET /test-cards` | 200 |
| `GET /test-mobile-money-accounts` | 200 |

### Moyens de paiement et OTP

| Endpoint | Resultat |
|---|---:|
| `GET /payment-accounts/user/1` | 200 |
| `GET /payment-accounts/id/1` | 200 |
| `POST /payment-accounts/card` | 201 |
| `POST /payment-accounts/{id}/verify-otp` | 200 |
| `POST /payment-accounts/mobile-money` | 201 |
| `POST /payment-accounts/{id}/verify-otp` | 200 |
| `DELETE /payment-accounts/id/{id}` | 204 |

Verification DB :

```text
payment_otps.status = VERIFIED
payment_accounts supprime = 0 ligne restante
```

### Paiement

| Endpoint | Resultat |
|---|---:|
| `POST /payments` sans JWT | 201 |
| `GET /payments/{paymentId}` | 200 |
| `GET /payments/user/{userId}` | 200 |
| `PUT /payments/{paymentId}/cancel` | 200 |

Verification DB :

```text
payments.status = SUCCESS
payments.status = CANCELLED pour le paiement PENDING de test
```

### Factures

| Endpoint | Resultat |
|---|---:|
| `GET /payments/{paymentId}/invoice` | 200 |
| `GET /invoices/{invoiceId}` | 200 |
| `GET /invoices/number/{invoiceNumber}` | 200 |
| `GET /invoices/user/{userId}` | 200 |

Verification DB :

```text
invoice creee pour le paiement teste = oui
```

### Remboursement

| Endpoint | Resultat |
|---|---:|
| `POST /payments/{paymentId}/refund` sans JWT | 201 |
| `GET /refunds/{refundId}` | 200 |
| `GET /refunds/payment/{paymentId}` | 200 |
| `GET /refunds/user/{userId}` | 200 |

Verification DB :

```text
refunds.status = REFUNDED
```

### Notification Kafka

| Endpoint | Resultat |
|---|---:|
| `GET /test/notification-format/{paymentId}` | 200 |
| `POST /test/notification/{paymentId}` | 200 |

Les logs confirment l'envoi Kafka :

```text
Evenement de notification Kafka envoye avec succes
```

## 4. Liaison avec G1 Billetterie

### Test runtime de G1

G1 a ete demarre localement sur :

```text
http://localhost:18081
```

Configuration utilisee :

```text
MONGO_URI=mongodb://admin:change_me@localhost:27017/billetterie?authSource=admin
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:29093
PAYMENT_SERVICE_URL=http://localhost:18086
```

Compilation G1 :

```text
./mvnw.cmd -q -DskipTests compile
```

Resultat avec le `pom.xml` actuel de G1 :

```text
echec : release version 25.0.2 not supported
```

Compilation utilisee pour le test runtime :

```text
./mvnw.cmd -q -DskipTests "-Djava.version=21" compile
```

Resultat :

```text
OK
```

Raison :

G1 utilise `List.getLast()` et `List.removeLast()`, donc Java 21 est necessaire. Le `pom.xml` de G1 ne fixe pas correctement `java.version`.

Dependances G1 utilisees :
- MongoDB Docker `billetterie-mongo`.
- Kafka Docker `kafka-payment`.
- G6 local `http://localhost:18086`.

Resultat du test G1-G6 :

```text
7 tests passes / 7 tests executes
0 echec technique
```

Tests executes :

| Test | Resultat |
|---|---:|
| `GET /actuator/health` cote G1 | 200 |
| `GET /api/v1/tickets/{ticketId}` pour ticket CREATED | 200 |
| `GET /api/v1/tickets/{ticketId}` pour ticket ISSUED | 200 |
| `POST /api/v1/tickets/{ticketId}/pay` avec DTO actuel G1 | 422 attendu |
| `POST /api/v1/tickets/{ticketId}/refund` via G6 | 200 |
| Verification Mongo : ticket passe en `REFUNDED` | OK |
| Verification MySQL G6 : ligne `refunds` creee | OK |

Conclusion runtime :

Le remboursement G1 -> G6 fonctionne maintenant avec l'endpoint compatible ajoute cote G6.

Le paiement G1 -> G6 ne fonctionne pas encore avec le code actuel G1, car G1 envoie toujours un payload incomplet vers `POST /payments`.

### Test du payload actuel de G1

Payload equivalent au DTO actuel G1 :

```json
{
  "userId": "1",
  "sourceType": "TICKET",
  "paymentMethod": "CARD",
  "savedPaymentToken": "CARD-TOKEN-001"
}
```

Resultat G6 :

```text
400 Bad Request
```

Ce resultat est attendu.

Raison :
- `sourceId` manque.
- `amount` manque.
- `email` manque.

Conclusion :

La creation de paiement depuis le code actuel G1 ne peut pas reussir tant que G1 n'envoie pas ces champs.

### Test du remboursement actuel de G1

Endpoint appele actuellement par G1 :

```text
POST /payments/{ticketId}/cancel
```

Resultat G6 apres correction :

```text
201 Created
status = REFUNDED
```

Conclusion :

Le remboursement actuel de G1 est maintenant supporte cote G6, a condition que le paiement initial ait ete cree avec :

```json
"sourceId": "<ticketId>"
```

## 5. Points corriges pendant les tests

### OTP

Probleme trouve :

`paymentAccountId` etait valide dans le body avant d'etre rempli depuis le path.

Correction :

```text
VerifyOtpRequest ne valide plus paymentAccountId dans le body.
```

Le body attendu peut etre simplement :

```json
{
  "otpCode": "123456"
}
```

### Endpoint compatible G1

Probleme trouve :

`POST /payments/{ticketId}/cancel` pouvait echouer si l'appel arrivait sans body JSON explicite.

Correction :

```text
Le body est maintenant optionnel et parse seulement s'il existe.
```

### Validation API

Probleme trouve :

Les erreurs `@Valid` n'avaient pas de handler explicite.

Correction :

```text
MethodArgumentNotValidException retourne maintenant 400 Bad Request avec les champs invalides.
```

## 6. Conclusion finale

Cote G6, les endpoints principaux, la DB, Kafka, OTP, paiement, facture, remboursement et compatibilite remboursement G1 sont valides.

Le seul blocage restant pour une liaison G1 -> G6 complete est cote G1 :

```text
POST /payments doit envoyer sourceId, amount et email.
```

Sans ces champs, G6 rejette correctement la requete en `400 Bad Request`.
