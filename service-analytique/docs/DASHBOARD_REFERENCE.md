# SGITU G8 — Grafana Dashboard Reference

> **Dashboard URL:** `http://localhost:3000`  
> **Dashboard Title:** SGITU — Analytique G8  
> **Auto-refresh:** every 30 seconds  
> **Default time range:** Last 6 hours

This document explains every panel in the Grafana monitoring dashboard for the `service-analytique` (G8). All metrics are computed by the scheduled analytics aggregation job and exposed via Prometheus.

---

## Section 1 — Vue d'ensemble (Overview)

These four stat panels at the top give a quick pulse of the entire transit system at a glance.

### 1.1 Total Validations
| Property | Value |
|---|---|
| **Prometheus metric** | `sgitu_freq_total_validations` |
| **Source stat ID** | `FREQ_TOTAL_VALIDATIONS` |
| **Computed from** | All `TICKETING` events with `status: validated` in the last 7 days |

**What it means:** The cumulative number of ticket validations (passengers boarding) recorded over the current week. This is the primary ridership KPI. A rising number indicates healthy system usage.

---

### 1.2 Véhicules Actifs (Active Vehicles)
| Property | Value |
|---|---|
| **Prometheus metric** | `sgitu_veh_active_count` |
| **Source stat ID** | `VEH_ACTIVE_COUNT` |
| **Computed from** | `VEHICLE` events with `status: in_service` in the last 24 hours |

**What it means:** The number of distinct vehicles currently in active service (i.e., that reported an `in_service` status event within the last day). A low count relative to the fleet size may indicate a maintenance issue or data ingestion problem from the G6 service.

---

### 1.3 Incidents Total
| Property | Value |
|---|---|
| **Prometheus metric** | `sgitu_inc` |
| **Source stat ID** | `INC_TOTAL` |
| **Computed from** | All `INCIDENT` events in the last 7 days |

**What it means:** The total number of incidents (delays, breakdowns, accidents) reported by the G7 service over the past week. Ideally kept as low as possible. A spike here should be correlated with the Incidents & Zones section below to identify affected areas.

---

### 1.4 Ponctualité (Punctuality Rate)
| Property | Value |
|---|---|
| **Prometheus metric** | `sgitu_veh_punctuality_rate` |
| **Source stat ID** | `VEH_PUNCTUALITY` |
| **Computed from** | Vehicle events — ratio of on-time arrivals (no delay) vs total arrivals |
| **Gauge thresholds** | 🔴 < 70% / 🟠 70–80% / 🟢 > 80% |

**What it means:** The percentage of vehicle arrivals that are on time (no delay reported). The color-coded gauge gives an immediate health signal — green means the network is running smoothly, red is a critical situation requiring operational intervention.

---

## Section 2 — Activité en temps réel (Real-time Activity)

These two time-series charts show trends over the dashboard's selected time range (default: last 6 hours).

### 2.1 Fréquentation (validations)
| Property | Value |
|---|---|
| **Prometheus metric** | `sgitu_freq_total_validations` |
| **Chart type** | Time-series line chart |

**What it means:** How the total ticket validation count evolves over time. Each new data point is recorded every time the scheduled analytics job runs. This chart helps identify peak hours, demand drops, and unusual patterns (e.g., a flat line might indicate an ingestion outage from G2).

---

### 2.2 Moyenne Journalière (Daily Average)
| Property | Value |
|---|---|
| **Prometheus metric** | `sgitu_freq_avg_daily` |
| **Source stat ID** | `FREQ_AVG_DAILY` |
| **Chart type** | Time-series line chart |

**What it means:** The rolling average number of passengers per day, computed from the ticket validation data of the past week. Useful for smoothing out noise from the total validation count and identifying longer-term ridership trends.

---

## Section 3 — Incidents & Zones

This section drills into the geographic and temporal distribution of reported incidents.

### 3.1 Zones à incidents répétés (Repeat Incident Zones)
| Property | Value |
|---|---|
| **Prometheus metric** | `sgitu_inc_repeat_zones` |
| **Source stat ID** | `INC_REPEAT_ZONES` |
| **Computed from** | Geographic zones that have had **2 or more** incidents this calendar month |

**What it means:** The number of distinct geographic zones that are repeat offenders — i.e., areas where incidents have happened more than once this month. A non-zero value flags specific locations (e.g., a particular intersection or station) that require targeted infrastructure attention.

---

### 3.2 Incidents par zone (coordonnées)
| Property | Value |
|---|---|
| **Prometheus metric** | `sgitu_inc_by_zone{zone="..."}` |
| **Source stat ID** | `INC_BY_ZONE` |
| **Chart type** | Bar chart (one bar per geographic zone) |
| **Labels** | GPS coordinate strings (e.g., `33.57,-7.59`) resolved from incident latitude/longitude |

**What it means:** A bar chart showing how many incidents occurred in each geographic zone over the past week. Each bar label is a GPS coordinate pair that the `ZoneResolver` derived from the raw incident latitude/longitude reported by G7. Taller bars indicate hotspots that deserve attention from operations teams. The coordinate system uses 2-decimal rounding to cluster nearby incidents together.

---

## Section 4 — Alertes & Résilience (Alerts & Resilience)

### 4.1 Alertes déclenchées par type
| Property | Value |
|---|---|
| **Prometheus metric** | `sgitu_alerts_triggered_total{alert_type="..."}` |
| **Chart type** | Bar chart over time |
| **Prometheus query** | `increase(sgitu_alerts_triggered_total[5m])` |

**What it means:** Shows the rate of alerts fired by the analytics engine, broken down by alert type (e.g., `PUNCTUALITY_ALERT`, `INCIDENT_ZONE_ALERT`). Each bar shows how many alerts were triggered in each 5-minute scrape window. This is a direct measure of how often the system is detecting anomalies. Note: alerts are *sent* to the G5 notification service; if that service is unavailable, the circuit breaker opens and alert events are still counted here even if delivery failed.

---

## Section 5 — Prédictions ML (ML Predictions)

These two panels display the output of the Python machine-learning models running in the `g8-ml-service` container. The models are retrained and queried each time the scheduled analytics job runs.

### 5.1 Score Heure de Pointe — ML (Peak Hour Score)
| Property | Value |
|---|---|
| **Prometheus metric** | `sgitu_pred_peak_hour_score` |
| **Source stat ID** | `PRED_01` |
| **Range** | 0.0 → 1.0 |

**What it means:** A machine-learning confidence score indicating how likely the **current hour** is a peak ridership hour. The model is trained on historical ticket validation data (last 30 days). A score close to **1.0** means the model is highly confident this is a peak hour — operators should ensure maximum vehicle deployment. A score near **0.0** means it is likely off-peak.

---

### 5.2 Score Risque Incident — ML (Incident Risk Score)
| Property | Value |
|---|---|
| **Prometheus metric** | `sgitu_pred_incident_risk_score` |
| **Source stat ID** | `PRED_02` |
| **Range** | 0.0 → 1.0 |

**What it means:** The highest incident risk score across all geographic zones, as predicted by the ML model based on historical incident patterns (last 30 days). A score near **1.0** means at least one zone has a very high predicted probability of an incident occurring in the near future, allowing proactive dispatch of maintenance or emergency teams. A score of **0.0** means the model predicts a calm period with no elevated risk zones.

---

## Metrics Lifecycle

```
Upstream Services (G1–G7)
        │
        ▼  HTTP REST / Kafka
  IngestionController  ──►  MongoDB (incoming_events collection)
        │
        ▼  every 60s (scheduled job)
  ScheduledAnalyticsJob
        │
        ├──► Aggregations (Ticket, Vehicle, Incident, Revenue, Subscription, User)
        │         └──► MongoDB (stat_snapshots collection)
        │
        ├──► ML Predictions (PRED_01, PRED_02 via Python ml-service)
        │         └──► MongoDB (stat_snapshots collection)
        │
        └──► AnalyticsMetricsService.refreshMetrics()
                  └──► Micrometer Gauges ──► Prometheus scrape ──► Grafana
```

All dashboard values are **at-most 60 seconds stale** under normal operation, given the job runs every minute and Prometheus scrapes every 15 seconds.
