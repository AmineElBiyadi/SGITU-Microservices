# Checklist G4 — Remarques prof + Livraison finale SGITU

**Groupe :** G4 — Coordination des transports  
**Deadline :** vendredi **29 mai 2026, 23h59**  
**Soutenance :** à partir du **1er juin 2026**  
**Archive :** `G4_SGITU_Final.zip` (adapter le nom de groupe)

Légende : `[x]` fait · `[ ]` à faire · `[~]` en cours / partiel

---

## PARTIE 1 — Remarques prof (séance 11 mai 2026)

### A. Déjà validé (à rappeler à l’oral)

- [x] Stade satisfaisant — niveau projet OK
- [x] Point de jonction entre services (G1, G3, G5, G7, G9, G10) — schéma architecture
- [x] Garder PostgreSQL — pas de changement SGBD

### B. Les 10 remarques techniques

| # | Point | Code | Rapport / UML | Postman / capture |
|---|--------|:----:|:-------------:|:-----------------:|
| 1 | Codes HTTP Swagger alignés (201, 400, 401, 403, 404, **409**) | [x] | [ ] capture Swagger `POST /missions` | [ ] test 409 même véhicule |
| 2 | Exemples JSON payloads complexes (mission, retard, déviation, notif, event) | [x] | [ ] cités dans rapport | [ ] collection à jour |
| 3 | Retards / déviations **sans bloquer** mission (reste EN_COURS) | [x] | [ ] expliqué § métier | [ ] retard puis GET mission |
| 4 | Résilience (Resilience4j / fallback) — évolution ou bonus | [x] | [ ] `docs/RESILIENCE_G4.md` | [ ] démo Chaos (`docs/CHAOS_MONKEY_G4.md`) |
| 5 | Rôles synchronisés G3 / G10 / G4 | [x] | [ ] tableau rôles dans rapport | [ ] login + JWT dans capture |
| 6 | Monitoring `/health` + `/logs` **sans token** | [x] | [ ] § supervision | [ ] GET logs sans Authorization |
| 7 | Docker : pas `localhost` pour Kafka ; image `apache/kafka:3.7.0` | [x] | [ ] § DevOps | [ ] `docker compose up` OK |
| 8 | Diagramme classes : Mission `1` — `*` CoordinationEvent | [~] `.puml` fait | [ ] **image dans rapport PDF** | — |
| 9 | Architecture externe : flèche G4 → G3 « valider user / conducteur / rôle » | [~] `.puml` fait | [ ] **image dans rapport PDF** | — |
| 10 | Consommation Kafka G7 `vehicule-positions` → déviation si besoin | [x] | [ ] § intégration G7 | [ ] test avec G7 ou mock Kafka |

**Fichiers utiles :** `docs/ROLES_G3_G4_ALIGNMENT.md`, `docs/diagrams/*.puml`, `docs/REMARQUES_PROF_STATUT.md`

---

## PARTIE 2 — Livraison finale VCA (Pr. BESRI)

> **Guide complet :** `docs/LIVRAISON_VCA_BESRI_G4.md`  
> **Archive :** `G4_SGITU_Final.zip` via `scripts/assemble-livraison-G4.ps1`

### A. Rapport Technique Final (PDF)

- [x] Source LaTeX `RAPPORT_G4_COORDINATION.tex` (sections VCA à jour)
- [ ] Compiler → `RAPPORT_G4_COORDINATION.pdf`
- [ ] Introduction, UML, architecture, API, sécurité, tests (chapitres 1–10)
- [ ] Insérer captures dans `rapport/captures/` ou racine rapport

### B. Présentation (PDF 10–15 slides)

- [x] Source `PRESENTATION_G4.tex` (12 slides)
- [ ] Compiler → `PRESENTATION_G4.pdf`

### C. Code & DevOps

- [x] `src/` + `Dockerfile` + `docker-compose.yml` (profil `monitoring`)
- [x] `README.md`
- [x] Script ZIP `scripts/assemble-livraison-G4.ps1`

### Pilier 1 — Intégration sans couture

- [x] Réseau Docker `sgitu-network` — G4 dans `../docker-compose.yml` racine
- [x] Stack autonome `docker compose --profile monitoring` (G4 + monitoring, **sans G5**)
- [ ] Test inter-groupes avec capture `docker network inspect sgitu-network`

### Pilier 2 — Observabilité

- [x] `/api/g4/health` + `/api/g4/logs` (publics)
- [x] Actuator + Prometheus (`/actuator/prometheus`)
- [x] Grafana dashboard `monitoring/grafana/`
- [ ] Captures dans `rapport/captures/`

### Pilier 3 — Validation croisée

- [x] Collection Postman dossier « 99 — Validation croisée »
- [x] Doc `docs/VALIDATION_CROISEE_G4.md`
- [ ] Capture Postman JWT G3 → G4 (ou G10 → G4)

### Chaos Monkey (bonus)

- [x] Fallback G5 → DEGRADED + file `pending_notifications`
- [x] Retry automatique + `POST /api/g4/pending-notifications/retry`
- [x] Collection Postman « 100 — Chaos Monkey »
- [ ] Captures démo soutenance

### A. Rapport final (PDF)

- [ ] Introduction — périmètre microservice G4
- [ ] **UML avancé** — classes, séquences, composants (feedback prof intégré)
  - [ ] Mission ↔ événements
  - [ ] Architecture externe (G1, G3, G5, G7, G9, G10, Kafka)
- [ ] Architecture technique — Java 21, Spring Boot 3, PostgreSQL, Kafka, Docker
- [ ] **Documentation API** — Swagger + exemples requête/réponse
- [ ] **Sécurité** — JWT, rôles G3 (`ROLE_OPERATOR`, `DISPATCHER`, `ADMIN_G4`, etc.)
- [ ] **Tests** — unitaires/intégration (`mvn test`) + **captures Postman**
- [ ] Intégrations — topics Kafka, contrats JSON, dépendances G3/G7/G9/G1/G5
- [ ] Supervision — `/api/g4/health`, `/api/g4/logs`, actuator
- [x] CI/CD — `.github/workflows/g4-ci.yml`, `g4-cd.yml` + `docs/CI_CD_G4.md`
- [ ] En cours / perspectives — tests inter-groupes, auth G10
- [ ] Export PDF depuis `RAPPORT_G4_COORDINATION.tex` (ou Word) — version **finale**

### B. Support de présentation (PDF, 10–15 slides)

- [ ] Contexte SGITU + rôle G4
- [ ] Architecture & jonctions entre groupes
- [ ] Démo ou captures (Swagger, Postman, Docker)
- [ ] Sécurité JWT / rôles
- [ ] Kafka (producteur/consommateur)
- [ ] Retards / déviations / 409
- [ ] Monitoring
- [ ] Limites & perspectives (résilience, K8s)
- [ ] Export PDF slides

### C. Code source & DevOps (dans le ZIP)

- [x] Code `src/` propre et compilable
- [x] `Dockerfile` présent
- [x] `docker-compose.yml` (postgres + kafka + g4-coordination, réseau `sgitu-network`)
- [ ] **`README.md`** — prérequis, `docker compose up`, URLs, comptes démo, tests
- [ ] Pas de secrets en clair (`.env.example` si besoin, mots de passe démo documentés uniquement)
- [ ] Collection Postman : `postman/SGITU-G4-Coordination-Transport.postman_collection.json`
- [ ] Exemples JSON : `postman/examples/`

### D. Archive ZIP groupe

- [ ] Dossier structuré (rapport PDF + slides PDF + code service-coordination-transport)
- [ ] Nom : `G4_SGITU_Final.zip` (ou nom officiel du binôme)
- [ ] **Un seul dépôt** par groupe sur la plateforme cours
- [ ] Vérification taille / contenu avant envoi

---

## PARTIE 3 — Critères d’évaluation finale (100 pts)

| Critère | Action G4 | Fait |
|---------|-----------|:----:|
| Fonctionnalité 100 % cas d’usage | Parcourir tous les endpoints métier + tests | [~] |
| Qualité API REST + OpenAPI | Swagger à jour, codes HTTP, erreurs structurées | [x] |
| Sécurité & cloud (JWT, rôles) | SecurityConfig + doc + capture token | [x] |
| Conteneurisation | `docker compose up -d` sans erreur | [ ] à re-tester |
| Rigueur académique | Rapport + UML lisibles | [ ] |

### Bonus (optionnel)

- [x] Communication asynchrone **Kafka** (G7 positions, G9 incidents, G1 lifecycle)
- [x] Kubernetes (simulation ou manifestes) — `k8s/` + `docs/KUBERNETES_G4.md` + `scripts/deploy-g4-k8s.ps1`
- [ ] Prometheus / Grafana — ou monitoring via `/health` + `/logs` documenté

---

## PARTIE 4 — Consignes classe (évaluation globale 11 mai)

### Priorités intégration

- [x] Contrats alignés — **mêmes topics** et JSON avec G1, G7, G9 — voir `docs/CONTRATS_ALIGNES_G4.md` + `KafkaContractValidator`
- [ ] Réseau Docker partagé `sgitu-network` — test avec au moins **un autre groupe**
- [ ] **Validation croisée** — capture Postman : JWT valide (idéalement via **G10**) → appel G4 réussi
- [ ] Auth harmonisée G3 (Users) + G10 (Gateway) — même `jwt.secret` en intégration

### Challenge « Chaos Monkey » (fortement valorisé)

- [x] Choisir **un** appel critique (ex. notification **G5** ou billetterie **G1**) — G5 via `G5NotificationClient`
- [x] Comportement si service down : pas de 500 brut, statut **`DEGRADED`** (Resilience4j)
- [ ] Préparer démo soutenance : prof peut **éteindre** un conteneur voisin — scénario `docs/CHAOS_MONKEY_G4.md`

---

## PARTIE 5 — Captures & tests à produire (preuves)

Cocher au fur et à mesure des captures enregistrées dans `rapport/captures/` ou équivalent.

### Postman (minimum)

- [ ] `POST /api/auth/login` — token reçu
- [ ] `POST /api/g4/lignes` — 201
- [ ] `POST /api/g4/missions` — 201
- [ ] `POST /api/g4/missions` (même véhicule EN_COURS) — **409**
- [ ] `POST /api/g4/events/detect-delay` — 201 puis `GET /api/g4/missions/{id}` — statut **EN_COURS**
- [ ] `POST /api/g4/events/detect-deviation` — 201
- [ ] `GET /api/g4/health` — sans token
- [ ] `GET /api/g4/logs` — sans token
- [ ] Swagger UI — vue d’ensemble + exemple mission

### Technique

- [ ] `mvn test` — BUILD SUCCESS (capture ou log)
- [ ] `docker compose up -d` — conteneurs healthy (capture Docker Desktop ou `docker ps`)
- [ ] (Optionnel) Appel G4 via **G10** avec JWT G3

### Inter-groupes

- [ ] Échange avec **G7** (position Kafka) ou **G3** (conducteur) — capture ou log supervision

---

## PARTIE 6 — Planning suggéré (avant le 29 mai)

| Semaine | Tâches |
|---------|--------|
| **S1** | README.md · re-test Docker · captures Postman |
| **S2** | Rapport PDF (UML images + § remarques prof) |
| **S3** | Slides 10–15 · test inter-groupe · ZIP |
| **S4** | Relecture · pas de secrets · dépôt 29/05 23h59 |

---

## PARTIE 7 — Synthèse une page (pour le rapport)

> Suite aux remarques du 11 mai 2026 et aux exigences de livraison finale, le microservice G4 Coordination des Transports est documenté (OpenAPI, exemples JSON), sécurisé (JWT, rôles G3/G10), conteneurisé (Docker Compose, PostgreSQL, Kafka 3.7.0), et supervisé (`/health`, `/logs`). Les perturbations (retard, déviation) sont traitées par événements sans blocage des missions ; les conflits véhicule/mission renvoient un HTTP 409. Les intégrations Kafka (G7 positions, G9 incidents, G1 cycle de vie) et la dépendance conceptuelle vers G3 pour les identités sont matérialisées dans l’architecture. La résilience avancée (Resilience4j, Chaos Monkey), les tests inter-groupes complets et le déploiement Kubernetes constituent les perspectives de consolidation.

---

## Références rapides

| Élément | Valeur |
|---------|--------|
| Port G4 | `8084` |
| Swagger | `http://localhost:8084/swagger-ui.html` |
| Health | `GET /api/g4/health` |
| Logs | `GET /api/g4/logs` |
| Kafka image | `apache/kafka:3.7.0` |
| Topic G7 | `vehicule-positions` |
| Comptes démo | `gestionnaire.reseau` / `gestionnaire.flotte` / `admin.technique` — mot de passe `password` |
| Repo | https://github.com/anadouae/SGITU-Microservices |

---

*Dernière mise à jour checklist : mai 2026 — commit remarques prof `440451c`.*
