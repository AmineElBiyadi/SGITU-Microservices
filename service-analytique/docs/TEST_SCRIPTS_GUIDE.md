# Guide des Scripts de Test — G8 Analytics Integration Validation

**Date :** 1er juin 2026  
**Contexte :** Ce document explique les deux scripts PowerShell utilisés pour valider l'intégration entre G8 Analytics et les autres microservices. Il détaille ce que chaque script fait, comment l'exécuter, et comment interpréter les résultats pour prouver que l'intégration fonctionne.

---

## Sommaire

1. [Vue d'ensemble](#vue-densemble)
2. [Script 1 : `test-g3-user-events.ps1`](#script-1--test-g3-user-eventsps1)
3. [Script 2 : `run-integration-tests.ps1`](#script-2--run-integration-testsps1)
4. [Résultats de succès](#résultats-de-succès)
5. [Troubleshooting](#troubleshooting)

---

## Vue d'ensemble

| Script | Périmètre | Microservices | Type | Durée |
|--------|-----------|----------------|------|-------|
| **test-g3-user-events.ps1** | G3 → Kafka → G8 seulement | G3, Kafka, G8 | Ciblé (1 source) | ~30s |
| **run-integration-tests.ps1** | Multi-source E2E | G1, G2, G3, G4, G6, G7, Kafka, G8, MongoDB | Complet | ~2–5min |

**Objectif commun :** Prouver que les événements émis par une source sont ingérés, persistés, et transformés en agrégations dans G8 MongoDB.

---

## Script 1 : `test-g3-user-events.ps1`

### Emplacement
```
service-analytique/test-g3-user-events.ps1
```

### Objectif
Tester la chaîne **G3 (Abonnements) → Kafka (g8-user-events) → G8 (Analytics)** en isolation.

### Ce qu'il fait

#### Phase 1 : Vérification de la Readiness (Préalables)
```
[PASS] Kafka container is ready
[PASS] G8 Mongo container is ready
[PASS] G8 Analytics container is ready
[PASS] G3 database container is ready
[PASS] G3 user-service container is ready
[PASS] G8 health endpoint is reachable
[PASS] G3 health endpoint is reachable
```
- S'assure que tous les services requis sont en exécution
- Teste la connectivité réseau (Docker DNS, ports exposés)
- Timeout : 30s par service

**Implication :** Si l'une de ces étapes échoue, les services ne sont pas prêts. Exécuter `docker compose up -d` d'abord.

#### Phase 2 : Création d'un Vrai Utilisateur G3
```
[PASS] G3 public user creation succeeds
```
- Appelle `POST /api/v1/users/public` sur G3
- Crée un utilisateur avec email/mot de passe aléatoires
- Reçoit un `userId` unique (ex. `userId=1`)

**Implication :** Cet utilisateur déclenchera automatiquement un événement `g8-user-events` via le producer interne G3.

#### Phase 3 : Vérification E2E (Kafka → MongoDB)
```
[PASS] G8 stored the G3 user event
[PASS] G3 user event path is end-to-end
[OK] G3 produced the user event and G8 consumed it.
```

**Étapes internes :**

1. **L'événement est publié à Kafka**
   - G3 détecte la création d'utilisateur
   - Envoie `{"userId":"1","action":"active","timestamp":"2026-06-01T06:05:42Z","schemaVersion":1}` au topic `g8-user-events`
   - Kafka ACK reçu

2. **L'événement est consommé par G8**
   - Le listener G8 `consumeUserEvents()` reçoit l'événement de Kafka
   - Injecte `schemaVersion` (si manquant) pour compatibilité
   - Valide le schéma et les champs obligatoires
   - Convertit en objet `Event` interne

3. **L'événement est persisté dans MongoDB**
   - `IngestionService.ingest()` crée une entrée dans la collection `events`
   - Enregistre : `sourceType=USER`, `statId=USR_01`, `payload={...}`, `status=ACCEPTED`
   - Déclenche les agrégations associées (ex. `USR_01` — Utilisateurs actifs journaliers)

4. **Le script vérifie la persistance**
   - Interroge MongoDB : `db.events.findOne({sourceType:"USER",payload.userId:"1"})`
   - Vérifie que `status=ACCEPTED` (pas `REJECTED`)
   - Affiche le `createdUserId` récupéré

**Implication :** Si cette étape échoue → événement rejeté en validation. Consulter les logs G8 pour diagnostiquer.

### Comment l'exécuter

#### Prérequis
- Windows PowerShell ou PowerShell Core
- Docker Desktop avec compose running
- `docker logs` accessible pour troubleshooting

#### Commande
```powershell
cd C:\Users\pc\Documents\S2\Microservices\Projet\code\SGITU-Microservices
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g3-user-events.ps1
```

#### Options
```powershell
# Avec logs verbose (pour debug)
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g3-user-events.ps1 -Verbose

# Réexécuter (crée un nouvel utilisateur chaque fois)
# Le script est idempotent → peut être run plusieurs fois
```

### Résultats possibles

#### ✅ Succès (10/10 PASS)
```
== Summary ==
Total: 10
Pass:  10
Fail:  0
```
**Interprétation :**
- ✅ Tous les services sont actifs
- ✅ G3 a créé un utilisateur avec succès
- ✅ Kafka a reçu l'événement
- ✅ G8 a ingéré et persisté l'événement
- ✅ MongoDB stocke maintenant l'événement avec `status=ACCEPTED`
- ✅ **La chaîne G3 → Kafka → G8 est FONCTIONNELLE**

---

## Script 2 : `test-root-integration.ps1`

### Emplacement
```
service-analytique/test-root-integration.ps1
```

### Objectif
Tester l'**intégration complète de G8** depuis la **racine du projet** avec le **docker-compose.yml racine**. Ce script valide que tous les services (Kafka, MongoDB, G8 ML, G8 Analytics, Prometheus) communiquent correctement et que G8 peut ingérer des événements via REST et Kafka, les traiter, créer des snapshots, et être monitored par Prometheus.

### Ce qu'il fait

#### Phase A : Validation du modèle Compose
```
[PASS] Compose model renders
```
- Vérifie que le `docker-compose.yml` racine est syntaxiquement correct
- Assure que les services sont correctement définis

**Implication :** Le compose ne peut pas démarrer s'il est invalide.

#### Phase B : Startup du runtime G8 (optionnel)
```
[PASS] Started Kafka, G8 Mongo, G8 ML, G8 analytics, Prometheus
```
- Par défaut : Le script **NE démarre pas les services** (mode `StartServices: $false`)
- Si appelé avec `-StartServices` : Démarre Kafka, MongoDB, ML, Analytics, Prometheus
- Si `-SkipBuild` est omis : Reconstruit les images avant démarrage

**Implication :** Le script assume que les services tournent déjà (manuel ou via CI/CD).

#### Phase C : Vérification de la santé des conteneurs
```
[PASS] sgitu-kafka is running/healthy
[PASS] g8-mongo is running/healthy
[PASS] g8-ml-service is running/healthy
[PASS] g8-analytics-service is running/healthy
```
- Attend que chaque conteneur soit en état `running` et `healthy`
- Timeout : 180 secondes (configurable)

**Implication :** Si un conteneur n'est pas healthy, il y a un problème de démarrage ou de liveness probe.

#### Phase D : Checks d'infrastructure directe
```
[PASS] Kafka broker responds
[PASS] Mongo ping succeeds
[PASS] ML service reachable from G8 container
```
- **Kafka :** Teste `kafka-broker-api-versions` pour vérifier la connectivité
- **MongoDB :** Exécute un `ping` admin depuis mongosh
- **ML service :** Fait un `curl` de G8 vers ML sur le réseau Docker interne

**Implication :** Vérifie que les services ne sont pas seulement "running" mais vraiment opérationnels.

#### Phase E : Vérification HTTP et JWT de G8
```
[PASS] G8 actuator health is reachable
[PASS] G8 blocks unauthenticated dashboard request (401)
[PASS] G8 accepts generated JWT from .env secret
```

**Détail :**
1. **Health endpoint :** `GET /actuator/health` → doit répondre 200
2. **Authentification :** `GET /api/v1/analytics/dashboard` sans token → 401 (rejection)
3. **JWT validation :** Même endpoint avec token généré depuis `.env` → 200 OK

**JWT token :** Le script lit `.env`, extrait `JWT_SECRET`, génère un HS256 JWT avec rôles `ROLE_ADMIN`, `ADMIN`.

**Implication :** G8 est sécurisé et authentification JWT fonctionne.

#### Phase F : Préparation des topics Kafka
```
[PASS] Kafka topics are ready
```
- Crée (ou vérifie l'existence) de 9 topics :
  - `ticket.validated`, `abonnement.souscription`, `payment.transaction.completed`
  - `g8.vehicule.status`, `incident.analytique.topic`
  - `g8-user-events` (G3 integration)
  - `g8-analytics-dlt`, `g8-analytics-results`, `g8-ml-predictions`

**Implication :** Topics prêts à recevoir des messages.

#### Phase G : Ingestion REST avec timestamps actuels
```
[PASS] REST ticket ingestion accepted
[PASS] REST vehicle ingestion accepted
[PASS] REST incident ingestion accepted
[PASS] Mongo incoming_events has data
```

**Détail :**
- Envoie 3 événements via REST avec JWT auth :
  - `POST /api/v1/ingestion/tickets` → billet
  - `POST /api/v1/ingestion/vehicles` → véhicule
  - `POST /api/v1/ingestion/incidents` → incident
- Chaque événement : `schemaVersion: 1`, timestamp ISO 8601, champs requis
- Vérifie que MongoDB collection `incoming_events` a reçu les données

**Implication :** REST ingestion fonctionne E2E.

#### Phase H : Ingestion Kafka avec timestamps actuels
```
[PASS] Published Kafka messages to G8 topics
[PASS] G8 consumed Kafka events into Mongo
[PASS] G8 Kafka consumer group is visible
```

**Détail :**
- Publie 3 messages à des topics Kafka :
  - `ticket.validated` → billet
  - `g8.vehicule.status` → véhicule
  - `incident.analytique.topic` → incident
- Attend (max 90s) que G8 consomme et persiste dans MongoDB
- Vérifie que le consumer group `g8-analytics-group` existe

**Implication :** Kafka ingestion E2E fonctionne.

#### Phase I : Scheduler, Snapshots, ML, Prometheus
```
[PASS] Manual analytics job trigger succeeds
[PASS] Analytics snapshots exist in Mongo
[PASS] G8 Prometheus endpoint responds
[PASS] Prometheus has g8-analytics target
```

**Détail :**
1. **Manual job trigger :** `POST /test/run` (endpoint de test) → déclenche une agrégation manuelle
2. **Snapshots :** Vérifie que `stat_snapshots` collection a ≥ 1 document
3. **Prometheus metrics :** `GET /actuator/prometheus` → doit contenir métriques JVM/HTTP
4. **Prometheus scraping :** Vérifie que Prometheus a enregistré G8 comme target

**Implication :** Analytics job, snapshots, et monitoring complets fonctionnent.

### Comment l'exécuter

#### Commande basique (services supposés déjà en exécution)
```powershell
cd C:\Users\pc\Documents\S2\Microservices\Projet\code\SGITU-Microservices
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-root-integration.ps1
```

#### Avec options
```powershell
# Démarrer les services en parallèle avec le test
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-root-integration.ps1 -StartServices

# Sans reconstruire les images
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-root-integration.ps1 -StartServices -SkipBuild

# Avec timeout personnalisé (défaut: 180s)
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-root-integration.ps1 -TimeoutSeconds 300
```

#### Durée d'exécution
- **Compose validation :** ~5s
- **Container health checks :** ~30–60s (dépend du démarrage)
- **Infrastructure & HTTP checks :** ~15s
- **Topic preparation :** ~10s
- **REST ingestion :** ~5s
- **Kafka ingestion + wait :** ~30s (attente de consommation)
- **Snapshots & monitoring :** ~15s
- **Total :** ~2–3 min (si services déjà running), ~5–7 min (avec `-StartServices`)

### Résultats possibles

#### ✅ Succès complet (Tous les checks PASS)
```
== Summary ==
Total: 24
Pass:  24
Fail:  0
All G8 root integration checks passed.
```

**Interprétation :**
- ✅ Compose model valide
- ✅ Tous les conteneurs sont sains
- ✅ Kafka, MongoDB, ML sont opérationnels
- ✅ Authentification JWT fonctionne
- ✅ Topics Kafka prêts
- ✅ REST ingestion acceptée
- ✅ Kafka ingestion consommée
- ✅ Snapshots créés
- ✅ Monitoring Prometheus OK
- ✅ **L'intégration racine G8 est PRÊTE POUR DÉPLOIEMENT**


## Résultats de succès

### Ce que "PASS" signifie pour chaque script

#### Script 1 : `test-g3-user-events.ps1` — 10/10 PASS
| Étape | Signification du PASS |
|-------|----------------------|
| Service Readiness | G3, Kafka, G8 et dépendances sont actifs et réseau OK |
| G3 user creation | G3 a émis un événement `g8-user-events` à Kafka |
| Kafka message exists | Kafka a stocké le message et peut le servir |
| G8 consumed event | G8 Kafka listener a reçu et traité le message |
| Event validated | Schéma, champs obligatoires OK; `schemaVersion` injecté |
| Event persisted | MongoDB contient l'événement avec `status=ACCEPTED` |
| Aggregation updated | Les stats G8 (ex. `USR_01`) réfléchissent le nouvel événement |

**Conclusion du PASS :** La chaîne **G3 → Kafka → G8 → MongoDB → Agrégations** est **100% opérationnelle**. Les événements utilisateurs de G3 sont capturés et analysés.

#### Script 2 : `run-integration-tests.ps1` — 6/6 PASS
| Stage | Signification du PASS |
|-------|----------------------|
| Stage 1 (G1) | Utilisateurs : événement ingéré, agrégations DAU mises à jour |
| Stage 2 (G2) | Billetterie : validations reçues, fréquences calculées |
| Stage 3 (G3) | Abonnements : événements créés/renouvelés, taux de churn tracés |
| Stage 4 (G4) | Paiements : transactions enregistrées, revenu agrégé |
| Stage 5 (G6) | Véhicules : status/occupancy persistés, ponctualité calculée |
| Stage 6 (G7) | Incidents : géolocalisés, zones à risque identifiées |

**Conclusion du PASS :** L'intégration **multi-source complète** est **opérationnelle**. Chaque microservice (G1–G7) peut envoyer des événements à G8, qui les traite, valide, persiste et agrège **en temps réel**.

---


## Résumé

| Aspect | Script 1 (G3 ciblé) | Script 2 (Root Compose) |
|--------|-------------------|----------------------|
| **Nom** | `test-g3-user-events.ps1` | `test-root-integration.ps1` |
| **Usage** | Debug G3 → G8 isolé | Validation pré-production complète |
| **Niveau** | Microservice pair | Système global racine |
| **Durée** | ~30s | ~2–3 min (services running) |
| **Couverture** | 1 source (G3) | REST + Kafka + Snapshots + Prometheus |
| **Cible** | Développeurs G3/G8 | QA, déploiement, CI/CD |
| **Verdict de succès** | 10/10 PASS | 24/24 PASS |
| **Signification** | G3 integration works | Full G8 system ready |
| **Services testés** | G3, Kafka, G8 | Kafka, MongoDB, ML, G8, Prometheus |

**Utilisez Script 1 pour :** Tester rapidement une correction G3 ou valider la chaîne G3 → Kafka → G8.  
**Utilisez Script 2 pour :** Valider l'intégration complète du `docker-compose.yml` racine avant un déploiement en staging/prod.

---

**Généré :** 1er juin 2026  
**Auteur :** G8 Analytics Integration Team
