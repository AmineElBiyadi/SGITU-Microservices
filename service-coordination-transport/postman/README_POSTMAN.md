# Postman — G4 Coordination des transports

## Fichier à importer

`SGITU-G4-Coordination-Transport.postman_collection.json`

## Avant de tester

```powershell
cd service-coordination-transport
docker compose up -d --build
```

Attendre **GET** `http://localhost:8084/api/g4/health` → `UP`.

## Ordre recommandé

1. **00 — Démarrage** → health
2. **GUIDE — Parcours complet (1→13)** → tout le scénario dans le bon ordre (recommandé)
3. Ou dossiers **02 → 12** avec le bon login à chaque fois

## Logins (mot de passe : `password`)

| Compte | Rôle | Dossiers |
|--------|------|----------|
| `gestionnaire.reseau` | G4_OPERATOR | 02–05 Lignes, arrêts, trajets, horaires |
| `gestionnaire.flotte` | DISPATCHER | 06–10 Missions, events, G9, notifications |
| `admin.technique` | G4_ADMIN | 12 Supervision (pending, operator) |

## Variables automatiques

Après chaque **POST créer**, l’`id` de la réponse est enregistré dans `ligneId`, `arretId`, `missionId`, etc.

## Regénérer la collection

```powershell
node postman/build-collection.js
```
