# Validation croisée — Pilier 3 (G4)

## Objectif

Fournir une **capture Postman** prouvant qu’un JWT valide permet d’appeler un service d’un autre groupe.

## Scénario A — G3 émet le JWT → G4 consomme (recommandé prof)

### Prérequis

- G3 up : `http://localhost:8083`
- G4 up : `http://localhost:8084`
- **Même `JWT_SECRET`** sur G3 et G4 (variable `SGITU_JWT_SECRET` / `JWT_SECRET`)

### Étapes Postman

1. **Login G3**  
   `POST http://localhost:8083/api/auth/login`  
   Body : `{ "username": "...", "password": "..." }`  
   → Copier `token` de la réponse

2. **Appel G4 avec token G3**  
   `GET http://localhost:8084/api/g4/missions`  
   Header : `Authorization: Bearer {{tokenG3}}`  
   → **200 OK** (preuve validation croisée)

3. **Capture** : fenêtre Postman montrant requête + réponse 200 + onglet Authorization

### Fichier capture

Enregistrer dans : `rapport/captures/10_validation_croisee_g3_g4.png`

---

## Scénario B — G4 émet le JWT → test local

1. `POST {{baseUrl}}/api/auth/login` — compte `gestionnaire.flotte` / `password`
2. `GET {{baseUrl}}/api/g4/missions` avec Bearer auto
3. Capture : `rapport/captures/11_validation_jwt_g4_local.png`

> Pour la soutenance, privilégier le **scénario A** (inter-groupe réel).

---

## Scénario C — G4 appelle G3 (validation chauffeur)

1. Activer : `SGITU_G3_VALIDATION_ENABLED=true`
2. `POST /api/g4/missions` avec `chauffeurId` existant côté G3
3. Capture logs G4 montrant appel `GET /api/users/drivers/ids`

---

## Scénario D — Via G10 Gateway (si disponible)

1. Login via G10
2. `GET http://localhost:8090/api/g4/missions` (route gateway vers G4)
3. Capture : `rapport/captures/12_validation_via_g10.png`

---

## Collection Postman

Dossier **« 99 — Validation croisée »** dans  
`postman/SGITU-G4-Coordination-Transport.postman_collection.json`

Variables à configurer :

| Variable | Exemple |
|----------|---------|
| `g3BaseUrl` | `http://localhost:8083` |
| `baseUrl` | `http://localhost:8084` |
| `g3AccessToken` | (rempli après login G3) |

---

## Critères d’acceptation prof

- [ ] JWT visible (header Authorization)
- [ ] Code **200** ou **201** (pas 401/403)
- [ ] Nom des groupes identifiables (G3 → G4)
- [ ] Capture incluse dans le rapport PDF et le ZIP final
