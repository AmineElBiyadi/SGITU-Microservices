# Base de données G4 — scripts

## Fichiers

| Fichier | Rôle |
|---------|------|
| `postgresql-seed-demo-g4.sql` | **Jeu de démo complet** (réseau + véhicule + mission) |
| `postgresql-vehicules-referentiel.sql` | DDL optionnel référentiel véhicules |
| `postgresql-mission-incident-impacts.sql` | DDL impacts G9 |

Les tables métier sont en général créées par **Spring JPA** (`ddl-auto=update`).

## Charger la démo

```powershell
cd service-coordination-transport
docker compose up -d postgres
powershell -ExecutionPolicy Bypass -File scripts/seed-database-g4.ps1
```

Ou manuellement :

```bash
docker exec -i sgitu-g4-postgres psql -U g4 -d sgitu_g4 < src/main/resources/db/postgresql-seed-demo-g4.sql
```

## Contenu du seed

| Élément | ID / valeur |
|---------|-------------|
| Ligne | `1` — code `L12` |
| Arrêts | 3 (avec lat/long) |
| Trajet | `1` — L12-ALLER |
| Horaires | 08:00, 08:15, 08:30 |
| Véhicule principal | `53c31262-591a-44d4-8872-51e84611ac5e` (EN_SERVICE) |
| Véhicule 409 test | `00000000-0000-4000-8000-000000000001` (DISPONIBLE) |
| Affectation | `1` ACTIF |
| Mission | `1` EN_COURS |

## Prérequis détection auto (Kafka)

- `SGITU_KAFKA_ENABLED=true`
- Mission `1` en `EN_COURS` avec `trajet_id=1`
- Messages `vehicule-positions` pour le même `vehiculeId`

Voir les exemples JSON en fin de `postgresql-seed-demo-g4.sql`.
