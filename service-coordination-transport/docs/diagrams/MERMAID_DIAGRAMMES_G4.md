# Diagrammes Mermaid — G4 Coordination des Transports

> Copier chaque bloc dans [Mermaid Live Editor](https://mermaid.live) pour exporter PNG/SVG pour le rapport LaTeX.

---

## 2.2 — Diagramme de cas d'utilisation

```mermaid
flowchart TB
    subgraph Acteurs
        OP["👤 Gestionnaire réseau<br/>ROLE_G4_OPERATOR"]
        DISP["👤 Gestionnaire flotte<br/>ROLE_DISPATCHER"]
        ADM["👤 Admin technique<br/>ROLE_G4_ADMIN"]
        G10["G10 Gateway"]
        G3["G3 Utilisateurs"]
        G7["G7 Suivi véhicules"]
        G9["G9 Incidents"]
    end

    subgraph G4["Microservice G4 — Coordination"]
        UC1["Gérer référentiel<br/>(lignes, trajets, arrêts, horaires)"]
        UC2["Gérer affectations<br/>véhicule / ligne"]
        UC3["Créer / suivre / clôturer<br/>missions"]
        UC4["Déclarer événements<br/>(retard, déviation, panne)"]
        UC5["Enregistrer impact<br/>incident G9"]
        UC6["Envoyer notification<br/>(façade G5)"]
        UC7["Superviser<br/>(health, logs, métriques)"]
        UC8["Consommer positions<br/>Kafka G7"]
        UC9["Consommer incidents<br/>Kafka G9"]
    end

    OP --> UC1
    DISP --> UC2
    DISP --> UC3
    DISP --> UC4
    DISP --> UC5
    DISP --> UC6
    ADM --> UC7
    ADM --> UC3
    G10 --> UC3
    G10 --> UC6
    G3 -.->|validation chauffeurId| UC3
    G7 --> UC8
    G9 --> UC9
    UC8 --> UC4
    UC9 --> UC5
```

---

## 2.3 — Diagramme de classes — Domaine métier G4

### 2.3.1 Référentiel réseau

```mermaid
classDiagram
    class Ligne {
        +Long id
        +String code
        +String nom
        +String description
    }
    class Trajet {
        +Long id
        +DirectionTrajet direction
        +String code
    }
    class Arret {
        +Long id
        +String nom
        +Double latitude
        +Double longitude
    }
    class TrajetArret {
        +Integer ordre
    }
    class Horaire {
        +Long id
        +LocalTime heureDepart
        +LocalTime heureArrivee
        +DayOfWeek jourSemaine
    }

    Ligne "1" --> "0..*" Trajet : contient
    Ligne "1" --> "0..*" Arret : dessert
    Trajet "1" --> "0..*" TrajetArret : ordonnance
    TrajetArret --> Arret
    Ligne "1" --> "0..*" Horaire : planifie
```

### 2.3.2 Exploitation flotte

```mermaid
classDiagram
    class AffectationVehiculeLigne {
        +Long id
        +String vehiculeId
        +StatutAffectation statut
        +LocalDate dateDebut
        +LocalDate dateFin
    }
    class Mission {
        +Long id
        +String vehiculeId
        +String chauffeurId
        +StatutMission statut
        +LocalDateTime plannedStart
        +LocalDateTime actualStart
        +LocalDateTime endedAt
    }
    class StatutMission {
        <<enumeration>>
        PLANIFIEE
        EN_COURS
        CLOTUREE
        ANNULEE
    }
    class Ligne {
        +Long id
    }

    Ligne "1" --> "0..*" AffectationVehiculeLigne
    Ligne "1" --> "0..*" Mission
    Mission --> StatutMission
    note for Mission "Règle : max 1 EN_COURS par vehiculeId → HTTP 409"
```

### 2.3.3 Coordination et incidents

```mermaid
classDiagram
    class Mission {
        +Long id
        +String vehiculeId
        +StatutMission statut
    }
    class CoordinationEventEntity {
        +Long id
        +CoordinationEventType type
        +CoordinationEventStatus status
        +String vehiculeId
        +String description
    }
    class CoordinationEventType {
        <<enumeration>>
        RETARD
        DEVIATION
        PANNE
        ANNULATION_MISSION
    }
    class MissionIncidentImpact {
        +Long id
        +String g9ReferenceIncident
        +String vehiculeId
        +String g9Type
        +String g9Statut
    }
    class IncidentG9 {
        <<externe G9>>
        +String reference
        +String statut
    }

    Mission "1" --> "0..*" CoordinationEventEntity : événements opérationnels
    Mission "1" --> "0..*" MissionIncidentImpact : impacts incident
    MissionIncidentImpact ..> IncidentG9 : référence uniquement
    note for CoordinationEventEntity "Sources : API G4, Kafka G7"
    note for MissionIncidentImpact "Sources : Kafka G9, REST /incident-impacts"
```

### 2.3.4 Résilience

```mermaid
classDiagram
    class PendingNotification {
        +Long id
        +String payloadJson
        +PendingNotificationStatus statut
        +Integer retryCount
        +LocalDateTime createdAt
        +LocalDateTime lastRetryAt
    }
    class PendingNotificationStatus {
        <<enumeration>>
        PENDING
        SENT
        FAILED
    }
    class G5NotificationClient {
        +sendNotification()
        CircuitBreaker g5Notification
    }

    G5NotificationClient ..> PendingNotification : persiste si G5 down
    note for PendingNotification "Retry auto 30s ou POST /pending-notifications/retry"
```

### 2.3 — Vue globale (toutes entités)

```mermaid
classDiagram
    Ligne --> Trajet
    Ligne --> Arret
    Ligne --> Horaire
    Ligne --> AffectationVehiculeLigne
    Ligne --> Mission
    Mission --> CoordinationEventEntity
    Mission --> MissionIncidentImpact
    PendingNotification ..> Mission : notification liée métier
```

---

## 2.4 — Diagrammes de séquence

### 2.4.1 Séquence 1 — Création de mission (validation G3)

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / G10
    participant C as MissionController
    participant S as MissionService
    participant G3 as G3UserClient
    participant G3API as G3 User Service
    participant DB as PostgreSQL
    participant K as Kafka

    Client->>C: POST /api/g4/missions + JWT Bearer
    C->>C: @PreAuthorize ROLE_DISPATCHER
    C->>S: create(MissionRequest)
    S->>S: assertVehicleAvailable (409 si conflit)
    alt SGITU_G3_VALIDATION_ENABLED = true
        S->>G3: assertDriverExistsIfEnabled(chauffeurId)
        G3->>G3API: GET /api/users/drivers/ids
        G3API-->>G3: [1, 5, 42...]
        alt chauffeurId absent
            G3-->>S: BadRequestException 400
        end
    end
    S->>DB: save(Mission)
    S->>K: publish missions-lifecycle (G1)
    S-->>C: MissionResponse
    C-->>Client: 201 Created
```

### 2.4.2 Séquence 2 — Consommation position G7 (Kafka)

```mermaid
sequenceDiagram
    autonumber
    participant G7 as G7 Suivi véhicules
    participant K as Kafka topic vehicule-positions
    participant Consumer as G7VehiclePositionKafkaConsumer
    participant Val as KafkaContractValidator
    participant Log as SupervisionLogService
    participant ES as CoordinationEventService
    participant DB as PostgreSQL

    G7->>K: publish JSON {vehiculeId, lat, long, ...}
    K->>Consumer: @KafkaListener message
    Consumer->>Val: validateG7Position(msg)
    alt JSON invalide
        Val-->>Consumer: IllegalArgumentException
        Consumer->>Log: WARN payload rejeté
    else JSON valide
        Consumer->>Log: INFO KAFKA-G7 position reçue
        opt règle déviation déclenchée
            Consumer->>ES: create DEVIATION event
            ES->>DB: save CoordinationEventEntity
        end
    end
```

### 2.4.3 Séquence 3 — Notification Chaos Monkey (G5 down)

```mermaid
sequenceDiagram
    autonumber
    actor Client as DISPATCHER
    participant NC as NotificationController
    participant ND as NotificationDispatchService
    participant G5 as G5NotificationClient
    participant G5API as G5 Notification Service
    participant CB as Resilience4j CircuitBreaker
    participant PN as PendingNotificationService
    participant DB as PostgreSQL
    participant Sched as RetryScheduler

    Client->>NC: POST /api/notifications/send
    NC->>ND: dispatch(request)
    ND->>G5: send via RestClient
    G5->>CB: appel protégé
    CB->>G5API: HTTP POST
    G5API--xCB: timeout / connection refused
    CB-->>G5: CircuitBreakerOpenException
    G5-->>ND: échec
    ND->>PN: save PENDING
    PN->>DB: insert pending_notifications
    ND-->>NC: status DEGRADED
    NC-->>Client: 202 Accepted {status: DEGRADED}

    Note over Sched,DB: Toutes les 30s ou POST /pending-notifications/retry
    Sched->>G5: retry si G5 UP
    G5->>DB: statut SENT
```

### 2.4.4 Séquence 4 — Impact incident G9

```mermaid
sequenceDiagram
    autonumber
    participant G9 as G9 Incidents
    participant K as Kafka incident.transport.topic
    participant KC as G9IncidentKafkaConsumer
    participant IS as IncidentImpactService
    participant G7 as G7VehicleClient
    participant DB as PostgreSQL
    actor REST as POST /api/g4/incident-impacts

    alt flux Kafka
        G9->>K: publish {referenceIncident, vehiculeId...}
        K->>KC: message
        KC->>IS: handleKafkaMessage
    else flux REST
        REST->>IS: IncidentImpactRequest + JWT
    end
    IS->>IS: resolveMission(missionId ou vehiculeId)
    IS->>G7: fetchStatus(vehiculeId)
    alt G7 down
        G7-->>IS: mock INCONNU
    else G7 up
        G7-->>IS: statut véhicule
    end
    IS->>DB: save MissionIncidentImpact
    IS-->>REST: 201 IncidentImpactResponse
```

---

## 2.5 — Diagramme de composants

```mermaid
flowchart TB
    subgraph Clients
        G10[G10 API Gateway]
        Postman[Postman / Front]
    end

    subgraph G4["G4 — service-coordination-transport :8084"]
        direction TB
        subgraph Presentation
            CTRL[Controllers REST]
            AUTH[AuthController]
        end
        subgraph Metier
            SVC[Services métier]
        end
        subgraph Persistance
            REPO[JPA Repositories]
        end
        subgraph Integration
            G3C[G3UserClient]
            G5C[G5NotificationClient]
            G7C[G7VehicleClient]
            G7K[G7VehiclePositionKafkaConsumer]
            G9K[G9IncidentKafkaConsumer]
            G1K[G1 Mission Producer]
        end
        subgraph Config
            SEC[Spring Security JWT]
            RES[Resilience4j]
            ACT[Actuator / Prometheus]
        end
    end

    subgraph Externes["Services SGITU — sgitu-network"]
        G3S[G3 :8083]
        G5S[G5 :8085]
        G7S[G7 :8087]
        G9S[G9 :8089]
        G1S[G1 :8081]
    end

    subgraph Infra
        PG[(PostgreSQL g4-postgres)]
        KF{{Kafka sgitu-kafka}}
    end

    G10 --> CTRL
    Postman --> AUTH
    Postman --> CTRL
    CTRL --> SVC
    SVC --> REPO
    REPO --> PG
    SVC --> G3C
    SVC --> G5C
    SVC --> G7C
    G3C --> G3S
    G5C --> G5S
    G7C --> G7S
    G7S --> KF
    KF --> G7K
    G9S --> KF
    KF --> G9K
    G7K --> SVC
    G9K --> SVC
    SVC --> G1K
    G1K --> KF
    KF --> G1S
    SEC --> CTRL
    RES --> G5C
    ACT --> Postman
```

---

## 2.6 — Diagramme de déploiement

```mermaid
flowchart TB
    subgraph Host["Machine hôte — développeur / promo"]
        subgraph Docker["Docker — réseau sgitu-network"]
            subgraph StackG4["Compose local G4"]
                APP["Conteneur g4-coordination<br/>Spring Boot JAR<br/>:8084"]
                PG["Conteneur g4-postgres<br/>PostgreSQL 16<br/>:5432 → host 5434"]
                K["Conteneur sgitu-kafka<br/>Apache Kafka 3.7<br/>:9092"]
            end
            subgraph Monitoring["Profil monitoring — optionnel"]
                PROM["prometheus :9090"]
                GRAF["grafana :3000"]
            end
            subgraph Monorepo["Autres groupes — compose racine"]
                G3C["g3-user-service :8083"]
                G7C["g7-service :8087"]
                G5C["notification-service :8085"]
            end
        end
    end

    DEV[Navigateur / Postman localhost] --> APP
    APP --> PG
    APP --> K
    PROM -->|scrape /actuator/prometheus| APP
    GRAF --> PROM
    APP -.->|HTTP| G3C
    APP -.->|HTTP| G5C
    APP -.->|HTTP| G7C
    K -.->|vehicule-positions| APP
```

---

## 2.7 — Diagramme d'activité — Détection déviation (G7)

```mermaid
flowchart TD
    A([Début — message Kafka reçu]) --> B{Désérialiser JSON}
    B -->|échec| C[Log WARN — payload rejeté]
    C --> Z([Fin])
    B -->|ok| D{vehiculeId présent ?}
    D -->|non| C
    D -->|oui| E{lat et long présents ?}
    E -->|non| C
    E -->|oui| F[Journaliser INFO KAFKA-G7]
    F --> G{Mission EN_COURS<br/>pour ce vehiculeId ?}
    G -->|non| H[Fin — pas d'événement]
    H --> Z
    G -->|oui| I{Position hors trajet<br/>ou règle déviation ?}
    I -->|non| H
    I -->|oui| J[Créer CoordinationEvent DEVIATION]
    J --> K[Persister en PostgreSQL]
    K --> L[Mission reste EN_COURS]
    L --> Z
```

---

## 2.8 — Diagramme de packages (structure code)

```mermaid
flowchart TB
    subgraph com.sgitu.g4
        APP[G4Application]
        subgraph controller
            MC[MissionController]
            LC[LigneController]
            EC[CoordinationEventController]
            IC[IncidentImpactController]
            NC[NotificationController]
            SC[G4SupervisionController]
        end
        subgraph service
            MS[MissionService]
            CS[CoordinationEventService]
            IS[IncidentImpactService]
            NS[NotificationDispatchService]
            PNS[PendingNotificationService]
        end
        subgraph repository
            MR[MissionRepository]
            LR[LigneRepository]
            ER[EventRepository]
        end
        subgraph entity
            ENT[Mission, Ligne, Trajet...]
        end
        subgraph dto
            DTO[Request / Response DTOs]
        end
        subgraph mapper
            MAP[EntityMapper]
        end
        subgraph integration
            INT[G3, G5, G7, G9, Kafka consumers]
        end
        subgraph config
            CFG[SecurityConfig, IntegrationProperties]
        end
        subgraph security
            SEC[JwtFilter, UserDetails]
        end
        subgraph exception
            EX[GlobalExceptionHandler]
        end
    end

    controller --> service
    service --> repository
    repository --> entity
    controller --> dto
    service --> mapper
    service --> integration
    controller --> config
    config --> security
    controller --> exception
```

---

## Export pour le rapport LaTeX

1. Ouvrir https://mermaid.live
2. Coller un diagramme
3. **Actions → PNG/SVG** → enregistrer dans `figures/` avec le nom du rapport :
   - `uml_use_case_g4.png`
   - `uml_classes_domaine_g4.png`
   - `uml_seq_mission_create.png`
   - etc.
