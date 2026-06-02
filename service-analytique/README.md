# SGITU Analytics Service (service-analytique)

> **Status: 🟢 Integration Complete**  
> The `integration/merged-analytique` branch has been fully merged into `main`. All endpoints are secured with JWT authentication, Kafka topics are fully integrated with the pipeline, and all automated end-to-end integration tests are passing.

This is the main analytics engine for the SGITU microservices architecture. It is responsible for aggregating data from various sources (ticketing, incidents, vehicles, revenue, subscriptions), detecting threshold breaches, and calling external Machine Learning services to generate predictions.

## Features Implemented

### 1. Data Aggregations
The service runs scheduled jobs every 60 seconds to compute various metrics from the `incoming_events` collection. Results are stored in the `stat_snapshots` collection.
*   **Incidents (INC_*)**: Total incidents, incidents by type, by zone, average resolution time, repeat incident zones.
*   **Vehicles (VEH_*)**: Active vehicles count, average punctuality rate, delay distribution, utilization rate, average speed by line.
*   **Ticketing / Frequency (FREQ_*)**: Total validations, peak hour distribution, peak hours, average daily passengers, line usage ranking, station footfall, weekend vs weekday ratio.
*   **Revenue (REV_*)**: Total revenue, revenue by ticket type, average revenue per passenger, payment method breakdown, revenue trend.
*   **Subscriptions (SUB_*)**: Active subscriptions, new subscriptions, renewal rate, churn rate, subscription type distribution.

### 2. Alerts System
*   **ThresholdAlertService**: Monitors incoming events and aggregations on every scheduler tick (every 60 seconds). If specific business rules are violated, it immediately sends targeted notifications to the **G5 Notification Service** via the API Gateway (`http://api-gateway:8080/api/notifications/send`). *(Note: The G5 service may not be running locally, so connection refused errors in the logs are expected if it is offline).*

#### Configured Thresholds & Triggers:
The service evaluates the following 5 critical thresholds:

1.  **Punctuality Drop (`PUNCTUALITY_ALERT`)**
    *   **Trigger**: When the average fleet punctuality (`VEH_PUNCTUALITY`) drops below **80%**.
    *   **Action**: Sends a `WARNING` via EMAIL & PUSH to **OPERATORS**.
2.  **High Incident Volume (`HIGH_INCIDENT_VOLUME`)**
    *   **Trigger**: When the total daily incidents (`INC_TOTAL`) exceed **10**.
    *   **Action**: Sends a `WARNING` via EMAIL & SMS to **SUPERVISORS**.
3.  **High Churn Rate (`HIGH_CHURN_RATE`)**
    *   **Trigger**: When the subscription churn rate (`SUB_CHURN`) exceeds **15%**.
    *   **Action**: Sends a `WARNING` via EMAIL to **MANAGEMENT**.
4.  **Low Daily Revenue (`LOW_DAILY_REVENUE`)**
    *   **Trigger**: Calculates the 30-day average for `REV_TOTAL`. Triggers if today's revenue falls below **70% of the 30-day average**.
    *   **Action**: Sends a `WARNING` via EMAIL to **MANAGEMENT**.
5.  **Repeat Incident Zones (`INCIDENT_ZONE_RISK`)**
    *   **Trigger**: When there is **at least 1** zone flagged for repeated incidents (`INC_REPEAT_ZONES` > 0).
    *   **Action**: Sends a `CRITICAL` alert via EMAIL, SMS & PUSH to **OPERATORS & SUPERVISORS**.
The `MlPredictionService` integrates with the standalone Python ML microservice (`g8-ml-service`).
*   **Peak Hours Prediction (PRED_01)**: Queries validated ticket events from the last 30 days, aggregates validation counts by hour, and sends the data to the ML service to predict peak hours.
*   **Incident Prediction (PRED_02)**: Queries incident events from the last 30 days, groups them by zone to calculate severity and incident counts, and sends the data to the ML service to predict at-risk zones.
*   Both prediction results are saved as `PREDICTION` snapshots in the `stat_snapshots` MongoDB collection.

---

## Architecture

The environment is containerized and consists of these services:
1.  **`g8-analytics-service`**: Spring Boot Java application (host port **8088**).
2.  **`g8-ml-service`**: Python FastAPI ML service (internal only — not exposed on the host).
3.  **`g8-mongo`**: MongoDB (port **27017**).
4.  **`g8-prometheus`**: Metrics scraper (internal).
5.  **`g8-grafana`**: Dashboards (host port **3000**, login `admin` / `sgitu2026`).

---

## How to Run the Project

The easiest way to run the entire stack is using Docker Compose. Ensure you have Docker and Docker Compose installed.

1.  Navigate to the `service-analytique` directory.
2.  Run the following command to build and start all containers:

```bash
docker-compose up --build
```

You should see logs indicating that MongoDB has started, the Python ML service is running, and the Spring Boot application has started successfully.

### 5. Monitoring (Prometheus + Grafana)
*   **Prometheus metrics:** `http://localhost:8088/actuator/prometheus` (after the stack is up).
*   **Grafana dashboard:** `http://localhost:3000` — provisioned dashboard **SGITU — Analytique G8**.
*   Incident zones use GPS coordinates rounded to 2 decimals (e.g. `33.57,-7.59`).

---

## How to Verify It's Working

### 1. Check Container Status
Ensure all three containers are running: `g8-analytics-service`, `g8-ml-service`, and `g8-mongo`.

### 2. Observe the Scheduler Logs
Every 60 seconds, the Spring Boot app will log the execution of the `ScheduledAnalyticsJob`. Look for lines like:
```text
INFO: ScheduledAnalyticsJob started
INFO: Computing INC_01 total_incidents
...
INFO: ScheduledAnalyticsJob finished
```

### 3. Seed Test Data for ML Predictions
By default, the `incoming_events` collection is empty. The ML prediction service will log warnings and skip execution if there is no data:
```text
WARN: No validated ticket data found in the last 30 days — skipping PRED_01
WARN: No incident data found in the last 30 days — skipping PRED_02
```

To test the ML integration, you need to insert some test events into MongoDB. Open a new terminal and run:

```bash
docker exec -it g8-mongo mongosh g8_analytics --eval '
db.incoming_events.insertMany([
  {
    sourceType: "TICKETING",
    payload: { status: "validated" },
    timestamp: new Date(),
    processed: false
  },
  {
    sourceType: "TICKETING",
    payload: { status: "validated" },
    timestamp: new Date(Date.now() - 3600000),
    processed: false
  },
  {
    sourceType: "INCIDENT",
    payload: { zone: "ZONE_A", severity: "HIGH" },
    timestamp: new Date(),
    processed: false
  },
  {
    sourceType: "INCIDENT",
    payload: { zone: "ZONE_B", severity: "CRITICAL" },
    timestamp: new Date(),
    processed: false
  }
]);
'
```

### 4. Verify ML Prediction Snapshots
After seeding the data, wait for the next scheduler tick (up to 60 seconds). You should now see success logs instead of warnings:
```text
INFO: PRED_01 (peak hours prediction) saved successfully
INFO: PRED_02 (incident zone prediction) saved successfully
```

You can verify that the snapshots were correctly saved in MongoDB by running:

```bash
docker exec -it g8-mongo mongosh g8_analytics --eval "db.stat_snapshots.find({statId: {\$in: ['PRED_01', 'PRED_02']}}).pretty()"
```

You should see two documents with `snapshotType: 'PREDICTION'` containing the responses from the Python ML service in their `metadata` fields.
