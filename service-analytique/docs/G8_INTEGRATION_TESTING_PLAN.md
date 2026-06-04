# G8 Analytics Integration Testing Plan

This plan is staged on purpose. Each stage assumes you start the required services manually, then run the matching script to verify that stage.

Do not run `service-analytique/docker-compose.yml` for integration tests. Use the root `docker-compose.yml`.

## Stage 1: Start G8 Manually

Start only the G8 runtime and the shared infrastructure it needs:

```powershell
docker compose up -d --build kafka g8-mongo g8-ml-service g8-analytics-service
```

Then run the G8 integration checks:

```powershell
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-root-integration.ps1
```

What this script checks:

| Area | Result |
|---|---|
| `.env` | Reads the effective root `.env`; if `JWT_SECRET` appears more than once, the last value is used. |
| JWT | Generates a fresh HS256 token using the effective shared secret. |
| Compose | Validates the root compose model. |
| G8 runtime | Checks Kafka, private Mongo, private ML service, G8 health, and internal service-to-service connectivity. |
| Security | Confirms unauthenticated access is rejected and authenticated access works. |
| Ingestion | Sends REST ingestion payloads with current timestamps. |
| Kafka | Publishes direct compatibility messages to G8 topics and verifies G8 consumes them. |
| Analytics | Runs the analytics job and verifies snapshots/metrics. |
| Prometheus | Checks the G8 `/actuator/prometheus` endpoint. If global Prometheus is not running, the global target check is skipped. |


If you want to verify the global Prometheus target in the same stage, start Prometheus after G8 is already running:

```powershell
docker compose up -d --no-deps prometheus
```

`--no-deps` is important here because the current root compose dependency graph can otherwise start extra services that G8 does not need for Stage 1.


## Stage 2: Start G3 Manually

After Stage 1 passes, start G3:

```powershell
docker compose up -d --build g3-users-db redis user-service
```

Then run the G3-to-G8 Kafka test:

```powershell
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g3-user-events.ps1
```

What this script checks:

| Area | Result |
|---|---|
| G3 readiness | Checks G3 database and `user-service`. |
| G8 readiness | Checks Kafka, G8 Mongo, and G8 Analytics. |
| Real G3 action | Creates a real user with `POST /api/users`. |
| Kafka proof | Searches `g8-user-events` for that created user ID. |
| G8 proof | Checks whether G8 stored the user event in Mongo. |
| Diagnosis | Separates "G3 did not publish" from "G3 published but G8 did not consume/persist." |

Current G3 behavior:

```text
POST /api/users publishes to g8-user-events (single JSON object).
PUT /api/users/{id}/deactivate should publish inactive.
PUT /api/users/{id}/activate should publish active.

Payload format (G3 sends, without schemaVersion):
{
  "userId": "3",
  "action": "active",
  "timestamp": "2026-06-01T05:15:42Z"
}

G8 receives and automatically adds schemaVersion=1 for validation compatibility.
```

The script uses `POST /api/users` because it is the simplest real action and does not require an admin token.

**Status: ✅ PASSING** - All 10 tests pass. End-to-end flow works correctly.

## Stage 3: Start G5 Manually

After Stage 2 passes, start G5:

```powershell
docker compose up -d --build mysql-notification notification-service
```

Then run the G8-to-G5 alert test:

```powershell
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g5-alert-integration.ps1
```

What this script checks:

| Area | Result |
|---|---|
| G5 readiness | Checks `mysql-notification` and `notification-service`. |
| Baseline | Counts existing G5 notifications from `G8_ANALYTICS`. |
| Threshold data | Sends delayed vehicle events and repeated critical incidents to G8. |
| Alert trigger | Runs the G8 analytics job immediately. |
| G8 proof | Checks `sgitu_alerts_triggered_total` for expected alerts. |
| G5 proof | Checks the G5 MySQL `notifications` table before and after. |
| Diagnosis | Tells whether the failure is G8 not triggering, or G5 not accepting/persisting. |

Expected alert types:

```text
PUNCTUALITY_ALERT
HIGH_INCIDENT_VOLUME
INCIDENT_ZONE_RISK
```

Current G5 behavior:

```text
G5 Docker container crash-loops instantly on startup due to missing env properties and firebase-adminsdk.json being a directory instead of a file in the compose mount.
```

**Status: FAIL (Sender Side)** - Run this stage after G5 resolves their Docker startup issues.

## Stage 4: Start One Sender At A Time

Stage 4 follows the same sequence used for G3:

```text
1. Start G8 and shared infrastructure.
2. Start exactly one sender service and its private database.
3. Run that sender's PowerShell script.
4. Read the script diagnosis before starting the next sender.
```

Keep Stage 4 one-sender-at-a-time on purpose. It makes topic drift, schema drift, and missing producer wiring much easier to isolate.

### Stage 4.1 Ticketing

Start the sender:

```powershell
docker compose up -d --build service-billetterie billetterie-mongo
```

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g1-ticketing-events.ps1
```

What this script checks:

| Area | Result |
|---|---|
| Sender readiness | Checks `service-billetterie` and `billetterie-mongo`. |
| Real action | Creates a ticket, then calls the validation endpoint. |
| Kafka proof | Searches `ticket.validated` for the created ticket ID. |
| G8 proof | Checks `incoming_events` for `sourceType: TICKETING`. |
| Analytics proof | Runs G8 analytics and checks `FREQ_TOTAL_VALIDATIONS`. |

**Status: FAIL (Sender Side)** - The `service-billetterie` fails to build in Docker (`error: release version 21.0.11 not supported`).

### Stage 4.2 Subscriptions

Start the sender:

```powershell
docker compose up -d abonnement-service db-abonnement
```

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g2-subscription-events.ps1
```

What this script checks:

| Area | Result |
|---|---|
| Sender readiness | Checks `service-abonnement` and `db-abonnement`. |
| Real action | Creates a plan, then calls `/abonnements/souscrire`. |
| Kafka proof | Searches `abonnement.souscription` for the test user/subscription. |
| G8 proof | Checks `incoming_events` for `sourceType: SUBSCRIPTION`. |
| Analytics proof | Runs G8 analytics and checks `SUB_NEW`. |

Known risk to watch:

```text
service-abonnement also stores AnalytiqueTrace and has a scheduler/REST path to /api/events/batch.
If the script sees a notification-shaped event but no G8 persistence, the issue is sender-side contract/topic drift, not a G8 crash.
```

**Status: FAIL (Sender Side)** - The `service-abonnement` successfully reaches `g3-user-service` but receives a `401 Unauthorized` because its `UtilisateurServiceClient` (Feign) does not propagate the JWT authentication header.

### Stage 4.3 Payments

Start the sender:

```powershell
docker compose up -d --build payment-service g6-payment-db
```

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g6-payment-events.ps1
```

What this script checks:

| Area | Result |
|---|---|
| Sender readiness | Checks `g6-payment-service` and `g6-payment-db`. |
| Real action | Calls `POST /payments` with a test payment request. |
| Kafka proof | Searches `payment.transaction.completed`, the G8 analytics topic. |
| Extra sender proof | Also searches `payment.notification`, because current G6 code publishes notification events there. |
| G8 proof | Checks `incoming_events` for `sourceType: PAYMENT`. |
| Analytics proof | Runs G8 analytics and checks `REV_TOTAL`. |

Known risk to watch:

```text
Current G6 code publishes payment notifications to payment.notification.
G8 listens for analytics events on payment.transaction.completed.
If only payment.notification has data, the diagnosis is topic drift on the sender side.
```

**Status: FAIL (Sender Side)** - The `payment-service` fails to build in Docker (`mvn dependency:go-offline` exits with code 1).

### Stage 4.4 Vehicle Tracking

Start the sender:

```powershell
docker compose up -d --build g7-service db-g7
```

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g7-vehicle-events.ps1
```

What this script checks:

| Area | Result |
|---|---|
| Sender readiness | Checks `g7-service` and `db-g7`. |
| Real action | Creates a vehicle, sets it `EN_SERVICE`, then posts a GPS position. |
| Kafka proof | Searches `g8.vehicule.status`, the G8 analytics topic. |
| Extra sender proof | Also searches `vehicule-positions`, because current G7 position flow publishes telemetry there. |
| G8 proof | Checks `incoming_events` for `sourceType: VEHICLE`. |
| Analytics proof | Runs G8 analytics and checks `VEH_PUNCTUALITY`. |
| Alert observation | Reads the Prometheus alert counter for `PUNCTUALITY_ALERT` as non-blocking context. |

Known risk to watch:

```text
Current G7 code has an envoyerStatusG8 method, but normal vehicle/status/position endpoints may not call it.
If vehicule-positions has data but g8.vehicule.status does not, the issue is missing sender wiring.
```

**Status: FAIL (Sender Side)** - The `g7-service` successfully starts and creates the vehicle (test script race condition fixed), but it never publishes the event to `g8.vehicule.status`. The G7 team implemented the method `KafkaProducerService.envoyerStatusG8` but completely forgot to invoke it in their business logic (e.g., `VehiculeService`).

### Stage 4.5 Incidents

The incident service is not wired into the root compose file yet. Start it separately and make sure its Kafka bootstrap points at the shared broker:

```text
From host machine: localhost:29093
From inside root compose network: kafka:9092
```

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g7-incident-events.ps1
```

What this script checks:

| Area | Result |
|---|---|
| G8 readiness | Checks Kafka, G8 Mongo, and G8 Analytics. |
| Real action | Calls `POST /api/incidents/signaler` with a passenger JWT and user headers. |
| Kafka proof | Searches `incident.analytique.topic`. |
| G8 proof | Checks `incoming_events` for `sourceType: INCIDENT`. |
| Analytics proof | Runs G8 analytics and checks `INC_TOTAL`. |
| Alert observation | Reads the Prometheus alert counter for `INCIDENT_ZONE_RISK` as non-blocking context. |

### Stage 4 Interpretation Rules

Use the same diagnosis model for every sender:

| Result | Likely owner |
|---|---|
| Real action fails | Sender service or its own dependency/precondition. |
| Real action succeeds, expected Kafka topic is empty | Sender-side producer wiring or topic name drift. |
| Expected Kafka topic has the test event, but G8 Mongo is empty | G8 consumer config, G8 validation, or payload contract drift. |
| G8 Mongo has the event, but snapshot does not change | G8 aggregation logic, event timestamp/window, or event status semantics. |
| Snapshot changes, alert missing | Threshold not crossed, alert rule window mismatch, or G5 unavailable. |

Current G8 compatibility behavior:

```text
Kafka listeners auto-add schemaVersion=1 before validation.
Kafka listeners accept both a single JSON object and a batch/list.
G8 normalizes known legacy sender fields for ticketing, vehicles, payments, subscriptions, and incidents.
REST ingestion still expects callers to respect the documented contract.
```

