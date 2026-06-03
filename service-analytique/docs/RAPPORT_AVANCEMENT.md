# Rapport d'Avancement : Microservice Analytique (G8)

**Équipe :** Service Analytique (`service-analytique`)  
**Dernière mise à jour :** 1 juin 2026  
**Rôle dans SGITU :** Collecte, agrégation et exposition des indicateurs (KPI) et prédictions à partir des événements des autres microservices (billetterie G2, abonnements G3, paiements G4, véhicules G6, incidents G7, utilisateurs G1).

---

## 1. Présentation du microservice

### Description du sous-système

Le **service analytique (G8)** constitue la couche décisionnelle du système SGITU. Il ingère des événements métier (REST et/ou Kafka), les persiste dans MongoDB, exécute des agrégations planifiées, déclenche des alertes sur seuils, interroge un microservice ML Python pour les prédictions, et expose les résultats via une API REST documentée (OpenAPI).

### Fonctionnalités métier implémentées

| Domaine | Description |
| :--- | :--- |
| **Ingestion** | Réception par lots (max 1 000 événements) via REST (`/api/v1/ingestion/*`) et consommation Kafka sur 6 topics (G1–G7). Validation `schemaVersion`, parsing des timestamps, statuts `SUCCESS` / `PARTIAL` / `REJECTED`. |
| **Agrégation batch** | Job planifié toutes les **60 s** (`ScheduledAnalyticsJob`) : 6 modules d’agrégation + détection d’alertes + 2 prédictions ML. |
| **Alertes** | `ThresholdAlertService` : 5 règles métier, envoi HTTP vers le service de notification **G5** avec **circuit breaker** Resilience4j. |
| **Prédictions ML** | `MlPredictionService` : heures de pointe (PRED_01) et zones à risque incidents (PRED_02), snapshots `PREDICTION` en base. |
| **Rapports** | Génération de rapports **JSON** consolidés (snapshots par type et période), stockés en MongoDB (`reports`). |
| **Consultation** | Endpoints REST pour lire les snapshots par domaine et le tableau de bord global. |

---

## 2. Conception (API REST)

### 2.1 Analytics — lecture des indicateurs

Base : `/api/v1/analytics` — contrôleur : `AnalyticsController`

| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/dashboard` | Retourne l’ensemble des snapshots (`stat_snapshots`). Paramètre optionnel `period` (non filtré côté dashboard actuellement). |
| `GET` | `/trips/summary` | Snapshots **TRIPS** (fréquentation / billetterie, IDs `FREQ_*`). |
| `GET` | `/revenue/summary` | Snapshots **REVENUE** (`REV_*`). |
| `GET` | `/incidents/stats` | Snapshots **INCIDENTS** (`INC_*`). |
| `GET` | `/vehicles/activity` | Snapshots **VEHICLES** (`VEH_*`). |
| `GET` | `/users/stats` | Snapshots **USERS** (`USER_*`). |
| `GET` | `/subscriptions/stats` | Snapshots **SUBSCRIPTIONS** (`SUB_*`). |
| `POST` | `/reports/generate` | Corps : `{ "period", "types": ["TRIPS", "REVENUE", ...] }` → rapport JSON persisté. |
| `GET` | `/reports/{id}` | Récupération d’un rapport par identifiant MongoDB. |

Pour les endpoints `*/summary` et `*/stats`, le paramètre query **`period`** permet de cibler une période précise ; sans `period`, tous les snapshots du type sont renvoyés.

### 2.2 Ingestion — réception des événements bruts

Base : `/api/v1/ingestion` — contrôleur : `IngestionController`

| Méthode | Endpoint | Source métier |
| :--- | :--- | :--- |
| `POST` | `/tickets` | Billetterie (G2) — `TICKETING` |
| `POST` | `/subscriptions` | Abonnements (G3) — `SUBSCRIPTION` |
| `POST` | `/payments` | Paiements (G4) — `PAYMENT` |
| `POST` | `/vehicles` | Suivi véhicules (G6) — `VEHICLE` |
| `POST` | `/incidents` | Incidents (G7) — `INCIDENT` |
| `POST` | `/users` | Utilisateurs (G1) — `USER` |

Réponses HTTP : `201` (succès), `207` (partiel), `400` (rejet), `503` (erreur MongoDB).

### 2.3 Choix techniques API

- **Format d’échange :** JSON (corps libre `Map<String, Object>` côté ingestion ; modèles typés pour rapports et snapshots).
- **Documentation :** annotations OpenAPI (`@Operation`, `@Tag`) + SpringDoc.
- **Erreurs ingestion :** `IngestionExceptionHandler` + codes HTTP ci-dessus.
- **Intégration passerelle :** l’API Gateway route `/api/analytics/**` et `/api/reports/**` vers le port **8088** (voir dépôt `api-gateway`).

---

## 3. Implémentation et architecture

### 3.1 Vue d’ensemble

```text
[ Microservices G1–G7 ] ──REST/Kafka──► [ G8 Analytics (Spring Boot) ]
                                              │
                    ┌─────────────────────────┼─────────────────────────┐
                    ▼                         ▼                         ▼
              MongoDB                   ML Service (FastAPI)      G5 Notifications
         incoming_events                  :5000                    :8085
         stat_snapshots
         reports
```

### 3.2 Couches applicatives (Java)

| Couche | Composants principaux |
| :--- | :--- |
| **Controllers** | `AnalyticsController`, `IngestionController`, `TestController` (tests / dev) |
| **Services** | `AnalyticsService`, `IngestionService`, `SnapshotService`, `MlPredictionService` |
| **Agrégation** | `IncidentAggregation`, `VehicleAggregation`, `TicketAggregation`, `RevenueAggregation`, `SubscriptionAggregation`, `UserAggregation` |
| **Messaging** | `KafkaIngestionConsumer`, `KafkaConsumerConfig` (ack manuel) |
| **Alertes** | `ThresholdAlertService`, `AlertSender` (Resilience4j `@CircuitBreaker`) |
| **Persistance** | `EventRepository`, `StatSnapshotRepository`, `SnapshotRepository`, `ReportRepository` |
| **Planification** | `ScheduledAnalyticsJob` (`@Scheduled(fixedRate = 60000)`) |
| **Schéma** | `SchemaVersionValidator` (version attendue par type de source) |

### 3.3 Indicateurs calculés (aperçu)

Les agrégations produisent des documents `StatSnapshot` (collection `stat_snapshots`) identifiés par `statId` :

- **Incidents (`INC_*`)** : total, par type, par zone, temps moyen de résolution, zones à incidents répétés.
- **Véhicules (`VEH_*`)** : flotte active, ponctualité, retards, taux d’utilisation, vitesse moyenne par ligne.
- **Fréquentation (`FREQ_*`)** : validations, distribution heures de pointe, classement lignes, affluence stations, ratio week-end / semaine.
- **Revenus (`REV_*`)** : total, par type de billet, revenu moyen par passager, modes de paiement, tendance.
- **Abonnements (`SUB_*`)** : actifs, nouveaux, renouvellements, churn, répartition par type.
- **Utilisateurs (`USER_*`)** : métriques agrégées sur les événements utilisateurs.
- **Prédictions (`PRED_01`, `PRED_02`)** : résultats ML stockés avec `snapshotType: PREDICTION`.

### 3.4 Système d’alertes (G5)

Évalué à chaque cycle du scheduler :

| Règle | Condition | `eventType` |
| :--- | :--- | :--- |
| Ponctualité | `VEH_PUNCTUALITY` &lt; 80 % | `PUNCTUALITY_ALERT` |
| Volume incidents | `INC_TOTAL` &gt; 10 / jour | `HIGH_INCIDENT_VOLUME` |
| Churn abonnements | `SUB_CHURN` &gt; 15 % | `HIGH_CHURN_RATE` |
| Revenu journalier | `REV_TOTAL` &lt; 70 % de la moyenne 30 j | `LOW_DAILY_REVENUE` |
| Zones à risque | `INC_REPEAT_ZONES` ≥ 1 | `INCIDENT_ZONE_RISK` |

URL configurable : `g5.notification.url` (défaut `http://api-gateway:8080/api/notifications/send`). En cas d’indisponibilité de G5, le circuit breaker ouvre et les alertes sont journalisées (`sendFallback`).

### 3.5 Microservice ML (Python / FastAPI)

Répertoire : `ml-service/`

| Endpoint ML | Algorithme (résumé) | Consommé par G8 |
| :--- | :--- | :--- |
| `POST /predict/peak-hours` | Scores de fréquence normalisés, top 3 heures | `MlPredictionService` → PRED_01 |
| `POST /predict/incidents` | Score de risque par zone (pondération sévérité, min-max) | PRED_02 |
| `GET /health` | Sonde de santé | Healthcheck Docker |

Stack : FastAPI, Pandas, NumPy (`requirements.txt`).

### 3.6 Technologies

| Composant | Version / détail |
| :--- | :--- |
| Java | 17 |
| Spring Boot | 3.3.5 |
| MongoDB | 7.0 (Docker) |
| Spring Kafka | Consommation multi-topics |
| SpringDoc OpenAPI | 2.5.0 |
| Resilience4j | 2.2.0 (circuit breaker G5) |
| Lombok | Modèles et logging (`@Slf4j`) |
| ML | Python 3, FastAPI, Uvicorn |

---

## 4. Documentation API (Swagger / OpenAPI)

- **Swagger UI (local) :** `http://localhost:8088/swagger-ui.html`
- **OpenAPI JSON :** `http://localhost:8088/v3/api-docs`
- **Collection Postman :** `docs/G8_Analytics_Postman_Collection.json`
- **Contrats de données (exemples JSON par groupe) :** `docs/DATA_CONTRACTS.md`
- **Référence dashboard Grafana :** `docs/DASHBOARD_REFERENCE.md`

---

## 5. Tests

### 5.1 Tests automatisés (JUnit 5)

| Classe de test | Périmètre |
| :--- | :--- |
| `AnalyticsControllerTest` | Endpoints analytics |
| `IngestionControllerTest` | Ingestion REST et codes HTTP |
| `KafkaIngestionConsumerTest` | Consommation Kafka |
| `ScheduledAnalyticsJobTest` | Orchestration du job |
| `MlPredictionServiceTest` | Appels ML (mockés) |
| `ThresholdAlertServiceCircuitBreakerTest` | Circuit breaker G5 |
| `SchemaVersionValidatorTest` | Validation de schéma |
| `IntegrationTest` | Scénario d'intégration |

**Note :** la compilation locale nécessite le traitement des annotations **Lombok** (configuré dans le build Maven / image Docker multi-étapes). Exécution recommandée : `docker-compose up --build` ou `./mvnw test` avec JDK 17 et Lombok actif.

### 5.2 Harness d'intégration automatisé (A-to-Z)

Un script PowerShell **`run-integration-tests.ps1`** remplace désormais le scénario Postman manuel pour la validation de bout-en-bout en environnement conteneurisé. Il couvre :

| Phase | Vérification |
| :--- | :--- |
| Phase 1 | Disponibilité du service (Spring Actuator Health) |
| Phase 2 | Filtre JWT — rejet des requêtes non authentifiées |
| Phase 3 | Ingestion REST + validation défensive (schéma, champs requis) |
| Phase 4 | Consommation asynchrone Kafka (topic `g2-ticketing-events`) |
| Phase 5 | Déclenchement du job d'agrégation, vérification des snapshots et des prédictions ML |

**Résultat actuel : 10/10 tests PASS — 100 % de réussite.**

```powershell
# Séquence complète recommandée :
docker compose down -v
docker compose up -d --build
powershell -ExecutionPolicy Bypass -File .\seed-dashboard-data.ps1
powershell -ExecutionPolicy Bypass -File .\run-integration-tests.ps1
```

### 5.3 Collection Postman — statut et utilité

La collection Postman (`docs/G8_Analytics_Postman_Collection.json`) reste **valide et à jour**. Elle est utile pour :

- **Exploration interactive** de l'API (lecture des snapshots par domaine, génération de rapports).
- **Tests de cas limites** (batch vide → 400, événement partiel → 207, rapport inexistant → 404).
- **Démonstration** à des collaborateurs qui préfèrent une interface graphique.

Elle n'est cependant **pas nécessaire pour valider le pipeline** : le harness PowerShell (`run-integration-tests.ps1`) et le script de seed (`seed-dashboard-data.ps1`) suffisent pour un cycle complet. La collection Postman peut être conservée comme complément.

> **Note :** La collection inclut désormais un dossier **Phase 0: Security**. L'exécution de la requête `00b - Generate JWT Token` génère automatiquement un jeton JWT en local (via `CryptoJS`) et l'injecte dans toutes les requêtes suivantes via l'authentification `Bearer` au niveau de la collection. Il n'est plus nécessaire d'ajouter les headers manuellement.

### 5.4 Tests manuels

- Swagger UI : `http://localhost:8088/swagger-ui.html` — explorer et tester tous les endpoints directement.
- Grafana : `http://localhost:3000` (admin / sgitu2026) — vérification visuelle des métriques après le seed.

---

## 6. Conteneurisation (Docker)

### 6.1 Service Java (`Dockerfile`)

- Build **multi-étape** : `eclipse-temurin:17-jdk-alpine` (compilation Maven) → `eclipse-temurin:17-jre-alpine` (runtime).
- Artefact : JAR Spring Boot, port exposé **8088**.

### 6.2 Service ML (`ml-service/Dockerfile`)

- Image dédiée FastAPI, port **5000**, healthcheck sur `/docs`.

---

## 7. Orchestration (Docker Compose)

Fichier : `docker-compose.yml`

| Service | Conteneur | Port hôte | Rôle |
| :--- | :--- | :--- | :--- |
| `mongodb` | `g8-mongo` | 27017 | Persistance |
| `ml-service` | `g8-ml-service` | 5000 | Prédictions ML |
| `g8-analytics` | `g8-analytics-service` | 8088 | Application Java |
| `zookeeper` | `g8-zookeeper` | — | Coordinateur Kafka |
| `kafka` | `g8-kafka` | 9092 | Broker de messages |
| `prometheus` | `g8-prometheus` | 9090 | Scraping des métriques Actuator |
| `grafana` | `g8-grafana` | 3000 | Tableaux de bord de supervision |

Variables d'environnement clés pour `g8-analytics` : `MONGO_URI`, `ML_SERVICE_URL`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`.

**Non inclus dans le Compose actuel :** service **G5** notifications — le circuit breaker Resilience4j prend en charge son indisponibilité.

### Lancement

```bash
cd service-analytique
docker-compose up --build
```

---

## 8. Sécurité et intégration plateforme

| Sujet | État dans G8 | Remarque |
| :--- | :--- | :--- |
| **JWT / Spring Security** | ✅ Implémenté | Filtre `JwtAuthenticationFilter` — toutes les routes protégées, secret configurable via `jwt.secret`. Validé par le harness (Phase 2). |
| **Validation des événements** | ✅ Implémentée | `schemaVersion` obligatoire, champs requis par source, timestamps, coordonnées GPS, valeurs numériques. |
| **Résilience appels externes** | ✅ Implémentée | Circuit breaker Resilience4j sur les alertes G5 ; erreurs ML gérées dans `MlPredictionService` (logs + skip si données insuffisantes). |
| **Kafka** | ✅ Intégré | Broker inclus dans le Compose local (`g8-kafka`). Consommateur batch testé et validé. |
| **Corrélation inter-services** | Partielle | Headers de corrélation côté gateway ; à harmoniser en intégration E2E. |

---

## 9. Intégration avec les autres groupes

| Groupe | Mécanisme prévu | Topics / endpoints |
| :--- | :--- | :--- |
| G1 Utilisateurs | Kafka `g1-user-events` + `POST /ingestion/users` | |
| G2 Billetterie | Kafka `g2-ticketing-events` + `POST /ingestion/tickets` | |
| G3 Abonnements | Kafka `g3-subscription-events` + `POST /ingestion/subscriptions` | |
| G4 Paiements | Kafka `g4-payment-events` + `POST /ingestion/payments` | |
| G6 Véhicules | Kafka `g6-vehicle-events` + `POST /ingestion/vehicles` | |
| G7 Incidents | Kafka `g7-incident-events` + `POST /ingestion/incidents` | |
| G5 Notifications | HTTP POST alertes | En cours de validation E2E avec l’équipe G5 |
| G10 Gateway | Routage `/api/analytics/**` | Port 8088 |

---

## 10. État d’avancement et perspectives

### Finalisé

- [x] Pipeline d'ingestion REST (6 sources) avec validation et réponses batch structurées.
- [x] Consommateurs Kafka batch pour les 6 flux d'événements (topics `g1` à `g7`).
- [x] Agrégations planifiées pour incidents, véhicules, billetterie, revenus, abonnements et utilisateurs.
- [x] API REST de consultation + génération de rapports JSON.
- [x] Microservice ML conteneurisé et intégré (2 prédictions : PRED_01, PRED_02).
- [x] Système d'alertes à seuils + circuit breaker Resilience4j vers G5.
- [x] Filtre JWT (`JwtAuthenticationFilter`) — toutes les routes sécurisées.
- [x] Stack Docker Compose complète : MongoDB, Zookeeper, Kafka, ML, analytics, Prometheus, Grafana.
- [x] Dockerfile multi-étape pour le service Java.
- [x] Métriques Prometheus (Micrometer Gauges) et health checks Spring Actuator opérationnels.
- [x] Tableaux de bord Grafana provisionnés et validés (9 panels actifs).
- [x] Script de seed (`seed-dashboard-data.ps1`) — génère des données réalistes sur 7 jours pour tous les groupes.
- [x] Harness d'intégration A-to-Z (`run-integration-tests.ps1`) — **10/10 tests PASS**.
- [x] Contrats de données documentés (`docs/DATA_CONTRACTS.md`) et référence dashboard (`docs/DASHBOARD_REFERENCE.md`).
- [x] **Integration avec G3 (utilisateurs)** — Stage 2 ✅ PASS (10/10 tests). Kafka topic `g8-user-events`, injection automatique de `schemaVersion` pour compatibilité.

### En cours / à finaliser

- [ ] **Intégration E2E avec G5** (notifications/alertes) dans l'environnement complet du projet (compose racine / CI). Stage 3 prêt à tester.
- [ ] **Intégration avec G7** (incidents en temps réel) — vérifier que le flux `g7-incident-events` envoie réellement des messages (actuellement pas de producteur wired à un endpoint G7).
- [ ] Alignement des **schémas d'événements** avec chaque équipe (versions `schemaVersion` > 1 si évolution des contrats).
- [ ] Routage gateway : vérifier la cohérence des chemins (`/api/v1/analytics` vs `/api/analytics` documentés côté gateway).
- [ ] Optimisation des agrégations sur gros volumes (index MongoDB, fenêtres temporelles).

### Améliorations envisagées

- Export PDF des rapports (actuellement JSON uniquement).
- Filtrage du dashboard par `period` et pagination des snapshots.
- Alertes G5 : tester le rétablissement du circuit breaker en environnement intégré.

### Difficultés rencontrées et résolues

- **Synchronisation des schémas** entre payloads des autres services, modèle `IncomingEvent` et contrats Pydantic du service ML.
- **Données historiques insuffisantes** : les prédictions ML sont ignorées tant qu’il n’y a pas d’événements sur 30 jours → résolu par le script de seed.
- **Consommateur Kafka** : configuration `setBatchListener(true)` nécessaire pour le listener `List<Map<String, Object>>`.
- **JWT PowerShell** : `ConvertTo-Json` formatait les tableaux en objets sur certaines versions PS → remplacé par des chaînes JSON brutes dans les scripts de test.
### Difficultés rencontrées et résolues

- **Synchronisation des schémas** entre payloads des autres services, modèle `IncomingEvent` et contrats Pydantic du service ML.
- **Données historiques insuffisantes** : les prédictions ML sont ignorées tant qu'il n'y a pas d'événements sur 30 jours → résolu par le script de seed.
- **Consommateur Kafka** : configuration `setBatchListener(true)` nécessaire pour le listener `List<Map<String, Object>>`.
- **JWT PowerShell** : `ConvertTo-Json` formatait les tableaux en objets sur certaines versions PS → remplacé par des chaînes JSON brutes dans les scripts de test.
- **Fenêtre temporelle INC_01** : l'agrégation utilisait uniquement la journée courante, ignorant les données historiques du seed → corrigé en fenêtre 7 jours.
- **G3 → G8 User Events Integration (RÉSOLU ✅)** : 
  - **Problème initial** : G3 publiait des objets JSON simples sans champ `schemaVersion`, mais G8 attendait un array/batch avec `schemaVersion`.
  - **Cause réelle** : Deux problèmes concomitants :
    1. Listener G8 attendait `List<Map<String, Object>>` au lieu d'un seul `Map<String, Object>`
    2. G3 omettait le champ `schemaVersion` requis par le validateur `SchemaVersionValidator`
  - **Solution implémentée** : 
    - Modifié `consumeUserEvents()` pour accepter un single `Map<String, Object>` (au lieu d'une liste)
    - Injection automatique de `schemaVersion: 1` dans le listener pour compatibilité avec G3
  - **Résultat** : End-to-end test Stage 2 ✅ 10/10 PASS (voir `G8_INTEGRATION_TESTING_PLAN.md`)

## 11. Structure du dépôt (référence)

```text
service-analytique/
├── src/main/java/ma/sgitu/g8/     # Application Spring Boot
├── src/test/java/                  # Tests JUnit
├── ml-service/                     # FastAPI (prédictions)
├── docs/                           # Ce rapport, Postman, guides
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

---

*Document généré à partir de l’analyse du code source du dossier `service-analytique` (mai 2026).*
