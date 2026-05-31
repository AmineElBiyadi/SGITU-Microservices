# Checklist G4 — Messages promo + G3 (à faire maintenant)

Dernière mise à jour : mai 2026 — à cocher avant tests finaux / jour d’intégration.

---

## 1. Git / Pull Requests (message classe)

| # | Action | Fait |
|---|--------|:----:|
| 1.1 | Avant chaque **PR** : relire diff (pas de secrets `.env`, pas de ZIP lourd) | [ ] |
| 1.2 | Dans la PR : **@247029523546252** (responsable merge) pour validation | [ ] |
| 1.3 | Ne pas laisser de PR ouverte « en attente » sans réponse | [ ] |
| 1.4 | Une PR = un sujet clair (ex. « G4: alignement rôles G3 », pas tout mélangé) | [ ] |
| 1.5 | Après merge : `git pull origin main` sur chaque poste du binôme | [ ] |

---

## 2. Docker Compose global (obligatoire promo)

| # | Action | Fait |
|---|--------|:----:|
| 2.1 | Cloner / mettre à jour le monorepo `SGITU-Microservices` | [ ] |
| 2.2 | Racine : `cp .env.example .env` et remplir **credentials G4** (voir responsable promo) | [ ] |
| 2.3 | Vérifier le bloc **G4** dans `docker-compose.yml` racine (`g4-coordination`, port **8084**) | [ ] |
| 2.4 | `hostname: g4-coordination` pour Prometheus / appels inter-services | [ ] |
| 2.5 | Tester : `docker compose up -d g4-coordination g4-postgres kafka` (ou stack complète promo) | [ ] |
| 2.6 | `docker compose ps` → G4 **healthy / running** | [ ] |
| 2.7 | Ne pas compter **uniquement** sur le compose local G4 pour l’intégration — **compose racine obligatoire** | [ ] |

**Commande type (racine monorepo) :**
```powershell
cd SGITU-Microservices
docker compose up -d g4-coordination g4-postgres kafka
curl http://localhost:8084/api/g4/health
```

---

## 3. Prometheus + Grafana partagés (compose racine)

| # | Action | Fait |
|---|--------|:----:|
| 3.1 | Repérer le **`prometheus.yml` à la racine** du monorepo (fichier promo partagé) | [ ] |
| 3.2 | Ajouter / compléter la section **G4** (job scrape) — voir modèle ci-dessous | [ ] |
| 3.3 | Cible : `g4-coordination:8084`, path `/actuator/prometheus` | [ ] |
| 3.4 | Vérifier que le compose racine monte ce fichier + lance **prometheus** et **grafana** | [ ] |
| 3.5 | http://localhost:9090/targets → job G4 **UP** | [ ] |
| 3.6 | http://localhost:3000 → Grafana (admin/admin ou creds promo) | [ ] |
| 3.7 | Capture pour rapport : targets UP + dashboard (optionnel) | [ ] |

**Modèle section G4 (à copier dans le `prometheus.yml` racine si absent) :**
```yaml
  - job_name: g4-coordination
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["g4-coordination:8084"]
        labels:
          service: g4-coordination-transport
          group: G4
```

**Référence locale déjà prête :** `service-coordination-transport/monitoring/prometheus.yml`

**Compose G4 seul (dev)** : `docker-compose.yml` + profil `monitoring` — OK chez vous, **en plus** du compose promo.

---

## 4. Rôles G3 — liste officielle (message G3)

G3 a publié une **nouvelle nomenclature**. À **aligner** avec eux avant les tests finaux.

### 4.1 Rôles G3 officiels (extrait utile pour G4)

| Famille | Rôles G3 |
|---------|----------|
| Usagers | `ROLE_PASSENGER`, `ROLE_STUDENT` |
| Flotte / suivi | `ROLE_DRIVER`, `ROLE_DISPATCHER`, `ROLE_OPERATOR`, **`ROLE_G4_OPERATOR`** |
| Incidents | `ROLE_SUPERVISOR`, `ROLE_TECHNICIAN`, `ROLE_SECURITY`, `ROLE_MEDIC`, `ROLE_CLEANER` |
| Admins | `ROLE_ADMIN`, `ROLE_G1_ADMIN`, …, **`ROLE_G4_ADMIN`**, `ROLE_G7_ADMIN`, `ROLE_G9_ADMIN` |

### 4.2 Rôles G4 alignés G3 (code + démo) — **fait**

| Compte démo | Rôle JWT G3 | Spring `hasRole()` |
|-------------|-------------|-------------------|
| `gestionnaire.reseau` | `ROLE_G4_OPERATOR` | `G4_OPERATOR` |
| `gestionnaire.flotte` | `ROLE_DISPATCHER` | `DISPATCHER` |
| `admin.technique` | `ROLE_G4_ADMIN` | `G4_ADMIN` |

### 4.3 Actions d’alignement

| # | Action | Fait |
|---|--------|:----:|
| 4.3.1 | Noms officiels G3 : `ROLE_G4_OPERATOR`, `ROLE_DISPATCHER`, `ROLE_G4_ADMIN` | [x] |
| 4.3.2 | `SecurityConfig.java` + tests + Postman mis à jour | [x] |
| 4.3.3 | Accord G9 : `ROLE_DISPATCHER` partagé — `ALIGNEMENT_ROLES_G3_G4_G9.md` | [x] |
| 4.3.4 | Même **`SGITU_JWT_SECRET`** dans `.env` racine | [ ] |
| 4.3.5 | Test token **G3** → G4 → **200** / **403** | [ ] |
| 4.3.6 | Capture validation croisée | [ ] |

---

## 5. Tests finaux « tout tester » (message classe)

| # | Test | Fait |
|---|------|:----:|
| 5.1 | `./mvnw test` → BUILD SUCCESS | [ ] |
| 5.2 | Compose **racine** : health G4 UP | [ ] |
| 5.3 | Postman : login + missions + 409 conflit véhicule | [ ] |
| 5.4 | Postman : notification → **DEGRADED** (G5 absent) | [ ] |
| 5.5 | JWT G3 (si dispo) → appel G4 | [ ] |
| 5.6 | Kafka / logs G4 (optionnel G7) | [ ] |
| 5.7 | GitHub Actions **G4 CI** vert après push | [ ] |
| 5.8 | Rapport PDF + slides PDF + ZIP livraison | [ ] |

---

## 6. Livrables 29 mai / soutenance 1er juin

| # | Livrable | Fait |
|---|----------|:----:|
| 6.1 | `RAPPORT_G4_COORDINATION.pdf` | [ ] |
| 6.2 | `PRESENTATION_G4.pdf` (10–15 slides) | [ ] |
| 6.3 | `G4_SGITU_Final.zip` (sans committer le ZIP dans Git) | [ ] |
| 6.4 | Captures dans `rapport/captures/` | [ ] |
| 6.5 | Démo prête : Compose + Postman ouverts | [ ] |

---

## 7. Messages à envoyer (copier-coller)

### Au responsable merge
> PR G4 prête — @247029523546252 merci de review/merge quand possible.

### À G3 (rôles)
> Bonjour, nous avons vu la liste officielle des rôles. Chez G4 nous utilisons aujourd’hui OPERATOR / DISPATCHER / ADMIN_G4 en démo. Pouvez-vous confirmer si le JWT doit contenir `ROLE_G4_OPERATOR` et `ROLE_G4_ADMIN` à la place ? On aligne SecurityConfig dès validation.

### À la promo (Prometheus)
> Section G4 ajoutée dans prometheus.yml racine — target `g4-coordination:8084`, path `/actuator/prometheus`. Prêt pour test compose partagé.

---

## Priorité cette semaine (ordre)

1. **Compose racine** + `.env`  
2. **Sync rôles G3** (noms JWT)  
3. **prometheus.yml racine** section G4  
4. **Tests Postman** + captures  
5. **PR** avec @ responsable  
6. PDF + ZIP  

---

## Fichiers utiles G4

| Sujet | Fichier |
|-------|---------|
| Jour d’intégration | `docs/JOUR_INTEGRATION_ET_LIVRAISON_G4.md` |
| Rôles G4 ↔ G3 | `docs/ROLES_G3_G4_ALIGNMENT.md` |
| Rôles G4 ↔ G9 | `docs/ALIGNEMENT_ROLES_G3_G4_G9.md` |
| Monitoring promo (racine) | `prometheus.yml` (job G4) + `docker compose up` |
| Monitoring local G4 | `service-coordination-transport/monitoring/` + `--profile monitoring` |
| CI/CD | `docs/CI_CD_G4.md` |
