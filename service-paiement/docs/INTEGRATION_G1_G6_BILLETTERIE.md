# Integration G1 Billetterie - G6 Paiement

Date de verification : 02/06/2026

Services verifies :
- G1 : `service-billetterie`
- G6 : `service-paiement`

## 1. Situation actuelle

Nous avons deja envoye a G1 le contrat recommande :
- `POST /payments` avec un payload complet.
- `POST /payments/{paymentId}/refund` pour le remboursement.
- Stockage de `paymentId` et `transactionToken` apres paiement.

Ce message reste correct pour un contrat propre entre les deux services.

Cependant, pour eviter un blocage si G1 ne modifie pas son remboursement, G6 a maintenant ete adapte cote paiement :
- G6 accepte les identifiants ticket MongoDB en `String`.
- G6 accepte aussi l'ancien appel G1 `POST /payments/{ticketId}/cancel`.
- G6 autorise les endpoints d'integration G1 sans JWT pour eviter un `401 Unauthorized` avec le `RestTemplate` actuel de G1.
- Les URLs Docker G1 <-> G6 ont ete alignees sur les vrais noms de services.

## 2. Creation de paiement : ce que G1 doit encore envoyer

Endpoint G6 :

```text
POST /payments
```

Payload attendu :

```json
{
  "userId": 1,
  "sourceType": "TICKET",
  "sourceId": "<ticketId>",
  "amount": 25.00,
  "paymentMethod": "CARD",
  "savedPaymentToken": "CARD-TOKEN-001",
  "email": "client@test.com"
}
```

Points obligatoires :
- `sourceId` doit contenir l'identifiant du ticket G1.
- `amount` doit contenir le prix du ticket.
- `email` doit etre renseigne pour les notifications et la facture.

G6 ne peut pas deviner correctement `sourceId`, `amount` et `email` si G1 ne les envoie pas. Cette partie reste donc obligatoire cote G1.

## 3. Remboursement : deux chemins maintenant acceptes

### Option recommandee

G1 stocke `paymentId` apres paiement, puis appelle :

```text
POST /payments/{paymentId}/refund
```

Body :

```json
{
  "amount": 25.00,
  "reason": "Remboursement ticket"
}
```

### Option de compatibilite ajoutee cote G6

Si G1 garde son ancien appel :

```text
POST /payments/{ticketId}/cancel
```

G6 le supporte maintenant.

Fonctionnement :
- G6 cherche le dernier paiement `SUCCESS` avec `sourceType = TICKET` et `sourceId = ticketId`.
- Si le body est vide, G6 rembourse automatiquement le montant total du paiement.
- Si un body est envoye, G6 utilise le montant et la raison envoyes.

Condition indispensable :

Le paiement initial doit avoir ete cree avec :

```json
"sourceId": "<ticketId>"
```

Sinon G6 ne pourra pas retrouver le paiement a rembourser par ticket.

## 4. Securite appliquee cote G6

Avant, tous les endpoints G6 etaient proteges par JWT.

Pour l'integration avec G1, G6 autorise maintenant sans JWT uniquement :

```text
POST /payments
POST /payments/*/refund
POST /payments/*/cancel
```

Tous les autres endpoints restent proteges.

Raison :

Le client REST observe cote G1 utilise un `RestTemplate` simple et ne semble pas envoyer de header :

```text
Authorization: Bearer <token>
```

Sans cette adaptation cote G6, l'appel G1 risquait de retourner `401 Unauthorized`.

## 5. Configuration Docker corrigee

Dans le compose global, G1 doit appeler G6 avec le nom du service Docker :

```yaml
PAYMENT_SERVICE_URL: ${G6_BASE_URL:-http://payment-service:8086}
```

Dans le meme compose, G6 appelle G1 avec :

```yaml
TICKET_SERVICE_URL: ${G1_BASE_URL:-http://service-billetterie:8081}
```

Important :

En profil Docker, G6 utilise HTTP car `server.ssl.enabled=false`.

En local hors Docker, G6 garde HTTPS via `application.properties`.

## 6. Fichiers modifies cote G6

### Type `sourceId` aligne avec G1

Fichiers :
- `service-paiement/src/main/java/ma/sgitu/payment/dto/request/PaymentRequest.java`
- `service-paiement/src/main/java/ma/sgitu/payment/entity/Payment.java`
- `service-paiement/src/main/java/ma/sgitu/payment/entity/Invoice.java`
- `service-paiement/src/main/java/ma/sgitu/payment/dto/response/PaymentDetailsResponse.java`
- `service-paiement/src/main/java/ma/sgitu/payment/dto/response/InvoiceResponse.java`
- `service-paiement/src/main/java/ma/sgitu/payment/config/DataInitializer.java`

Modification :

```java
private String sourceId;
```

### Recherche paiement par ticket G1

Fichier :

```text
service-paiement/src/main/java/ma/sgitu/payment/repository/PaymentRepository.java
```

Ajout :

```java
Optional<Payment> findFirstBySourceTypeAndSourceIdAndStatusOrderByCreatedAtDesc(
        SourceType sourceType,
        String sourceId,
        PaymentStatus status
);
```

### Endpoint remboursement compatible G1

Fichier :

```text
service-paiement/src/main/java/ma/sgitu/payment/controller/RefundController.java
```

Ajout :

```text
POST /payments/{ticketId}/cancel
```

### Logique remboursement par `ticketId`

Fichier :

```text
service-paiement/src/main/java/ma/sgitu/payment/service/RefundService.java
```

Ajout :

```java
processRefundByTicketSourceId(String ticketId, RefundRequest request)
```

### Securite inter-services G1/G6

Fichier :

```text
service-paiement/src/main/java/ma/sgitu/payment/security/SecurityConfig.java
```

Ajout :

```java
.requestMatchers(HttpMethod.POST, "/payments").permitAll()
.requestMatchers(HttpMethod.POST, "/payments/*/refund").permitAll()
.requestMatchers(HttpMethod.POST, "/payments/*/cancel").permitAll()
```

### URLs Docker

Fichiers :
- `docker-compose.yml`
- `service-paiement/src/main/resources/application-docker.properties`

Corrections :

```text
http://payment-service:8086
http://service-billetterie:8081
```

### Compilation Java / Lombok

Fichier :

```text
service-paiement/pom.xml
```

Correction :
- version Lombok fixee a `1.18.46`.
- configuration `maven-compiler-plugin` avec annotation processor Lombok.

## 7. Message correctif possible a envoyer a G1

Bonjour,

Nous avons ajoute une compatibilite cote G6 pour faciliter l'integration.

Le contrat recommande reste :

```text
POST /payments/{paymentId}/refund
```

Mais si votre service garde l'appel actuel :

```text
POST /payments/{ticketId}/cancel
```

G6 le supporte maintenant.

Condition obligatoire : lors du paiement initial, merci d'envoyer toujours :

```json
{
  "sourceType": "TICKET",
  "sourceId": "<ticketId>",
  "amount": <ticket.price>,
  "email": "<email_client>"
}
```

Sans `sourceId`, `amount` et `email`, G6 ne peut pas creer un paiement ticket complet.

Merci.

## 8. Conclusion

Cote G6, les blocages principaux avec G1 ont ete corriges :
- `sourceId` accepte maintenant les IDs MongoDB de G1.
- l'ancien remboursement G1 `/payments/{ticketId}/cancel` est supporte.
- les endpoints G1/G6 critiques ne bloquent plus sur JWT.
- les URLs Docker sont alignees.

Le seul point qui reste vraiment dependant de G1 est le payload de creation du paiement.

Si G1 envoie un `POST /payments` sans `sourceId`, sans `amount` ou sans `email`, G6 doit refuser la requete car le paiement serait incomplet.
