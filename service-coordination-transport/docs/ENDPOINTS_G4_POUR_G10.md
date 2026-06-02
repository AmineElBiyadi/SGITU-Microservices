# G4 — Coordination des transports — Catalogue endpoints (version finale)

**Destinataire :** G10 (API Gateway)  
**Service :** `g4-coordination-transport` (Groupe G4)  
**Version application :** `0.0.1-SNAPSHOT`  
**Base URL (Docker) :** `http://g4-coordination:8084`  
**Base URL (local) :** `http://localhost:8084`  
**OpenAPI :** `GET /v3/api-docs` — Swagger UI : `/swagger-ui.html`

---

## 1. Informations pour la passerelle G10

| Élément | Valeur |
|---------|--------|
| **Hostname Docker** | `g4-coordination` |
| **Port** | `8084` |
| **Context-path** | `/` (racine, pas de préfixe supplémentaire) |
| **Authentification** | JWT Bearer — header `Authorization: Bearer <token>` |
| **Émetteur JWT attendu** | G3 (Users) via G10 — secret partagé `SGITU_JWT_SECRET` |
| **Login dev local G4** | `POST /api/auth/login` (démo uniquement, pas en prod si G3 seul) |
| **CORS** | À configurer côté G10 si front web |

### Rôles G3 reconnus par G4

| Rôle JWT (claim `roles`) | Périmètre G4 |
|--------------------------|--------------|
| `ROLE_G4_OPERATOR` | CRUD réseau (lignes, trajets, arrêts, horaires) + lecture |
| `ROLE_DISPATCHER` | CRUD flotte (missions, affectations, events, notifications, incident-impacts) + lecture |
| `ROLE_G4_ADMIN` | Tous droits G4 + supervision |

> Accord G9 : `ROLE_DISPATCHER` peut aussi appeler G9 (incidents) — hors périmètre G4.

---

## 2. Endpoints publics (sans JWT)

| Méthode | Chemin | Description | Réponse typique |
|---------|--------|-------------|-----------------|
| `POST` | `/api/auth/login` | Login démo G4 (dev) | `200` + `{ token, type, expiresIn, role }` |
| `GET` | `/api/g4/health` | Santé service (DB, Kafka, pending notifs) | `200` JSON |
| `GET` | `/api/g4/logs` | Journal supervision structuré | `200` liste |
| `GET` | `/actuator/health` | Health Spring Boot | `200` |
| `GET` | `/actuator/info` | Info build | `200` |
| `GET` | `/actuator/prometheus` | Métriques Prometheus | `200` texte |
| `GET` | `/v3/api-docs` | OpenAPI JSON | `200` |
| `GET` | `/swagger-ui.html` | Interface Swagger | `200` HTML |
| `GET` | `/swagger-ui/**` | Assets Swagger | `200` |

---

## 3. Authentification

| Méthode | Chemin | Auth | Rôles | Body | Réponses |
|---------|--------|------|-------|------|----------|
| `POST` | `/api/auth/login` | Non | — | `{ "username", "password" }` | `200` token / `401` |

**Comptes démo (password : `password`) :**

| username | Rôle JWT |
|----------|----------|
| `gestionnaire.reseau` | `ROLE_G4_OPERATOR` |
| `gestionnaire.flotte` | `ROLE_DISPATCHER` |
| `admin.technique` | `ROLE_G4_ADMIN` |

---

## 4. Supervision G4

| Méthode | Chemin | Auth | Rôles | Réponses |
|---------|--------|------|-------|----------|
| `GET` | `/api/g4/health` | Non | — | `200` |
| `GET` | `/api/g4/logs` | Non | — | `200` |
| `GET` | `/api/v1/operator/status` | Oui | `G4_ADMIN` | `200` / `401` / `403` |
| `GET` | `/api/g4/pending-notifications` | Oui | `G4_ADMIN` | `200` |
| `POST` | `/api/g4/pending-notifications/retry` | Oui | `G4_ADMIN` | `200` |

---

## 5. Réseau — Lignes (`/api/g4/lignes`)

| Méthode | Chemin | Auth | Rôles écriture / lecture | Réponses |
|---------|--------|------|--------------------------|----------|
| `POST` | `/api/g4/lignes` | Oui | Écriture : `G4_OPERATOR`, `G4_ADMIN` | `201` / `400` / `403` |
| `GET` | `/api/g4/lignes` | Oui | Lecture : tous rôles G4 | `200` |
| `GET` | `/api/g4/lignes/actives` | Oui | Lecture | `200` |
| `GET` | `/api/g4/lignes/{ligneId}` | Oui | Lecture | `200` / `404` |
| `PUT` | `/api/g4/lignes/{ligneId}` | Oui | `G4_OPERATOR`, `G4_ADMIN` | `200` / `404` |
| `DELETE` | `/api/g4/lignes/{ligneId}` | Oui | `G4_OPERATOR`, `G4_ADMIN` | `204` / `404` |
| `GET` | `/api/g4/lignes/{ligneId}/trajets` | Oui | Lecture | `200` |

---

## 6. Réseau — Trajets (`/api/g4/trajets`)

| Méthode | Chemin | Auth | Rôles écriture | Réponses |
|---------|--------|------|----------------|----------|
| `POST` | `/api/g4/trajets` | Oui | `G4_OPERATOR`, `G4_ADMIN` | `201` / `400` |
| `GET` | `/api/g4/trajets` | Oui | Lecture tous | `200` |
| `GET` | `/api/g4/trajets/{trajetId}` | Oui | Lecture | `200` / `404` |
| `GET` | `/api/g4/trajets/{trajetId}/arrets` | Oui | Lecture | `200` |
| `PUT` | `/api/g4/trajets/{trajetId}` | Oui | `G4_OPERATOR`, `G4_ADMIN` | `200` |
| `DELETE` | `/api/g4/trajets/{trajetId}` | Oui | `G4_OPERATOR`, `G4_ADMIN` | `204` |

---

## 7. Réseau — Arrêts (`/api/g4/arrets`)

| Méthode | Chemin | Auth | Rôles écriture | Réponses |
|---------|--------|------|----------------|----------|
| `POST` | `/api/g4/arrets` | Oui | `G4_OPERATOR`, `G4_ADMIN` | `201` |
| `GET` | `/api/g4/arrets` | Oui | Lecture | `200` |
| `GET` | `/api/g4/arrets/{arretId}` | Oui | Lecture | `200` / `404` |
| `PUT` | `/api/g4/arrets/{arretId}` | Oui | `G4_OPERATOR`, `G4_ADMIN` | `200` |
| `DELETE` | `/api/g4/arrets/{arretId}` | Oui | `G4_OPERATOR`, `G4_ADMIN` | `204` |
| `GET` | `/api/g4/arrets/ligne/{ligneId}` | Oui | Lecture | `200` |

---

## 8. Réseau — Horaires (`/api/g4/horaires`)

| Méthode | Chemin | Auth | Rôles écriture | Réponses |
|---------|--------|------|----------------|----------|
| `POST` | `/api/g4/horaires` | Oui | `G4_OPERATOR`, `G4_ADMIN` | `201` |
| `GET` | `/api/g4/horaires` | Oui | Lecture | `200` |
| `GET` | `/api/g4/horaires/{horaireId}` | Oui | Lecture | `200` / `404` |
| `PUT` | `/api/g4/horaires/{horaireId}` | Oui | `G4_OPERATOR`, `G4_ADMIN` | `200` |
| `DELETE` | `/api/g4/horaires/{horaireId}` | Oui | `G4_OPERATOR`, `G4_ADMIN` | `204` |

---

## 9. Flotte — Missions (`/api/g4/missions`)

| Méthode | Chemin | Auth | Rôles écriture | Réponses |
|---------|--------|------|----------------|----------|
| `POST` | `/api/g4/missions` | Oui | `DISPATCHER`, `G4_ADMIN` | `201` / `400` (UUID invalide, pas d'affectation `ACTIF`, véhicule pas `EN_SERVICE`) / **409** |
| `GET` | `/api/g4/missions` | Oui | Lecture | `200` |
| `GET` | `/api/g4/missions/actives` | Oui | Lecture | `200` |
| `GET` | `/api/g4/missions/{missionId}` | Oui | Lecture | `200` / `404` |
| `GET` | `/api/g4/missions/{missionId}/status` | Oui | Lecture | `200` |
| `PUT` | `/api/g4/missions/{missionId}` | Oui | `DISPATCHER`, `G4_ADMIN` | `200` / **409** |
| `POST` | `/api/g4/missions/{missionId}/cloturer` | Oui | `DISPATCHER`, `G4_ADMIN` | `200` |
| `POST` | `/api/g4/missions/{missionId}/annuler` | Oui | `DISPATCHER`, `G4_ADMIN` | `200` |

**Champ optionnel :** `chauffeurId` (référence conducteur G3).

---

## 10. Référentiel véhicules G7 (`/api/g4/vehicules`) — **NOUVEAU**

Alimenté par Kafka `vehicle.registered` (G7) ou sync REST. `vehiculeId` = **UUID G7** (string).

| Méthode | Chemin | Auth | Rôles écriture | Réponses |
|---------|--------|------|----------------|----------|
| `GET` | `/api/g4/vehicules` | Oui | Lecture (`G4_OPERATOR`, `DISPATCHER`, `G4_ADMIN`) | `200` |
| `GET` | `/api/g4/vehicules/disponibles` | Oui | Lecture | `200` — statut G7 `DISPONIBLE` |
| `GET` | `/api/g4/vehicules/{vehiculeId}` | Oui | Lecture | `200` / `400` |
| `POST` | `/api/g4/vehicules/sync-from-g7/{vehiculeId}` | Oui | `DISPATCHER`, `G4_ADMIN` | `200` — `GET` G7 puis upsert référentiel |

> **Routage G10 :** déjà couvert par le prédicat existant `Path=/api/g4/**` — **aucune route gateway à ajouter**.

---

## 11. Flotte — Affectations (`/api/g4/affectations`)

| Méthode | Chemin | Auth | Rôles écriture | Réponses |
|---------|--------|------|----------------|----------|
| `POST` | `/api/g4/affectations` | Oui | `DISPATCHER`, `G4_ADMIN` | `201` / `400` (véhicule inconnu ou pas `DISPONIBLE`) |
| `GET` | `/api/g4/affectations` | Oui | Lecture | `200` |
| `GET` | `/api/g4/affectations/{affectationId}` | Oui | Lecture | `200` |
| `GET` | `/api/g4/affectations/vehicule/{vehiculeId}` | Oui | Lecture | `200` |
| `PUT` | `/api/g4/affectations/{affectationId}` | Oui | `DISPATCHER`, `G4_ADMIN` | `200` |
| `DELETE` | `/api/g4/affectations/{affectationId}` | Oui | `DISPATCHER`, `G4_ADMIN` | `204` |

---

## 12. Événements de coordination (`/api/g4/events`)

| Méthode | Chemin | Auth | Rôles | Réponses |
|---------|--------|------|-------|----------|
| `POST` | `/api/g4/events` | Oui | `DISPATCHER`, `G4_ADMIN` | `201` |
| `GET` | `/api/g4/events` | Oui | Lecture | `200` |
| `GET` | `/api/g4/events/{eventId}` | Oui | Lecture | `200` |
| `GET` | `/api/g4/events/type/{eventType}` | Oui | Lecture | `200` |
| `GET` | `/api/g4/events/status/{status}` | Oui | Lecture | `200` |
| `POST` | `/api/g4/events/detect-delay` | Oui | `DISPATCHER`, `G4_ADMIN` | `201` |
| `POST` | `/api/g4/events/detect-deviation` | Oui | `DISPATCHER`, `G4_ADMIN` | `201` |
| `POST` | `/api/g4/events/detect-breakdown` | Oui | `DISPATCHER`, `G4_ADMIN` | `201` |
| `POST` | `/api/g4/events/cancel-mission` | Oui | `DISPATCHER`, `G4_ADMIN` | `201` |

**Types événement :** `RETARD`, `DEVIATION`, `PANNE`, `ANNULATION`, etc. — mission reste en général `EN_COURS`.

---

## 13. Impacts incident G9 (`/api/g4/incident-impacts`)

| Méthode | Chemin | Auth | Rôles | Réponses |
|---------|--------|------|-------|----------|
| `POST` | `/api/g4/incident-impacts` | Oui | `DISPATCHER`, `G4_ADMIN` | `201` |
| `GET` | `/api/g4/incident-impacts` | Oui | Lecture | `200` |
| `GET` | `/api/g4/incident-impacts/{impactId}` | Oui | Lecture | `200` |
| `GET` | `/api/g4/incident-impacts/mission/{missionId}` | Oui | Lecture | `200` |

> Distinct des événements G4 — lien mission ↔ référence incident **G9** (Kafka ou REST).

---

## 14. Notifications vers G5 (`/api/notifications`)

| Méthode | Chemin | Auth | Rôles | Réponses |
|---------|--------|------|-------|----------|
| `POST` | `/api/notifications/send` | Oui | `DISPATCHER`, `G4_ADMIN` | **202** `ACCEPTED` ou **202** `DEGRADED` si G5 down |

G4 appelle G5 via gateway (`SGITU_G10_URL` + path notification). Pas de `500` brut si G5 injoignable.

---

## 15. API de référence G7 (`/api/v1`) — lecture réseau

Contrat **lecture seule** pour intégration G7 (positions GPS via Kafka).

| Méthode | Chemin | Auth | Rôles | Réponses |
|---------|--------|------|-------|----------|
| `GET` | `/api/v1/lignes` | Oui | Lecture tous | `200` |
| `GET` | `/api/v1/lignes/{id}/trajet` | Oui | Lecture | `200` |
| `GET` | `/api/v1/lignes/{id}/horaires` | Oui | Lecture | `200` |
| `GET` | `/api/v1/arrets` | Oui | Lecture | `200` |
| `GET` | `/api/v1/arrets/{id}` | Oui | Lecture | `200` |

---

## 16. Synthèse des compteurs

| Catégorie | Nombre d'endpoints |
|-----------|-------------------|
| Public (auth, health, docs, actuator) | 9 + actuator metrics |
| Lignes | 7 |
| Trajets | 6 |
| Arrêts | 6 |
| Horaires | 5 |
| **Véhicules G7 (référentiel)** | **4** |
| Missions | 9 |
| Affectations | 6 |
| Events | 10 |
| Incident-impacts | 4 |
| Notifications | 1 |
| Pending-notifications | 2 |
| API v1 (G7 ref) | 5 |
| Operator status | 1 |
| **Total endpoints métier REST** | **66** |
| **+ supervision / auth / doc** | **~76** routes HTTP |

---

## 17. Codes HTTP utilisés par G4

| Code | Usage |
|------|--------|
| `200` | OK lecture / mise à jour |
| `201` | Création |
| `202` | Notification acceptée / dégradée |
| `204` | Suppression |
| `400` | Validation / métier (UUID G7, affectation manquante, chauffeur G3) |
| `401` | JWT absent ou invalide |
| `403` | Rôle insuffisant |
| `404` | Ressource introuvable |
| `409` | Conflit (ex. véhicule déjà en mission `EN_COURS`) |

---

## 18. Intégrations asynchrones (hors REST G10 — info)

| Sens | Techno | Topic / cible |
|------|--------|---------------|
| G7 → G4 | Kafka consumer | `vehicle.registered` |
| G7 → G4 | Kafka consumer | `vehicule-positions` |
| G9 → G4 | Kafka consumer | `incident.transport.topic` |
| G4 → G1 | Kafka producer | `missions-lifecycle` |
| G4 → G5 | HTTP (via G10) | `POST` path configuré `SGITU_INTEGRATION_G5_NOTIFICATION_PATH` |
| G4 → G3 | HTTP optionnel | `GET /api/users/drivers/ids` (validation chauffeur) |

---

## 19. Exemple routage G10 (suggestion)

```yaml
# Exemple — à adapter au format réel de G10
routes:
  - id: g4-api
    uri: http://g4-coordination:8084
    predicates:
      - Path=/api/g4/**, /api/notifications/**, /api/v1/**
    filters:
      - PreserveHostHeader
      # JWT forward : ne pas strip Authorization
```

**Routes à exposer via G10 :**

- `/api/g4/**`
- `/api/notifications/**` (proxy vers G4, G4 relaye G5)
- `/api/v1/**` (référence G7)
- Optionnel dev : `/api/auth/login` si login centralisé chez G3 uniquement, désactiver route login G4 en prod

**Ne pas exposer publiquement sans contrôle :** `/actuator/prometheus` (réseau interne / monitoring seulement).

---

## 20. Fichiers de référence dans le repo G4

| Fichier | Contenu |
|---------|---------|
| `docs/ROLES_G3_G4_ALIGNMENT.md` | Matrice rôles |
| `docs/CONTRATS_ALIGNES_G4.md` | Hostnames, topics Kafka |
| `docs/LIVRABLE_G10_NOUVEAUX_ENDPOINTS_G4.md` | **Résumé 1 page pour G10** (nouveaux `/api/g4/vehicules`) |
| `postman/SGITU-G4-Coordination-Transport.postman_collection.json` | Tests complets |
| `/v3/api-docs` | OpenAPI machine-readable |

---

## 21. Contact / validation

- **Groupe :** G4 — Coordination des transports (ENSA Tétouan, GI2)
- **Tests :** `mvn test` + collection Postman
- **Health check gateway :** `GET http://g4-coordination:8084/api/g4/health` → `"status":"UP"`

*Document généré à partir du code source `src/main/java/com/sgitu/g4/controller/*` et `SecurityConfig.java` — mai 2026.*
