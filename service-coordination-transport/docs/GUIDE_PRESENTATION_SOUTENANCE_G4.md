# C’est quoi le « Support de Présentation » (partie B) ?

## En une phrase

Ce n’est **pas** le rapport technique. C’est un **PowerPoint / PDF de 10 à 15 diapositives** que vous **montrez à l’oral** le **1er juin** (soutenance), et que vous **joignez aussi dans le ZIP** de livraison.

| Document | Format | Quand ? | Contenu |
|----------|--------|---------|---------|
| **A. Rapport technique** | PDF long (20+ pages) | À rendre + à lire si le prof demande | Tout le détail : UML, code, Docker, sécurité… |
| **B. Support de présentation** | PDF court (**10–15 slides**) | **À l’oral** + dans le ZIP | Résumé visuel + ce que vous allez **démontrer** |

---

## Ce que le prof veut voir sur les slides

### 1. Synthèse visuelle du projet
- Qui êtes-vous (G4) ?
- Quel problème métier vous résolvez ?
- Schéma simple : G4 au milieu, flèches vers G3, G5, G7, G9…

→ **Pas** de copier-coller le rapport page par page.

### 2. Valeur ajoutée **métier**
Répondre : *« À quoi sert G4 pour l’exploitant du transport ? »*

Exemples à dire :
- Planifier lignes / horaires (gestionnaire réseau)
- Piloter missions et véhicules (gestionnaire flotte)
- Gérer retards et déviations **sans arrêter** la mission
- Lier une mission à un incident G9 sans confondre les modèles

### 3. Intégration **technique**
Répondre : *« Comment G4 parle aux autres groupes ? »*

Exemples :
- Kafka : `vehicule-positions` (G7), `incident.transport.topic` (G9)
- HTTP : notifications G5, validation G3
- JWT commun, Docker `sgitu-network`, health/logs

### 4. Démonstration (soutenance finale)
Le prof dit aussi : *« synthèse technique **et démonstration** »*.

Sur **1 à 2 slides**, listez ce que vous ferez **en direct** (5–8 min) :
1. `docker compose up` → montrer conteneurs
2. Swagger ou Postman : créer une mission
3. `GET /api/g4/health` sans token
4. (Optionnel) Couper G5 → notification DEGRADED (Chaos Monkey)

---

## Fichier à produire chez vous

| Étape | Action |
|-------|--------|
| 1 | Ouvrir `PRESENTATION_G4.tex` (déjà prêt, 14 slides) |
| 2 | Compiler en PDF : Overleaf ou `pdflatex PRESENTATION_G4.tex` (2 fois) |
| 3 | Obtenir **`PRESENTATION_G4.pdf`** |
| 4 | Mettre ce PDF dans **`G4_SGITU_Final.zip`** (avec le rapport) |
| 5 | Le **1er juin** : projeter ce PDF pendant votre oral |

**Alternative :** recopier le plan des slides dans **PowerPoint** ou **Canva** si vous préférez un design plus visuel (schémas, captures d’écran).

---

## Plan des 14 slides (fichier `PRESENTATION_G4.tex`)

| # | Titre | Métier ou technique ? |
|---|--------|------------------------|
| 1 | Page de garde | — |
| 2 | Contexte SGITU | Les deux |
| 3 | **Valeur métier** — pourquoi G4 existe | **Métier** |
| 4 | **Cas d’usage** — scénario conducteur / flotte | **Métier** |
| 5 | Architecture technique | **Technique** |
| 6 | Intégration inter-groupes | **Technique** |
| 7 | UML (modèle) | Conception |
| 8 | API REST / Swagger | **Technique** |
| 9 | Sécurité JWT | **Technique** |
| 10 | Observabilité | **Technique** |
| 11 | Chaos Monkey | **Technique** + démo |
| 12 | **Plan de démonstration live** | **Démo soutenance** |
| 13 | Tests & validation | **Technique** |
| 14 | Conclusion + questions | — |

---

## Durée oral conseillée (~12 min)

| Temps | Partie |
|-------|--------|
| 2 min | Slides 1–4 : contexte + **métier** |
| 4 min | Slides 5–11 : **technique** + intégrations |
| 4 min | Slide 12 + **démo live** (Postman / Docker) |
| 2 min | Conclusion + questions |

---

## Erreurs fréquentes à éviter

- Confondre **rapport PDF** et **slides PDF** → deux fichiers différents
- 30 slides avec trop de code → max **15**
- Slides sans **démo** → le prof attend une **preuve live**
- Oublier d’exporter en **PDF** pour le ZIP
