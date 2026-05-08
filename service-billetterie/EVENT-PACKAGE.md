# Documentation du Package `event` (Service Billetterie)

## 1. Vue d'ensemble
Le package `event` sert de **couche de communication asynchrone** pour le `service-billetterie`. Son rôle principal est de publier des événements Kafka de manière non-bloquante à chaque fois qu'une action métier importante se produit sur un ticket (émission, validation, annulation, transfert, etc.). Ces événements sont ensuite consommés par les autres microservices de l'écosystème (comme les Notifications, le Paiement ou l'Analytique).

---

## 2. Architecture du Package

Le package est découpé de manière claire pour isoler les responsabilités :

- **`interfaces/`** : 
  - `BaseEvent` : Interface commune à tous les événements. Elle impose la présence de l'horodatage (`getTimestamp()`) et du type d'événement (`getEventType()`).
  - `EventPublisher<T>` : Interface d'abstraction qui définit le contrat pour publier des événements, rendant le code modulaire.

- **`config/`** :
  - `KafkaProducerConfig` : Configuration Spring pour configurer le `KafkaTemplate`. Elle configure l'adresse du broker et, point très important, applique un sérialiseur JSON (`JsonSerializer`) pour s'assurer que les événements voyagent au format JSON lisible sur le réseau.
  - `KafkaTopics` : Dictionnaire qui centralise tous les noms de topics sous forme de constantes `static final String`. Cela prévient les fautes de frappe silencieuses.

- **`publisher/`** :
  - `KafkaEventPublisher` : L'implémentation "Fire-And-Forget". Elle publie les événements sur Kafka en utilisant des `CompletableFuture` pour ne **jamais bloquer** le fil d'exécution (Thread) principal. Elle gère également le logging propre des succès et échecs.

- **`events/`** : 
  - Contient les 11 modèles de données (POJO) enrichis par Lombok. Ils représentent précisément le contrat JSON attendu par les consommateurs.

---

## 3. Liste exhaustive des Événements (`events/`)

> [!IMPORTANT]
> **Contrat G5 - Notifications** : Le nommage des champs à l'intérieur de ces événements (ex: `userId`, `recipientId`, `expiresAt`) respecte strictement le template imposé par le groupe G5 pour garantir la bonne distribution des notifications (Email, SMS, Push).

| Événement | Topic Kafka (Constante) | Description de l'action déclencheuse |
|---|---|---|
| **`TicketIssuedEvent`** | `TICKET_ISSUED` | Un passager vient d'acheter ou d'obtenir un nouveau ticket. |
| **`TicketValidatedEvent`** | `TICKET_VALIDATED` | Le ticket a été présenté à une borne/tourniquet et a été scanné (QR, face, etc.) avec succès. |
| **`TicketFlaggedEvent`** | `TICKET_FLAGGED` | Anomalie détectée lors de la validation (ticket déjà utilisé, hors horaire, token invalide). Le ticket est signalé. |
| **`TicketFlagReviewedEvent`** | `TICKET_FLAG_REVIEWED` | Un administrateur ou un processus automatique a examiné un flag et rendu une décision (fraude confirmée ou faux positif). |
| **`TicketCancelledEvent`** | `TICKET_CANCELLED` | Annulation volontaire d'un billet par le passager, le système ou un admin. |
| **`TicketTransferInitiatedEvent`**| `TICKET_TRANSFER_INITIATED` | Un utilisateur lance le processus pour donner son billet à quelqu'un d'autre (avec une expiration de validité pour le transfert). |
| **`TicketTransferCompletedEvent`**| `TICKET_TRANSFER_COMPLETED` | Le destinataire a accepté le transfert. Le billet change de propriétaire avec succès. |
| **`TicketTransferCancelledEvent`**| `TICKET_TRANSFER_CANCELLED` | Le transfert a échoué (refusé par le destinataire, annulé par l'expéditeur, ou délai expiré). |
| **`TicketRefundRequestedEvent`**| `TICKET_REFUND_REQUESTED`| L'utilisateur a émis une demande formelle pour se faire rembourser son ticket. |
| **`TicketRefundedEvent`** | `TICKET_REFUNDED` | Le remboursement a été approuvé et les fonds ont été restitués. |
| **`TicketExpiredEvent`** | `TICKET_EXPIRED` | La durée de validité du ticket est passée, il ne peut plus être utilisé. |

---


