# G8 Analytics Service: Data Examples Reference

This document serves as the comprehensive reference for all data structures flowing into, through, and out of the G8 Analytics Service.

## Endpoint Summary

| Method | Path | Purpose |
| :--- | :--- | :--- |
| `POST` | `/api/v1/ingestion/tickets` | Ingest ticketing events |
| `POST` | `/api/v1/ingestion/subscriptions` | Ingest subscription events |
| `POST` | `/api/v1/ingestion/payments` | Ingest payment events |
| `POST` | `/api/v1/ingestion/vehicles` | Ingest vehicle telemetry and status events |
| `POST` | `/api/v1/ingestion/incidents` | Ingest incident reports |
| `POST` | `/api/v1/ingestion/users` | Ingest user activity events |
| `GET` | `/api/v1/analytics/trips/summary` | Retrieve trips (FREQ) snapshots |
| `GET` | `/api/v1/analytics/revenue/summary` | Retrieve revenue (REV) snapshots |
| `GET` | `/api/v1/analytics/incidents/stats` | Retrieve incidents (INC) snapshots |
| `GET` | `/api/v1/analytics/vehicles/activity` | Retrieve vehicles (VEH) snapshots |
| `GET` | `/api/v1/analytics/users/stats` | Retrieve user statistics snapshots |
| `GET` | `/api/v1/analytics/subscriptions/stats` | Retrieve subscriptions (SUB) snapshots |
| `GET` | `/api/v1/analytics/dashboard` | Retrieve all current dashboard snapshots |
| `POST` | `/api/v1/analytics/reports/generate` | Trigger the generation of a new report |
| `GET` | `/api/v1/analytics/reports/{id}` | Retrieve a generated report by ID |
| `POST` | `/predict/peak-hours` | (ML Service) Predict future peak hours |
| `POST` | `/predict/incidents` | (ML Service) Predict high-risk incident zones |

---

## Section 1 — Ingestion Inputs (what other groups send us)

### TICKETING
**POST `/api/v1/ingestion/tickets`**
```json
[
  {
    "timestamp": "2026-05-03T08:30:00Z",
    "userId": "USR-98765",
    "status": "validated",
    "line": "L1",
    "stationId": "ST-05"
  },
  {
    "timestamp": "2026-05-03T08:31:12Z",
    "userId": "USR-11223",
    "status": "expired",
    "line": "L1",
    "stationId": "ST-05"
  },
  {
    "timestamp": "2026-05-03T08:35:00Z",
    "userId": "USR-55443",
    "status": "validated",
    "line": "L2",
    "stationId": "ST-10"
  }
]
```

### SUBSCRIPTION
**POST `/api/v1/ingestion/subscriptions`**
```json
[
  {
    "timestamp": "2026-05-03T09:00:00Z",
    "userId": "USR-456",
    "action": "created",
    "planType": "MONTHLY_STUDENT"
  },
  {
    "timestamp": "2026-05-03T09:15:00Z",
    "userId": "USR-789",
    "action": "renewed",
    "planType": "YEARLY_STANDARD"
  },
  {
    "timestamp": "2026-05-03T09:20:00Z",
    "userId": "USR-101",
    "action": "cancelled",
    "planType": "MONTHLY_STANDARD"
  }
]
```

### PAYMENT
**POST `/api/v1/ingestion/payments`**
```json
[
  {
    "timestamp": "2026-05-03T10:05:00Z",
    "transactionId": "TXN-001",
    "status": "completed",
    "amount": 25.50,
    "method": "CARD"
  },
  {
    "timestamp": "2026-05-03T10:10:00Z",
    "transactionId": "TXN-002",
    "status": "completed",
    "amount": 10.00,
    "method": "CASH"
  },
  {
    "timestamp": "2026-05-03T10:15:00Z",
    "transactionId": "TXN-003",
    "status": "failed",
    "amount": 15.00,
    "method": "CARD"
  }
]
```

### VEHICLE
**POST `/api/v1/ingestion/vehicles`**
```json
[
  {
    "timestamp": "2026-05-03T11:00:00Z",
    "vehicleId": "BUS_404",
    "status": "in_service",
    "line": "L1",
    "delayMinutes": 2,
    "speed": 45.2
  },
  {
    "timestamp": "2026-05-03T11:05:00Z",
    "vehicleId": "TRAM_05",
    "status": "out_of_service"
  },
  {
    "timestamp": "2026-05-03T11:10:00Z",
    "vehicleId": "BUS_112",
    "status": "in_service",
    "line": "L4",
    "delayMinutes": 5,
    "speed": 30.5
  }
]
```

### INCIDENT
**POST `/api/v1/ingestion/incidents`**
```json
[
  {
    "timestamp": "2026-05-03T12:00:00Z",
    "incidentId": "INC-1001",
    "type": "accident",
    "zone": "Z_NORTH",
    "resolutionMinutes": 45
  },
  {
    "timestamp": "2026-05-03T12:45:00Z",
    "incidentId": "INC-0998",
    "type": "delay",
    "zone": "Z_CENTER",
    "resolutionMinutes": 15
  },
  {
    "timestamp": "2026-05-03T13:00:00Z",
    "incidentId": "INC-1002",
    "type": "breakdown",
    "zone": "Z_EAST",
    "resolutionMinutes": 120
  }
]
```

### USER
**POST `/api/v1/ingestion/users`**
```json
[
  {
    "timestamp": "2026-05-03T14:00:00Z",
    "userId": "USR-111",
    "action": "active",
    "deviceOS": "IOS"
  },
  {
    "timestamp": "2026-05-03T14:05:00Z",
    "userId": "USR-112",
    "action": "active",
    "deviceOS": "ANDROID"
  },
  {
    "timestamp": "2026-05-03T14:10:00Z",
    "userId": "USR-222",
    "action": "inactive",
    "deviceOS": "WEB"
  }
]
```

### NOTE — G7 Vehicle Data (Kafka)
G7 (Suivi des véhicules) does NOT use the POST `/api/v1/ingestion/vehicles` endpoint. Vehicle telemetry data is high-frequency and time-sensitive, so G7 sends data via Kafka instead.

- **Topic name:** `vehicle-events` (to be confirmed with G7)
- **Consumer:** G8 listens to this topic internally via a Kafka consumer
- **Format:** same payload structure as the vehicle ingestion examples above

The REST endpoint `POST /api/v1/ingestion/vehicles` remains available for testing purposes only.

---

## Section 2 — Raw Events in MongoDB (incoming_events collection)

**TICKETING Event**
```json
{
  "_id": { "$oid": "6634d1b8c2c1a84f3d1b8a12" },
  "sourceType": "TICKETING",
  "sourceId": "VALIDATOR_101",
  "eventType": "TICKET_VALIDATED",
  "payload": {
    "ticketId": "TCK-98765",
    "status": "VALID",
    "scanType": "NFC"
  },
  "timestamp": { "$date": "2026-05-03T08:30:00Z" },
  "receivedAt": { "$date": "2026-05-03T08:30:01Z" },
  "lineId": "L1",
  "zoneId": "Z_CENTER",
  "processed": false
}
```

**SUBSCRIPTION Event**
```json
{
  "_id": { "$oid": "6634d1b8c2c1a84f3d1b8a13" },
  "sourceType": "SUBSCRIPTION",
  "sourceId": "PORTAL_WEB",
  "eventType": "SUB_CREATED",
  "payload": {
    "userId": "USR-456",
    "planType": "MONTHLY_STUDENT",
    "action": "CREATED"
  },
  "timestamp": { "$date": "2026-05-03T09:00:00Z" },
  "receivedAt": { "$date": "2026-05-03T09:00:02Z" },
  "lineId": null,
  "zoneId": null,
  "processed": true
}
```

**PAYMENT Event**
```json
{
  "_id": { "$oid": "6634d1b8c2c1a84f3d1b8a14" },
  "sourceType": "PAYMENT",
  "sourceId": "POS_301",
  "eventType": "PAYMENT_COMPLETED",
  "payload": {
    "transactionId": "TXN-001",
    "amount": 25.50,
    "method": "CARD",
    "status": "COMPLETED"
  },
  "timestamp": { "$date": "2026-05-03T10:05:00Z" },
  "receivedAt": { "$date": "2026-05-03T10:05:01Z" },
  "lineId": "L3",
  "zoneId": null,
  "processed": false
}
```

**VEHICLE Event**
```json
{
  "_id": { "$oid": "6634d1b8c2c1a84f3d1b8a15" },
  "sourceType": "VEHICLE",
  "sourceId": "BUS_404",
  "eventType": "TELEMETRY_UPDATE",
  "payload": {
    "status": "ACTIVE",
    "speed": 45.2,
    "occupancy": 85,
    "delayMinutes": 2
  },
  "timestamp": { "$date": "2026-05-03T11:00:00Z" },
  "receivedAt": { "$date": "2026-05-03T11:00:00Z" },
  "lineId": "L1",
  "zoneId": "Z_CENTER",
  "processed": true
}
```

**INCIDENT Event**
```json
{
  "_id": { "$oid": "6634d1b8c2c1a84f3d1b8a16" },
  "sourceType": "INCIDENT",
  "sourceId": "SYS_MONITOR",
  "eventType": "INCIDENT_REPORTED",
  "payload": {
    "incidentId": "INC-1001",
    "severity": "HIGH",
    "description": "Traffic accident blocking bus lane",
    "resolutionMinutes": null
  },
  "timestamp": { "$date": "2026-05-03T12:00:00Z" },
  "receivedAt": { "$date": "2026-05-03T12:00:03Z" },
  "lineId": "L2",
  "zoneId": "Z_NORTH",
  "processed": false
}
```

**USER Event**
```json
{
  "_id": { "$oid": "6634d1b8c2c1a84f3d1b8a17" },
  "sourceType": "USER",
  "sourceId": "APP_MOBILE",
  "eventType": "USER_LOGIN",
  "payload": {
    "userId": "USR-111",
    "deviceOS": "iOS"
  },
  "timestamp": { "$date": "2026-05-03T14:00:00Z" },
  "receivedAt": { "$date": "2026-05-03T14:00:01Z" },
  "lineId": null,
  "zoneId": null,
  "processed": true
}
```

---

## Section 3 — Computed Snapshots in MongoDB (stat_snapshots collection)

### TRIPS (FREQ_01 to FREQ_07)
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9001" },
  "snapshotType": "TRIPS",
  "statId": "FREQ_01",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 145000.0,
  "metadata": {
    "id": "FREQ_TOTAL_VALIDATIONS",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9002" },
  "snapshotType": "TRIPS",
  "statId": "FREQ_02",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 0.0,
  "metadata": {
    "id": "FREQ_PEAK_HOUR_DIST",
    "data": {
      "8": 15000,
      "9": 12000,
      "17": 18000,
      "18": 16500
    }
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9003" },
  "snapshotType": "TRIPS",
  "statId": "FREQ_03",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 0.0,
  "metadata": {
    "id": "FREQ_BY_LINE",
    "data": {
      "L1": 45000,
      "L2": 32000,
      "T1": 68000
    }
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9004" },
  "snapshotType": "TRIPS",
  "statId": "FREQ_04",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 2.4,
  "metadata": {
    "id": "FREQ_FRAUD_RATE",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9005" },
  "snapshotType": "TRIPS",
  "statId": "FREQ_05",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 18.5,
  "metadata": {
    "id": "FREQ_TRANSFER_RATE",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9006" },
  "snapshotType": "TRIPS",
  "statId": "FREQ_06",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 3480.0,
  "metadata": {
    "id": "FREQ_INVALID_SCANS",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9007" },
  "snapshotType": "TRIPS",
  "statId": "FREQ_07",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 72.1,
  "metadata": {
    "id": "FREQ_CARD_USAGE_RATE",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```

### REVENUE (REV_01 to REV_05)
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9011" },
  "snapshotType": "REVENUE",
  "statId": "REV_01",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 450000.50,
  "metadata": {
    "id": "REV_TOTAL",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9012" },
  "snapshotType": "REVENUE",
  "statId": "REV_02",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 0.0,
  "metadata": {
    "id": "REV_BY_LINE",
    "data": {
      "L1": 150000.0,
      "T1": 300000.50
    }
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9013" },
  "snapshotType": "REVENUE",
  "statId": "REV_03",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 0.0,
  "metadata": {
    "id": "REV_CASH_VS_CARD",
    "data": {
      "CASH": 120000.0,
      "CARD": 330000.50
    }
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9014" },
  "snapshotType": "REVENUE",
  "statId": "REV_04",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 124.0,
  "metadata": {
    "id": "REV_HIGH_VALUE_TX",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9015" },
  "snapshotType": "REVENUE",
  "statId": "REV_05",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 1500.0,
  "metadata": {
    "id": "REV_REFUND_VOLUME",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```

### INCIDENTS (INC_01 to INC_05)
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9021" },
  "snapshotType": "INCIDENTS",
  "statId": "INC_01",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 12.0,
  "metadata": {
    "id": "INC_TOTAL",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9022" },
  "snapshotType": "INCIDENTS",
  "statId": "INC_02",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 45.5,
  "metadata": {
    "id": "INC_RESOLUTION_TIME",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9023" },
  "snapshotType": "INCIDENTS",
  "statId": "INC_03",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 2.0,
  "metadata": {
    "id": "INC_CRITICAL",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9024" },
  "snapshotType": "INCIDENTS",
  "statId": "INC_04",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 15.0,
  "metadata": {
    "id": "INC_AVG_DELAY",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9025" },
  "snapshotType": "INCIDENTS",
  "statId": "INC_05",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 1.0,
  "metadata": {
    "id": "INC_REPEAT_ZONES",
    "data": {
      "Z_CENTER": 4
    }
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```

### VEHICLES (VEH_01 to VEH_05)
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9031" },
  "snapshotType": "VEHICLES",
  "statId": "VEH_01",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 450.0,
  "metadata": {
    "id": "VEH_ACTIVE_COUNT",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9032" },
  "snapshotType": "VEHICLES",
  "statId": "VEH_02",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 92.5,
  "metadata": {
    "id": "VEH_PUNCTUALITY",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9033" },
  "snapshotType": "VEHICLES",
  "statId": "VEH_03",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 18.0,
  "metadata": {
    "id": "VEH_OVERCAPACITY",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9034" },
  "snapshotType": "VEHICLES",
  "statId": "VEH_04",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 5.0,
  "metadata": {
    "id": "VEH_MAINTENANCE",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9035" },
  "snapshotType": "VEHICLES",
  "statId": "VEH_05",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 24.8,
  "metadata": {
    "id": "VEH_AVG_SPEED",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```

### SUBSCRIPTIONS (SUB_01 to SUB_05)
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9041" },
  "snapshotType": "SUBSCRIPTIONS",
  "statId": "SUB_01",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 85000.0,
  "metadata": {
    "id": "SUB_ACTIVE",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9042" },
  "snapshotType": "SUBSCRIPTIONS",
  "statId": "SUB_02",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 450.0,
  "metadata": {
    "id": "SUB_NEW",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9043" },
  "snapshotType": "SUBSCRIPTIONS",
  "statId": "SUB_03",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 1200.0,
  "metadata": {
    "id": "SUB_RENEWALS",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9044" },
  "snapshotType": "SUBSCRIPTIONS",
  "statId": "SUB_04",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 2.1,
  "metadata": {
    "id": "SUB_CHURN",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9045" },
  "snapshotType": "SUBSCRIPTIONS",
  "statId": "SUB_05",
  "granularity": "DAY",
  "period": "2026-05-03",
  "value": 125000.0,
  "metadata": {
    "id": "SUB_REVENUE",
    "data": {}
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": false
}
```

---

## Section 4 — ML Prediction Snapshots (PRED_01 and PRED_02)

**PRED_01: Peak Hours Prediction**
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9051" },
  "snapshotType": "PREDICTION",
  "statId": "PRED_01",
  "granularity": null,
  "period": null,
  "value": null,
  "metadata": {
    "predicted_peak_hours": [17, 8, 18],
    "distribution": [
      { "hour": 8, "score": 0.15 },
      { "hour": 9, "score": 0.10 },
      { "hour": 17, "score": 0.20 },
      { "hour": 18, "score": 0.18 }
    ],
    "generatedAt": "2026-05-03T14:30:00.123"
  },
  "computedAt": { "$date": "2026-05-03T14:30:00Z" },
  "prediction": true
}
```

**PRED_02: Incident Zone Prediction**
```json
{
  "_id": { "$oid": "6634d555c2c1a84f3d1b9052" },
  "snapshotType": "PREDICTION",
  "statId": "PRED_02",
  "granularity": null,
  "period": null,
  "value": null,
  "metadata": {
    "at_risk_zones": [
      { "zone": "Z_CENTER", "riskScore": 1.0, "riskLevel": "HIGH" },
      { "zone": "Z_NORTH", "riskScore": 0.65, "riskLevel": "MEDIUM" },
      { "zone": "Z_EAST", "riskScore": 0.2, "riskLevel": "LOW" }
    ],
    "generatedAt": "2026-05-03T14:30:01.456"
  },
  "computedAt": { "$date": "2026-05-03T14:30:01Z" },
  "prediction": true
}
```

---

## Section 5 — Analytics API Responses (what G10 receives from us)

**GET `/api/v1/analytics/trips/summary`**
*(Note: Omitting ID/internal fields for brevity as sent to client)*
```json
[
  {
    "id": "6634d555c2c1a84f3d1b9001",
    "snapshotType": "TRIPS",
    "statId": "FREQ_01",
    "granularity": "DAY",
    "period": "2026-05-03",
    "value": 145000.0,
    "metadata": { "id": "FREQ_TOTAL_VALIDATIONS", "data": {} },
    "computedAt": "2026-05-03T14:30:00",
    "prediction": false
  }
]
```

**GET `/api/v1/analytics/revenue/summary`**
```json
[
  {
    "id": "6634d555c2c1a84f3d1b9011",
    "snapshotType": "REVENUE",
    "statId": "REV_01",
    "granularity": "DAY",
    "period": "2026-05-03",
    "value": 450000.50,
    "metadata": { "id": "REV_TOTAL", "data": {} },
    "computedAt": "2026-05-03T14:30:00",
    "prediction": false
  }
]
```

**GET `/api/v1/analytics/incidents/stats`**
```json
[
  {
    "id": "6634d555c2c1a84f3d1b9021",
    "snapshotType": "INCIDENTS",
    "statId": "INC_01",
    "granularity": "DAY",
    "period": "2026-05-03",
    "value": 12.0,
    "metadata": { "id": "INC_TOTAL", "data": {} },
    "computedAt": "2026-05-03T14:30:00",
    "prediction": false
  }
]
```

**GET `/api/v1/analytics/vehicles/activity`**
```json
[
  {
    "id": "6634d555c2c1a84f3d1b9031",
    "snapshotType": "VEHICLES",
    "statId": "VEH_01",
    "granularity": "DAY",
    "period": "2026-05-03",
    "value": 450.0,
    "metadata": { "id": "VEH_ACTIVE_COUNT", "data": {} },
    "computedAt": "2026-05-03T14:30:00",
    "prediction": false
  }
]
```

**GET `/api/v1/analytics/users/stats`**
```json
[
  {
    "id": "6634d555c2c1a84f3d1b9061",
    "snapshotType": "USERS",
    "statId": "USR_01",
    "granularity": "DAY",
    "period": "2026-05-03",
    "value": 15000.0,
    "metadata": { "id": "USR_ACTIVE_USERS", "data": {} },
    "computedAt": "2026-05-03T14:30:00",
    "prediction": false
  }
]
```

**GET `/api/v1/analytics/subscriptions/stats`**
```json
[
  {
    "id": "6634d555c2c1a84f3d1b9041",
    "snapshotType": "SUBSCRIPTIONS",
    "statId": "SUB_01",
    "granularity": "DAY",
    "period": "2026-05-03",
    "value": 85000.0,
    "metadata": { "id": "SUB_ACTIVE", "data": {} },
    "computedAt": "2026-05-03T14:30:00",
    "prediction": false
  }
]
```

**GET `/api/v1/analytics/dashboard`**
```json
[
  {
    "id": "6634d555c2c1a84f3d1b9001",
    "snapshotType": "TRIPS",
    "statId": "FREQ_01",
    "granularity": "DAY",
    "period": "2026-05-03",
    "value": 145000.0,
    "metadata": { "id": "FREQ_TOTAL_VALIDATIONS", "data": {} },
    "computedAt": "2026-05-03T14:30:00",
    "prediction": false
  },
  {
    "id": "6634d555c2c1a84f3d1b9011",
    "snapshotType": "REVENUE",
    "statId": "REV_01",
    "granularity": "DAY",
    "period": "2026-05-03",
    "value": 450000.50,
    "metadata": { "id": "REV_TOTAL", "data": {} },
    "computedAt": "2026-05-03T14:30:00",
    "prediction": false
  }
]
```

---

## Section 6 — Report Generation

**POST `/api/v1/analytics/reports/generate` (Request Body)**
```json
{
  "period": "2026-05-03",
  "types": ["TRIPS", "REVENUE", "INCIDENTS"]
}
```

**POST `/api/v1/analytics/reports/generate` (Response)**
```json
{
  "id": "6634d8a1c2c1a84f3d1ba111",
  "generatedAt": "2026-05-03T14:40:01",
  "period": "2026-05-03",
  "requestedTypes": ["TRIPS", "REVENUE", "INCIDENTS"],
  "snapshots": [
    {
      "id": "6634d555c2c1a84f3d1b9001",
      "snapshotType": "TRIPS",
      "statId": "FREQ_01",
      "granularity": "DAY",
      "period": "2026-05-03",
      "value": 145000.0,
      "metadata": { "id": "FREQ_TOTAL_VALIDATIONS", "data": {} },
      "computedAt": "2026-05-03T14:30:00",
      "prediction": false
    },
    {
      "id": "6634d555c2c1a84f3d1b9011",
      "snapshotType": "REVENUE",
      "statId": "REV_01",
      "granularity": "DAY",
      "period": "2026-05-03",
      "value": 450000.50,
      "metadata": { "id": "REV_TOTAL", "data": {} },
      "computedAt": "2026-05-03T14:30:00",
      "prediction": false
    }
  ]
}
```

**GET `/api/v1/analytics/reports/{id}`**
*(Returns the exact same response as the generation endpoint above).*

**Report Document in MongoDB (`reports` collection)**
```json
{
  "_id": { "$oid": "6634d8a1c2c1a84f3d1ba111" },
  "generatedAt": { "$date": "2026-05-03T14:40:01Z" },
  "period": "2026-05-03",
  "requestedTypes": ["TRIPS", "REVENUE", "INCIDENTS"],
  "snapshots": [
    {
      "_id": { "$oid": "6634d555c2c1a84f3d1b9001" },
      "snapshotType": "TRIPS",
      "statId": "FREQ_01",
      "granularity": "DAY",
      "period": "2026-05-03",
      "value": 145000.0,
      "metadata": { "id": "FREQ_TOTAL_VALIDATIONS", "data": {} },
      "computedAt": { "$date": "2026-05-03T14:30:00Z" },
      "prediction": false
    }
  ]
}
```

---

## Section 7 — Notifications sent to G5

### PUNCTUALITY_ALERT
```json
{
  "source": "G8_ANALYTICS",
  "eventType": "PUNCTUALITY_ALERT",
  "severity": "WARNING",
  "notificationType": "EMAIL, PUSH",
  "targetAudience": "OPERATORS",
  "subject": "[SGITU] Taux de ponctualité critique",
  "body": "Le taux de ponctualité est tombé à 78.5% (seuil : 80%)",
  "metadata": {
    "statId": "VEH_02",
    "value": 78.5,
    "threshold": 80,
    "period": "today"
  }
}
```

### HIGH_INCIDENT_VOLUME
```json
{
  "source": "G8_ANALYTICS",
  "eventType": "HIGH_INCIDENT_VOLUME",
  "severity": "WARNING",
  "notificationType": "EMAIL, SMS",
  "targetAudience": "SUPERVISORS",
  "subject": "[SGITU] Nombre d'incidents élevé",
  "body": "Incidents aujourd'hui : 12.0 (seuil : 10)",
  "metadata": {
    "statId": "INC_01",
    "value": 12.0,
    "threshold": 10,
    "period": "today"
  }
}
```

### HIGH_CHURN_RATE
```json
{
  "source": "G8_ANALYTICS",
  "eventType": "HIGH_CHURN_RATE",
  "severity": "WARNING",
  "notificationType": "EMAIL",
  "targetAudience": "MANAGEMENT",
  "subject": "[SGITU] Taux d'attrition élevé",
  "body": "Taux de churn : 16.5% (seuil : 15%)",
  "metadata": {
    "statId": "SUB_04",
    "value": 16.5,
    "threshold": 15,
    "period": "this_month"
  }
}
```

### LOW_DAILY_REVENUE
```json
{
  "source": "G8_ANALYTICS",
  "eventType": "LOW_DAILY_REVENUE",
  "severity": "WARNING",
  "notificationType": "EMAIL",
  "targetAudience": "MANAGEMENT",
  "subject": "[SGITU] Revenu journalier bas",
  "body": "Revenu du jour inférieur à 70% de la moyenne",
  "metadata": {
    "statId": "REV_01",
    "value": 25000.0,
    "threshold": 40000.0,
    "period": "today"
  }
}
```

### INCIDENT_ZONE_RISK
```json
{
  "source": "G8_ANALYTICS",
  "eventType": "INCIDENT_ZONE_RISK",
  "severity": "CRITICAL",
  "notificationType": "EMAIL, SMS, PUSH",
  "targetAudience": "OPERATORS, SUPERVISORS",
  "subject": "[SGITU] Zone à risque détectée",
  "body": "4.0 zone(s) avec incidents répétés détectée(s)",
  "metadata": {
    "statId": "INC_05",
    "value": 4.0,
    "threshold": 3,
    "period": "this_month"
  }
}
```

---

## Section 8 — ML Service Inputs and Outputs

### POST `/predict/peak-hours`
**Input**
```json
{
  "data": [
    { "hour": 6, "validationCount": 5000 },
    { "hour": 7, "validationCount": 12000 },
    { "hour": 8, "validationCount": 25000 },
    { "hour": 9, "validationCount": 18000 },
    { "hour": 10, "validationCount": 10000 },
    { "hour": 16, "validationCount": 11000 },
    { "hour": 17, "validationCount": 22000 },
    { "hour": 18, "validationCount": 24000 }
  ]
}
```

**Output**
```json
{
  "predicted_peak_hours": [8, 18, 17],
  "distribution": [
    { "hour": 8, "score": 0.196 },
    { "hour": 18, "score": 0.188 },
    { "hour": 17, "score": 0.173 },
    { "hour": 9, "score": 0.141 },
    { "hour": 7, "score": 0.094 },
    { "hour": 16, "score": 0.086 },
    { "hour": 10, "score": 0.078 },
    { "hour": 6, "score": 0.039 }
  ],
  "generatedAt": "2026-05-03T14:30:00.123"
}
```

### POST `/predict/incidents`
**Input**
```json
{
  "data": [
    { "zone": "Z_CENTER", "incidentCount": 5, "severity": "HIGH" },
    { "zone": "Z_NORTH", "incidentCount": 2, "severity": "CRITICAL" },
    { "zone": "Z_SOUTH", "incidentCount": 1, "severity": "MEDIUM" },
    { "zone": "Z_WEST", "incidentCount": 8, "severity": "LOW" },
    { "zone": "Z_EAST", "incidentCount": 0, "severity": "LOW" },
    { "zone": "Z_AIRPORT", "incidentCount": 1, "severity": "HIGH" },
    { "zone": "Z_UNIVERSITY", "incidentCount": 3, "severity": "MEDIUM" },
    { "zone": "Z_INDUSTRIAL", "incidentCount": 2, "severity": "HIGH" }
  ]
}
```

**Output**
```json
{
  "at_risk_zones": [
    { "zone": "Z_CENTER", "riskScore": 1.0, "riskLevel": "HIGH" },
    { "zone": "Z_NORTH", "riskScore": 0.533, "riskLevel": "MEDIUM" },
    { "zone": "Z_WEST", "riskScore": 0.533, "riskLevel": "MEDIUM" },
    { "zone": "Z_UNIVERSITY", "riskScore": 0.4, "riskLevel": "MEDIUM" },
    { "zone": "Z_INDUSTRIAL", "riskScore": 0.4, "riskLevel": "MEDIUM" },
    { "zone": "Z_AIRPORT", "riskScore": 0.2, "riskLevel": "LOW" },
    { "zone": "Z_SOUTH", "riskScore": 0.133, "riskLevel": "LOW" },
    { "zone": "Z_EAST", "riskScore": 0.0, "riskLevel": "LOW" }
  ],
  "generatedAt": "2026-05-03T14:30:01.456"
}
```

---

## Section 9 — Error Responses

**400 BAD REQUEST (Empty batch provided to ingestion)**
```json
{
  "timestamp": "2026-05-03T14:40:05.123+00:00",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/v1/ingestion/tickets"
}
```

**207 MULTI-STATUS (Mixed valid/invalid batch)**
```json
{
  "status": "PARTIAL",
  "message": "Processed 2/3 events successfully",
  "processedCount": 2,
  "failedCount": 1,
  "errors": [
    "Event at index 1: Invalid payload structure"
  ]
}
```

**404 NOT FOUND (Report ID not found)**
```json
{
  "timestamp": "2026-05-03T14:41:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "path": "/api/v1/analytics/reports/6634d8a1c2c1a84f3d1ba999"
}
```

**500 INTERNAL SERVER ERROR (Database unavailable)**
```json
{
  "timestamp": "2026-05-03T14:42:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/v1/ingestion/tickets"
}
```

---

## Data Flow Diagram

**Other groups → Ingestion → MongoDB → Scheduler → Snapshots → Analytics API → G10 → Admin/Directeur**

1. **Other Groups (G1-G7):** External services generate domain-specific data (e.g., tickets scanned, vehicles tracked) and send them as raw JSON to G8's `IngestionController` via HTTP POST.
2. **Ingestion & MongoDB:** The `IngestionService` receives these payloads, maps them to `IncomingEvent` documents, and persists them in the `incoming_events` collection in MongoDB.
3. **Scheduler:** A background cron job (`ScheduledAnalyticsJob`) wakes up periodically (e.g., every 60 seconds) to process unprocessed events.
4. **Snapshots (Aggregations & ML):** The scheduler triggers the Aggregation services (Trips, Revenue, etc.) to compute KPIs. It also triggers the `MlPredictionService` which queries MongoDB, formats data, sends it to the Python ML Service endpoints, and retrieves predictions. Both the computed KPIs and ML predictions are saved as `StatSnapshot` documents in the `stat_snapshots` collection via `SnapshotService.upsert()`. The `ThresholdAlertService` also runs here, sending alerts to G5 if any snapshot breaches its designated threshold.
5. **Analytics API:** The `AnalyticsController` exposes REST endpoints (GET) to retrieve these `StatSnapshot` documents and generate aggregated Reports.
6. **G10 (Reporting Interface):** The G10 Reporting module queries the Analytics API to construct dashboards and exportable reports.
7. **Admin/Directeur:** End-users interact with G10 to view the final, aggregated insights and predictions, completing the data lifecycle.
