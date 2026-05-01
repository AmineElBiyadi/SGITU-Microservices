# Paperless Ticket Management Service — System Architecture

## Overview

The Paperless Ticket Management Service is a Spring Boot application that manages the full lifecycle of digital tickets — from issuance through validation and expiry. It is structured into four core packages, each with a distinct responsibility.

```
com.example.billetterie
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