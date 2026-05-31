# Alignement des rôles G3 ↔ G4 ↔ G10

**Nomenclature officielle G3 (mai 2026)** — 3 rôles pour G4 :

| Rôle JWT (G3) | Dans Spring `hasRole()` | Métier G4 |
|---------------|-------------------------|-----------|
| `ROLE_G4_OPERATOR` | `G4_OPERATOR` | Gestionnaire **réseau** |
| `ROLE_DISPATCHER` | `DISPATCHER` | Gestionnaire **flotte** (+ accord G9 incidents) |
| `ROLE_G4_ADMIN` | `G4_ADMIN` | Admin technique **G4** |

> **Accord G4 ↔ G9 :** un seul `ROLE_DISPATCHER` partagé — voir `docs/ALIGNEMENT_ROLES_G3_G4_G9.md`.

Les rôles sont **définis dans G3** (`service-utilisateur`). G10 émet le JWT ; G4 **valide** et applique les règles ci-dessous.

## Comptes démo G4 (`POST /api/auth/login`, dev uniquement)

| Compte | Mot de passe | Rôle JWT |
|--------|--------------|----------|
| `gestionnaire.reseau` | `password` | `ROLE_G4_OPERATOR` |
| `gestionnaire.flotte` | `password` | `ROLE_DISPATCHER` |
| `admin.technique` | `password` | `ROLE_G4_ADMIN` |

## Matrice des endpoints G4

| Périmètre | Rôles autorisés |
|-----------|-----------------|
| Lecture `GET /api/g4/**`, `GET /api/v1/**` | `G4_OPERATOR`, `DISPATCHER`, `G4_ADMIN` |
| CRUD lignes / trajets / arrêts / horaires | `G4_OPERATOR`, `G4_ADMIN` |
| CRUD missions / affectations / events / notifications | `DISPATCHER`, `G4_ADMIN` |
| CRUD **incident-impacts** | `DISPATCHER`, `G4_ADMIN` |
| Supervision (`pending-notifications`, `operator/status`) | `G4_ADMIN` |
| Public sans token | `/api/auth/login`, `/api/g4/health`, `/api/g4/logs`, Swagger, `GET /actuator/health` |

## Chaîne JWT

1. **G3** : utilisateur + rôles `ROLE_G4_OPERATOR`, `ROLE_DISPATCHER`, `ROLE_G4_ADMIN`.
2. **G10** : login → JWT signé, claim `roles`.
3. **G4** : `JwtAuthenticationFilter` + `hasRole("G4_OPERATOR")` etc.

Secret : `SGITU_JWT_SECRET` identique G3 / G10 / G4.

## Autres rôles G3 (hors périmètre écriture G4)

`ROLE_PASSENGER`, `ROLE_STUDENT`, `ROLE_DRIVER`, `ROLE_OPERATOR` (générique), `ROLE_TECHNICIAN`, `ROLE_SUPERVISOR`, … — pas d’accès CRUD G4 sauf configuration future explicite.

## Fichier code

`src/main/java/com/sgitu/g4/config/SecurityConfig.java`
