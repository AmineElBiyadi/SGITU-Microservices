# G4 → G10 — Nouveaux endpoints à connaître (mai 2026)

**De :** Groupe G4 — Coordination des transports  
**Pour :** Groupe G10 — API Gateway  
**Fichier détaillé :** `docs/ENDPOINTS_G4_POUR_G10.md`

---

## Réponse courte

**4 nouveaux endpoints REST** sous `/api/g4/vehicules/**`.  
**Aucune modification de configuration gateway requise** si la route existante est déjà :

```yaml
Path=/api/g4/**,/api/v1/operator/**
uri: http://coordination-service:8084
```

---

## Nouveaux chemins (proxy tel quel)

| Méthode | Chemin G10 (client) | Auth JWT | Rôles écriture |
|---------|---------------------|----------|----------------|
| `GET` | `/api/g4/vehicules` | Oui | — (lecture) |
| `GET` | `/api/g4/vehicules/disponibles` | Oui | — |
| `GET` | `/api/g4/vehicules/{vehiculeId}` | Oui | — |
| `POST` | `/api/g4/vehicules/sync-from-g7/{vehiculeId}` | Oui | `DISPATCHER`, `G4_ADMIN` |

`vehiculeId` = **UUID** string (référentiel G7).

---

## Comportement métier impactant G10 (pas de nouveau route)

| Endpoint existant | Changement |
|-------------------|------------|
| `POST /api/g4/affectations` | `400` si véhicule inconnu / pas `DISPONIBLE` ; appelle G7 `EN_SERVICE` si `ACTIF` |
| `POST /api/g4/missions` | `400` si pas d'affectation `ACTIF` ou véhicule pas `EN_SERVICE` ; `409` inchangé |
| `GET /api/g4/missions/{id}/status` | Réponse enrichie : `vehiculeStatutG7` |

---

## Kafka (hors gateway HTTP)

| Topic | Producteur | Consommateur |
|-------|------------|--------------|
| `vehicle.registered` | G7 | G4 |
| `vehicule-positions` | G7 | G4 (inchangé) |

---

## Checklist G10

- [ ] Confirmer que `/api/g4/**` route vers G4:8084 (déjà le cas en standard).
- [ ] Forward header `Authorization: Bearer` sans le retirer.
- [ ] Pas d’exposition publique de `/actuator/prometheus` (inchangé).
- [ ] Documenter pour le front : ordre **véhicule G7 → affectation → mission**.

---

## Test via gateway

```http
GET http://localhost:8080/api/g4/vehicules/disponibles
Authorization: Bearer <token G3/G10>
```

Attendu : `200` + liste (éventuellement vide avant sync G7).

---

*G4 — Coordination des transports*
