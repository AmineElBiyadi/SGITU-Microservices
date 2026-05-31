# Livraison VCA — Pr. BESRI (2025-2026)

**Groupe :** G4 — Coordination des transports  
**Archive :** `G4_SGITU_Final.zip`  
**Deadline :** 29 mai 2026, 23h59 — Soutenance à partir du 1er juin 2026

Ce document mappe **chaque exigence officielle** vers les fichiers du projet.

---

## 1. Contexte

Phase finale SGITU : produit **fini**, **documenté**, **sécurisé**, prêt pour déploiement orchestré (`sgitu-network`).

---

## 2. Livrables obligatoires

### A. Rapport Technique Final (PDF)

| Section exigée | Fichier / contenu G4 |
|----------------|----------------------|
| Introduction — périmètre | `RAPPORT_G4_COORDINATION.tex` ch. 1 |
| Conception avancée — UML | ch. 2 + `docs/diagrams/*.puml` |
| Architecture technique | ch. « Architecture technique et choix technologiques » |
| Documentation API — Swagger | ch. 4 + captures `rapport/captures/` |
| Sécurité JWT et rôles | ch. 7 + `docs/SECURITE_END_TO_END_G4.md` |
| Validation & tests | ch. 8 + Postman + `mvn test` |

**Compiler le PDF :**
```bash
cd service-coordination-transport
pdflatex RAPPORT_G4_COORDINATION.tex
pdflatex RAPPORT_G4_COORDINATION.tex
```
→ `RAPPORT_G4_COORDINATION.pdf`

**Captures à insérer** (copier à la racine du dossier rapport ou `rapport/captures/`) :
- `fig_4_1_swagger_overview.png`, `fig_4_2_*.png`, `fig_5_1_*.png`, `fig_6_*.png`
- `10_validation_croisee_g3_g4.png`, `20_chaos_monkey_degraded.png`

---

### B. Support de présentation (PDF, 10-15 slides)

| Fichier | Slides |
|---------|--------|
| `PRESENTATION_G4.tex` | 12 slides (contexte, métier, archi, API, sécu, observabilité, Chaos Monkey, tests, démo) |

**Compiler :**
```bash
pdflatex PRESENTATION_G4.tex
pdflatex PRESENTATION_G4.tex
```
→ `PRESENTATION_G4.pdf`

---

### C. Code source & DevOps

| Exigence | Fichier |
|----------|---------|
| `src/` complet | `src/main/java`, `src/main/resources`, `src/test` |
| Dockerfile production | `Dockerfile` (multi-stage, user non-root) |
| docker-compose + dépendances | `docker-compose.yml` (profil `monitoring`), `../docker-compose.yml` (monorepo) |
| README | `README.md` |

**Lancer le projet :**
```bash
docker compose --profile monitoring up -d --build
.\mvnw.cmd test
```

---

## 3. Critères d'évaluation — preuves G4

| Critère | Preuve |
|---------|--------|
| **Fonctionnalité 100 %** | CRUD réseau + flotte + événements + incident-impacts + notifications ; Postman collection complète |
| **Qualité API REST** | OpenAPI, codes 200/201/400/401/403/404/409/202, `GlobalExceptionHandler` |
| **Sécurité & cloud** | `SecurityConfig.java`, JWT, rôles G3, validation croisée Postman |
| **Conteneurisation** | `docker compose up` → 3+ conteneurs healthy ; capture Docker Desktop |
| **Rigueur académique** | Rapport PDF + UML `.puml` + slides |

---

## 4. Assembler l'archive ZIP

```powershell
cd service-coordination-transport
.\scripts\assemble-livraison-G4.ps1
```

Produit : **`G4_SGITU_Final.zip`** à la racine de `service-coordination-transport/`.

Structure ZIP :
```
G4_SGITU_Final.zip
├── LISEZMOI.txt
├── RAPPORT_TECHNIQUE_G4.pdf
├── PRESENTATION_G4.pdf
└── service-coordination-transport/
    ├── src/
    ├── Dockerfile
    ├── docker-compose.yml
    ├── docker-compose.yml (+ profil monitoring)
    ├── README.md
    ├── postman/
    ├── docs/
    └── monitoring/
```

---

## 5. Checklist avant envoi (23h59)

- [ ] `mvn test` → SUCCESS
- [ ] `docker compose --profile monitoring up -d` → OK
- [ ] Rapport PDF compilé et relu
- [ ] Slides PDF (10-15 pages)
- [ ] Captures Postman (validation croisée + Chaos Monkey)
- [ ] ZIP `G4_SGITU_Final.zip` < taille max plateforme
- [ ] Dépôt GitHub à jour (`main`)
- [ ] Upload sur la plateforme cours

---

## 6. Soutenance 1er juin — Chaos Monkey

Voir **`docs/SOUTENANCE_CHAOS_ORAL.md`** : script si le prof éteint G5 (ou autre service).

**Phrase clé :** « G4 reste UP, réponse DEGRADED, notification en file d'attente, retry automatique. »
