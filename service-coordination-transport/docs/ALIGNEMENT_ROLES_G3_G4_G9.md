# Alignement des rôles G3 — G4 — G9 (accord inter-groupes)

**Date de décision :** mai 2026  
**Groupes :** G4 Coordination transport, G9 Gestion des incidents, G3 Utilisateurs (source JWT)

---

## Décision retenue

Si **la même personne métier** (gestionnaire de régulation / flotte) envoie des requêtes HTTP vers **G4** (missions, événements) et **G9** (incidents) :

→ **Un seul rôle dans le JWT G3 : `ROLE_DISPATCHER`**

→ **Pas** de rôles dupliqués par microservice (`G4_DISPATCHER`, `G9_DISPATCHER`).

Chaque service (**G4**, **G9**) applique **localement** ce que `ROLE_DISPATCHER` (et les autres rôles G3) peut faire sur **ses propres endpoints**.

---

## Principe

```text
G3 (annuaire + rôles)
    │
    ▼  JWT : roles = [ "ROLE_DISPATCHER", ... ]
    │
    ├──► G4  → SecurityConfig : missions, events, incident-impacts
    │
    └──► G9  → SecurityConfig G9 : CRUD incidents, suivi, etc.
```

| Couche | Responsabilité |
|--------|----------------|
| **G3** | Définit les rôles **métier** (`ROLE_DISPATCHER`, `ROLE_OPERATOR`, …) |
| **G10** | Transmet le JWT inchangé |
| **G4 / G9** | Filtrage **local** par endpoint (pas de confiance aveugle) |

---

## Rôles G3 recommandés (matrice commune)

| Rôle G3 | Profil métier | G4 (coordination) | G9 (incidents) — proposition alignée |
|---------|---------------|-------------------|--------------------------------------|
| `ROLE_OPERATOR` | Gestionnaire **réseau** (lignes, horaires) | CRUD offre transport | Lecture seule ou aucun accès |
| `ROLE_DISPATCHER` | Gestionnaire **flotte + régulation** (missions + incidents opérationnels) | CRUD missions, events, **incident-impacts** | CRUD / suivi incidents |
| `ROLE_TECHNICIAN` | Technicien **incidents only** (sans pilotage flotte) | Pas d’écriture flotte | Incidents techniques |
| `ROLE_DRIVER` | Conducteur | Référence `chauffeurId` | — |
| `ROLE_ADMIN` | Admin SI global | Tous droits G4 | Tous droits G9 |
| `ROLE_ADMIN_G4` / `ROLE_ADMIN_G9` | Admin par microservice | Supervision G4 | Admin G9 |

> **À éviter** sauf besoin métier explicite : `G4_DISPATCHER`, `G9_DISPATCHER` (doublons inutiles si même utilisateur).

---

## Cas d’usage : un dispatcher, deux services

1. Login **G3** (ou via **G10**) → JWT avec `ROLE_DISPATCHER`.
2. `POST /api/g4/missions` — créer une mission (G4).
3. `POST /api/g9/...` — déclarer un incident (G9, endpoints selon leur API).
4. G9 publie sur Kafka `incident.transport.topic` → G4 enregistre un **impact** mission (`MissionIncidentImpact`).
5. Même Bearer token pour toutes les requêtes.

---

## Implémentation côté G4 (déjà en place)

| Élément | Détail |
|---------|--------|
| Fichier | `SecurityConfig.java` |
| Rôle flotte | `hasRole("DISPATCHER")` pour missions, affectations, events, notifications |
| Lien incident G9 | `POST/GET /api/g4/incident-impacts` → **DISPATCHER**, G4_ADMIN |
| Doc détaillée | `docs/ROLES_G3_G4_ALIGNMENT.md` |

Compte démo local : `gestionnaire.flotte` / `password` → `ROLE_DISPATCHER`.

---

## Ce que G9 doit faire de son côté

1. Accepter le **même JWT** (secret `SGITU_JWT_SECRET` partagé).
2. Mapper `ROLE_DISPATCHER` sur leurs endpoints incidents (équivalent métier).
3. Ne **pas** exiger un rôle spécifique `G9_DISPATCHER` si l’utilisateur est déjà `DISPATCHER` chez G3.
4. (Optionnel) Réserver `ROLE_TECHNICIAN` pour les profils **incidents uniquement**, sans accès missions G4.

---

## Message type pour le groupe G9

> Un seul `ROLE_DISPATCHER` dans G3 suffit quand la même personne gère missions (G4) et incidents (G9). Chaque microservice applique localement les autorisations sur ses API. G4 autorise déjà `DISPATCHER` sur les incident-impacts liés aux missions.

---

## Intégration / soutenance

| Test | Preuve |
|------|--------|
| Token G3 avec `ROLE_DISPATCHER` → `GET /api/g4/missions` | 200 |
| Même token → endpoint G9 protégé | 200 (si G9 aligné) |
| `ROLE_G4_OPERATOR` → `POST /api/g4/missions` | **403** sur G4 |

Voir aussi : `docs/SECURITE_END_TO_END_G4.md`, `docs/VALIDATION_CROISEE_G4.md`.

---

## Références

- `docs/ROLES_G3_G4_ALIGNMENT.md` — matrice endpoints G4
- `docs/CONTRATS_ALIGNES_G4.md` — Kafka G9 `incident.transport.topic`
- `docs/diagrams/mission-incident-vs-coordination.puml` — séparation modèle incident vs événement G4
