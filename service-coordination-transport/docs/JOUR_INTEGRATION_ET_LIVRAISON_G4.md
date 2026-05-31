# Guide G4 — Avant / pendant / après le jour d'intégration

**Groupe G4 uniquement.** Ce document évite la confusion avec G5, G7, etc.

---

## Les 3 moments du projet

```text
┌─────────────────┐   ┌──────────────────────┐   ┌─────────────────┐
│  MAINTENANT     │   │  JOUR D'INTÉGRATION  │   │  SOUTENANCE     │
│  (chez toi)     │   │  (avec la prof +     │   │  (1er juin)     │
│                 │   │   autres groupes)    │   │                 │
└─────────────────┘   └──────────────────────┘   └─────────────────┘
```

---

## PHASE 1 — Maintenant (avant le jour d'intégration)

### Ce que TU livres (ton ZIP G4)

| Élément | Fichier / preuve |
|---------|------------------|
| Code G4 | `src/` |
| Dockerfile G4 | `Dockerfile` |
| Compose G4 seul | `docker-compose.yml` |
| Compose + monitoring | `docker-compose.yml` (profil `monitoring`) |
| README | `README.md` |
| Rapport PDF | `RAPPORT_G4_COORDINATION.pdf` |
| Slides PDF | `PRESENTATION_G4.pdf` |
| Postman | `postman/` |

### Commandes Docker (chez toi)

**Quotidien — G4 seul :**
```powershell
cd service-coordination-transport
docker compose up -d --build
```

**Avec Prometheus + Grafana (monitoring G4) :**
```powershell
docker compose --profile monitoring up -d --build
```
*ou :* `docker compose --profile monitoring up -d --build`

**Vérifier :**
| URL | Attendu |
|-----|---------|
| http://localhost:8084/api/g4/health | `status: UP` |
| http://localhost:8084/api/g4/logs | liste de logs |
| http://localhost:9090/targets | cible `g4-coordination:8084` **UP** |
| http://localhost:3000 | Grafana (admin/admin) |

### Captures à faire MAINTENANT (rapport)

Dans `rapport/captures/` :
- `01_health.png`
- `02_logs.png`
- `03_prometheus_targets_up.png`
- `04_grafana_dashboard.png`
- `10_postman_missions.png`
- `20_chaos_monkey_degraded.png` (notification → DEGRADED sans G5)

### Ce que tu NE fais PAS seule

- ❌ Builder / lancer G5 dans ton compose  
- ❌ Réparer le code des autres groupes  
- ❌ Faire tourner les 10 microservices sur ton PC en permanence  

---

## PHASE 2 — Jour d'intégration (avec la prof)

### Objectif du jour

Tous les groupes allument **leur** service sur le réseau Docker commun **`sgitu-network`** et testent **1 appel entre 2 groupes**.

### Ce que G4 apporte ce jour-là

| Tu apportes | Détail |
|-------------|--------|
| Image Docker G4 | Déjà buildée |
| Bloc dans compose racine | `SGITU-Microservices/docker-compose.yml` → services `g4-coordination`, `g4-postgres` |
| Port **8084** | Pas de conflit avec les autres |
| Postman | Scénarios prêts |
| `.env` partagé | Même `JWT_SECRET` que G3 si test JWT |

### Ce que tu fais le matin du jour J

```powershell
cd C:\Users\daurinia\Desktop\SGITU-Microservices
cp .env.example .env
# Remplir JWT_SECRET identique au groupe G3

docker compose up -d g4-coordination g4-postgres kafka
docker compose ps
```

*(Ou le prof lance tout le monorepo — suivre ses consignes.)*

### Tests inter-groupes à préparer (choisir 2 minimum)

| # | Test | Avec qui | Preuve |
|---|------|----------|--------|
| 1 | Login G3 → `GET /api/g4/missions` avec Bearer | G3 | Capture Postman |
| 2 | G7 publie sur `vehicule-positions` → log G4 | G7 | `GET /api/g4/logs` |
| 3 | G5 up sur réseau → notification `ACCEPTED` | G5 | Postman (optionnel) |
| 4 | G5 down → notification `DEGRADED` | G5 ou absent | Postman |

### Si le groupe G5 est présent ce jour

Dans un fichier `.env` à la racine G4 ou monorepo :
```env
SGITU_G5_URL=http://notification-service:8085
```
Puis redémarrer G4 :
```powershell
docker compose up -d g4-coordination
```

### Si G5 n'est pas là (normal)

Rien à changer. `POST /api/notifications/send` → **DEGRADED** = comportement correct (Chaos Monkey).

### Phrase pour la prof

> « G4 est sur `sgitu-network` port 8084. Nous avons testé [G3 JWT / Kafka G7]. En l'absence de G5, les notifications passent en mode dégradé sans erreur 500. »

---

## PHASE 3 — Après le jour d'intégration (avant le 29 mai)

### À ajouter au rapport / ZIP si nouveau tests

| Si vous avez fait… | Ajoutez au rapport |
|--------------------|-------------------|
| Test JWT G3 → G4 | Capture + paragraphe « validation croisée » |
| Test Kafka avec G7 | Topic + extrait logs G4 |
| Test avec G5 | Capture ACCEPTED vs DEGRADED |
| Prometheus UP | Capture targets + Grafana |

### Commit Git (si changements d'URL d'intégration)

Exemple : URLs G3/G7 validées en séance → mettre à jour `docs/CONTRATS_ALIGNES_G4.md` et **uniquement le bloc G4** dans `../.env.example` (pas les sections G5/G7/etc.).

### Pas besoin de refaire

- Tout le code métier (déjà fait)  
- Dockerfile G4 (déjà fait)  
- Retirer G5 de votre compose (déjà fait)  

---

## PHASE 4 — Soutenance (1er juin)

### Matériel à avoir sur le PC

- [ ] Docker : stack G4 up (`docker compose up -d` ou `--profile monitoring`)
- [ ] Postman collection importée
- [ ] Slides PDF ouvertes
- [ ] Onglets : health, logs, (optionnel) Prometheus

### Démo 5 minutes recommandée

1. `docker compose ps` → conteneurs G4  
2. `/api/g4/health`  
3. Postman : login + liste missions  
4. Postman : notification → montrer **DEGRADED** ou **ACCEPTED**  
5. « Questions ? »

---

## Récap : qui fait quoi

| Responsable | Quoi |
|-------------|------|
| **G4 (vous)** | Microservice coordination, compose G4, health/logs, Prometheus/Grafana **sur G4**, intégration **vers** les autres |
| **G5** | Leur service notifications — ils le lancent eux-mêmes |
| **G3** | Users + JWT |
| **Prof / promo** | Réseau commun, planning jour d'intégration |

---

## Commandes rapides (aide-mémoire)

```powershell
# G4 seul
docker compose up -d --build

# G4 + Prometheus + Grafana
docker compose --profile monitoring up -d --build

# Arrêter
docker compose down

# Logs G4
docker logs g4-coordination-service -f

# Tests
.\mvnw.cmd test
```
