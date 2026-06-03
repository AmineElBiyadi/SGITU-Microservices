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

## Stage 3: Start G5 Manually (THIS DOSEN'T WORK BECAUSE G5 DOCKERFILE HAS A PROBLEM)

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


