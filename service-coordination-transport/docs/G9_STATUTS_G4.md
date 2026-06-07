# G9 → G4 — Gestion des statuts (implémenté)

Topic : `incident.transport.topic`

| Statut G9 | Action G4 |
|-----------|-----------|
| **CONFIRME** | Base + **G5** (`INCIDENT_CONFIRMED`) + **G7** `INCIDENT` + log perturbation |
| **RESOLU** | Base + **G7** `EN_SERVICE` (si mission EN_COURS) ou `DISPONIBLE` + log clôture |
| **REJETE** | Base + log fausse alerte (pas G5, pas G7) |

Exemples Kafka : `postman/examples/kafka-g9-incident-*.json`

JWT promo (aligné G3) : `SGITU_JWT_SECRET=SGITU_G3_JWT_SECRET_KEY_CHANGE_ME_IN_PRODUCTION_256BITS!!`
