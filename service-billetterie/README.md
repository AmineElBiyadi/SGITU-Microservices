# Service Billetterie - Paperless Ticket Management

## Table des matières

- [Prérequis](#prérequis)
- [Lancement local avec Docker Compose](#lancement-local-avec-docker-compose)
- [Lancement local sans Docker](#lancement-local-sans-docker)
- [Configuration](#configuration)
- [Accès aux services](#accès-aux-services)
- [Architecture du projet](#architecture-du-projet)

---

## Prérequis

- **Docker** et **Docker Compose** installés
- **Java 21** (pour lancement sans Docker)
- **Maven 3.9+** (pour lancement sans Docker)

---

## Lancement local avec Docker Compose

Cette méthode est recommandée car elle lance automatiquement toutes les dépendances (MongoDB, Kafka, Prometheus, Grafana).

### 1. Configurer les variables d'environnement

Copiez le fichier d'exemple et modifiez les valeurs si nécessaire :

```bash
cp .env.example .env
```

Le fichier `.env` contient :
```bash
MONGO_ROOT_USERNAME=admin
MONGO_ROOT_PASSWORD=change_me
MONGO_DATABASE=billetterie
KAFKA_CLUSTER_ID=sgitu-kafka-cluster-001
```

### 2. Lancer les services

Depuis le répertoire `service-billetterie` :

```bash
docker compose up -d
```

Cette commande démarre :
- **service-billetterie** (port 8081)
- **mongodb** (port 27017)
- **kafka** (port 9092)
- **prometheus** (port 9090)
- **grafana** (port 3000)

### 3. Vérifier les logs

```bash
docker compose logs -f service-billetterie
```

### 4. Arrêter les services

```bash
docker compose down
```

Pour supprimer également les volumes (données) :

```bash
docker compose down -v
```

---

## Lancement local sans Docker

Si vous préférez lancer les services manuellement (utile pour le développement).

### 1. Démarrer MongoDB

```bash
docker run -d --name billetterie-mongo \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=admin \
  -e MONGO_INITDB_DATABASE=billetterie \
  mongo:7
```

### 2. Démarrer Kafka

```bash
docker run -d --name billetterie-kafka \
  -p 9092:9092 \
  -e KAFKA_CLUSTER_ID=sgitu-kafka-cluster-001 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES="broker,controller" \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS="1@kafka:9093" \
  -e KAFKA_LISTENERS="PLAINTEXT://0.0.0.0:29092,PLAINTEXT_HOST://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093" \
  -e KAFKA_ADVERTISED_LISTENERS="PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092" \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP="PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT,CONTROLLER:PLAINTEXT" \
  -e KAFKA_CONTROLLER_LISTENER_NAMES="CONTROLLER" \
  -e KAFKA_INTER_BROKER_LISTENER_NAME="PLAINTEXT" \
  confluentinc/cp-kafka:7.8.7
```

### 3. Configurer l'application

Créez ou modifiez `src/main/resources/application-local.yml` :

```yaml
server:
  port: 8081

spring:
  data:
    mongodb:
      uri: mongodb://admin:admin@localhost:27017/billetterie?authSource=admin

  kafka:
    bootstrap-servers: localhost:9092
```

### 4. Lancer l'application Spring Boot

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Ou avec Maven installé :

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## Lancement dans l'architecture SGITU complète

Pour lancer le service dans le contexte de tous les microservices SGITU :

### 1. Configurer les variables à la racine

```bash
cd ..
cp .env.example .env
```

### 2. Lancer tous les services

```bash
docker compose up -d service-billetterie billetterie-mongo kafka
```

Le service sera accessible sur le port **8081** et MongoDB sur **27018** (port hôte différent pour éviter les conflits).

---

## Configuration

### Variables d'environnement principales

| Variable | Description | Défaut |
|----------|-------------|--------|
| `MONGO_ROOT_USERNAME` | Utilisateur MongoDB admin | admin |
| `MONGO_ROOT_PASSWORD` | Mot de passe MongoDB admin | change_me |
| `MONGO_DATABASE` | Nom de la base de données | billetterie |
| `KAFKA_CLUSTER_ID` | ID du cluster Kafka | sgitu-kafka-cluster-001 |
| `SERVER_PORT` | Port du service | 8081 |
| `PAYMENT_SERVICE_URL` | URL du service paiement | http://localhost:8086 |
| `COORDINATION_SERVICE_URL` | URL du service coordination | http://localhost:8084 |

### Profiles Spring

- **default** : Configuration Docker (variables d'environnement)
- **local** : Configuration pour développement local

---

## Accès aux services

Une fois démarré, vous pouvez accéder à :

- **API REST** : http://localhost:8081
- **Swagger UI** : http://localhost:8081/swagger-ui.html
- **OpenAPI JSON** : http://localhost:8081/v3/api-docs
- **Actuator Health** : http://localhost:8081/actuator/health
- **Prometheus Metrics** : http://localhost:8081/actuator/prometheus
- **Prometheus UI** : http://localhost:9090 (si Docker Compose)
- **Grafana** : http://localhost:3000 (si Docker Compose, login: admin/admin)
- **MongoDB** : mongodb://localhost:27017 (ou localhost:27018 en mode SGITU)

---

## Architecture du projet

```
com.ensate.billetterie
├── ticket        ← Spring Boot entry point, REST API, lifecycle orchestration
├── identity      ← Token generation and verification (QR, Fingerprint, Face ID)
├── validation    ← Pipeline-based ticket validation using Chain of Responsibility
└── event         ← Kafka publishing abstraction for all state-change events
```

---

## Package Responsibilities

### `ticket` — Entry Point & Orchestrator

The only Spring Boot package. Owns the REST layer and the `TicketService`, which acts as the central coordinator — it calls into `identity`, `validation`, and `event` to fulfil each operation.

**Key classes:**

| Class | Role |
|---|---|
| `TicketController` | REST endpoints: create, transfer, cancel, validate |
| `TicketService` | Orchestrates all ticket operations |
| `TicketRepository` | MongoDB persistence via `MongoRepository` |
| `Ticket` | Root document stored in the `tickets` collection |
| `TransferRecord` | Embedded subdocument — transfer history stored inside `Ticket` |
| `TicketStatus` | Enum: `ISSUED`, `TRANSFERRED`, `USED`, `CANCELLED`, `EXPIRED` |

**DTOs:**

| DTO | Direction | Purpose |
|---|---|---|
| `CreateTicketRequest` | Inbound | eventId, holderId, identityMethod, metadata |
| `TransferTicketRequest` | Inbound | toHolderId, reason |
| `ValidateTicketRequest` | Inbound | tokenValue, identityPayload |
| `TicketResponse` | Outbound | Ticket summary returned to the client |
| `ValidationResult` | Outbound | Result of a validate call — success/failure + reason |

---

### `identity` — Token Issuance & Verification

A plain Java package (no Spring). Responsible for generating and verifying identity tokens using different biometric/encoding strategies. Uses both the **Strategy** and **Factory** patterns.

**Pattern breakdown:**

- **Strategy** — `IdentityMethod` is the common interface. Each implementation (`QRCodeImpl`, `FingerprintImpl`, `FaceIDImpl`) is a different strategy for the same operation.
- **Factory** — `IdentityMethodFactory` selects the correct strategy at runtime based on `IdentityMethodType`, using a static registry map.

**Key classes:**

| Class | Role |
|---|---|
| `IdentityMethod` | Interface: `generateToken(ctx)`, `verifyToken(token, ctx)` |
| `QRCodeImpl` | Encodes payload into a QR data string; verifies by decoding |
| `FingerprintImpl` | Hashes biometric template; verifies against stored hash |
| `FaceIDImpl` | Wraps face-embedding comparison logic |
| `IdentityMethodFactory` | `create(IdentityMethodType)` — returns the correct impl from a registry map |
| `IdentityService` | Public API: `issue(type, ctx)` and `verify(token, ctx)` — delegates to factory |
| `IdentityToken` | Output of issuance: tokenValue, methodType, issuedAt, expiresAt, metadata |
| `IdentityContext` | Input to every method: holderId, eventId, rawPayload |
| `IdentityMethodType` | Enum: `QR_CODE`, `FINGERPRINT`, `FACE_ID` |

**Flow — token issuance:**
```
TicketService
  └── IdentityService.issue(QR_CODE, context)
        └── IdentityMethodFactory.create(QR_CODE)  →  QRCodeImpl
              └── QRCodeImpl.generateToken(context)  →  IdentityToken
```

**Flow — token verification (inside validation pipeline):**
```
TokenVerificationStep
  └── IdentityService.verify(token, context)
        └── IdentityMethodFactory.create(token.methodType)  →  QRCodeImpl
              └── QRCodeImpl.verifyToken(token, context)  →  boolean
```

---

### `validation` — Validation Pipeline

A plain Java package. Implements a **Chain of Responsibility** pipeline. Each step receives a mutable `ValidationContext`, performs its check, and either enriches the context or throws `ValidationException` to halt the chain.

**Key classes:**

| Class | Role |
|---|---|
| `ValidationStep` | Interface: `validate(ValidationContext)` |
| `ValidationPipeline` | Holds an ordered list of steps; `execute(ctx)` runs them in sequence |
| `ValidationContext` | Mutable object passed through all steps — carries ticketId, tokenValue, holderId, eventId, identityPayload |
| `ValidationException` | Unchecked — thrown by any step on failure; carries `failedStep` and `reason` |

**Steps (executed in this order):**

| Step | What it checks |
|---|---|
| `TicketExistenceStep` | Ticket exists in the database and is not deleted |
| `TicketStatusStep` | Status is `ISSUED` or `TRANSFERRED` — not already `USED` or `CANCELLED` |
| `ExpiryCheckStep` | `expiresAt` is in the future |
| `HolderMatchStep` | The presenter's identity matches the current `holderId` on the ticket |
| `EventActiveStep` | The associated event is currently open for entry |
| `TokenVerificationStep` | Calls `IdentityService.verify()` with the context payload |

**Pipeline execution:**
```
ValidationPipeline.execute(context)
  ├── TicketExistenceStep.validate(context)
  ├── TicketStatusStep.validate(context)
  ├── ExpiryCheckStep.validate(context)
  ├── HolderMatchStep.validate(context)
  ├── EventActiveStep.validate(context)
  └── TokenVerificationStep.validate(context)
        └── calls IdentityService.verify(...)
```

Any step can throw `ValidationException` — the pipeline stops immediately and the exception propagates up to `TicketService`, which maps it to a `ValidationResult(valid: false, failedStep: "...", message: "...")`.

---

### `event` — Kafka Publishing Abstraction

Provides a single, broker-agnostic interface for publishing domain events whenever ticket state changes. All other packages depend on this interface; none of them know about Kafka directly.

**Key classes:**

| Class | Role |
|---|---|
| `EventPublisher<T>` | Interface: `publish(topic, event)` |
| `KafkaEventPublisher<T>` | Implements `EventPublisher` — wraps `KafkaTemplate`, publishes async |
| `KafkaTopics` | Constants: `ticket.issued`, `ticket.transferred`, `ticket.validated`, `ticket.cancelled`, `ticket.expired` |
| `KafkaProducerConfig` | `@Configuration` — wires `KafkaTemplate` bean with JSON serializer |

**Events published:**

| Event | Triggered when | Key fields |
|---|---|---|
| `TicketIssuedEvent` | Ticket created | ticketId, holderId, eventId, methodType, issuedAt |
| `TicketTransferredEvent` | Ticket transferred | ticketId, fromHolder, toHolder, transferredAt |
| `TicketValidatedEvent` | Validate endpoint called | ticketId, success, failedStep, validatedAt |
| `TicketCancelledEvent` | Ticket cancelled | ticketId, holderId, cancelledAt, reason |
| `TicketExpiredEvent` | Scheduled expiry job runs | ticketId, expiredAt |

Publishing is **fire-and-forget** (non-blocking). `KafkaEventPublisher` uses `CompletableFuture.whenComplete()` to log delivery success or failure without blocking the calling thread.

---

## Full Request Flows

### Create a ticket

```
POST /tickets
  └── TicketController.createTicket(CreateTicketRequest)
        └── TicketService.createTicket(request)
              ├── IdentityService.issue(methodType, context)  →  IdentityToken
              ├── Ticket.builder()...status(ISSUED)...build()
              ├── TicketRepository.save(ticket)
              └── EventPublisher.publish("ticket.issued", TicketIssuedEvent)
```

### Validate a ticket

```
POST /tickets/{id}/validate
  └── TicketController.validateTicket(id, ValidateTicketRequest)
        └── TicketService.validateTicket(id, request)
              ├── Build ValidationContext from request
              ├── ValidationPipeline.execute(context)
              │     ├── TicketExistenceStep
              │     ├── TicketStatusStep
              │     ├── ExpiryCheckStep
              │     ├── HolderMatchStep
              │     ├── EventActiveStep
              │     └── TokenVerificationStep → IdentityService.verify()
              ├── ticket.markUsed()
              ├── TicketRepository.save(ticket)
              └── EventPublisher.publish("ticket.validated", TicketValidatedEvent)
```

### Transfer a ticket

```
PUT /tickets/{id}/transfer
  └── TicketController.transferTicket(id, TransferTicketRequest)
        └── TicketService.transferTicket(id, request)
              ├── TicketRepository.findById(id)
              ├── ticket.transferTo(newHolderId, reason)
              │     └── appends TransferRecord to transferHistory
              ├── TicketRepository.save(ticket)
              └── EventPublisher.publish("ticket.transferred", TicketTransferredEvent)
```

---

## MongoDB Document Structure

Tickets are stored in a single `tickets` collection. `TransferRecord` is embedded as a subdocument array — no separate collection.

```json
{
  "_id": "64f3a2b1c8e4d500123abc01",
  "event_id": "event-99",
  "holder_id": "user-42",
  "token_value": "QR:eyJhbGciOiJIUzI1NiJ9...",
  "identity_method": "QR_CODE",
  "status": "TRANSFERRED",
  "transfer_history": [
    {
      "from_holder": "user-42",
      "to_holder": "user-88",
      "transferred_at": "2025-04-10T14:23:00Z",
      "reason": "Gifted to friend"
    }
  ],
  "metadata": {
    "seat": "B12",
    "tier": "VIP"
  },
  "expires_at": "2025-05-01T20:00:00Z",
  "issued_at": "2025-04-01T09:00:00Z",
  "updated_at": "2025-04-10T14:23:01Z"
}
```

---

## Package Dependency Map

```
ticket  ──────────────────────────────┐
  │                                   │
  ├── depends on ──► identity         │
  ├── depends on ──► validation       │  all publish via
  └── depends on ──► event ◄──────────┘

validation ──► identity   (TokenVerificationStep calls IdentityService)
identity   ──► (none)     (pure Java, no dependencies on other packages)
event      ──► (none)     (pure abstraction + Kafka infra only)
```

> `identity` and `event` are leaf packages — they depend on nothing within the project. `validation` depends only on `identity`. `ticket` sits at the top and wires everything together.

---

## Adding a New Identity Method

1. Add the value to `IdentityMethodType` enum — e.g. `NFC`
2. Create `NFCImpl implements IdentityMethod` in `identity/implementations`
3. Register it in `IdentityMethodFactory.REGISTRY` — one line:
   ```java
   IdentityMethodType.NFC, new NFCImpl()
   ```

No other code changes needed. The factory, service, and pipeline are all closed to modification.

---

## Adding a New Validation Step

1. Create `YourStep implements ValidationStep` in `validation/steps`
2. Add it to the pipeline in `TicketService` (or wherever the pipeline is assembled):
   ```java
   pipeline.addStep(new YourStep(...));
   ```

Steps are ordered — insert at the correct position. Checks that short-circuit early (existence, status) should come before expensive checks (token verification, event lookup).