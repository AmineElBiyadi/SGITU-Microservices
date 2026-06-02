-- =============================================================================
-- SGITU G4 — Jeu de données de démonstration (PostgreSQL)
-- =============================================================================
-- Prérequis : tables créées par Spring JPA (ddl-auto=update) ou migrations.
-- Usage :
--   docker exec -i sgitu-g4-postgres psql -U g4 -d sgitu_g4 < src/main/resources/db/postgresql-seed-demo-g4.sql
-- Ou : psql -h localhost -p 5434 -U g4 -d sgitu_g4 -f ...
--
-- Contenu :
--   • Ligne L12 + 3 arrêts GPS (Tétouan) + trajet + horaires
--   • Véhicule UUID G7 + affectation ACTIF + mission EN_COURS
--   • Véhicule Postman secondaire DISPONIBLE (tests 409)
-- =============================================================================

BEGIN;

-- Nettoyage (ordre respectant les FK)
TRUNCATE TABLE
    coordination_events,
    mission_incident_impacts,
    missions,
    affectations_vehicule_ligne,
    horaires,
    trajet_arret,
    trajets,
    arrets,
    lignes,
    vehicules_referentiel,
    pending_notifications
RESTART IDENTITY CASCADE;

-- -----------------------------------------------------------------------------
-- 1. Réseau G4 — Ligne L12
-- -----------------------------------------------------------------------------
INSERT INTO lignes (id, code, nom, description, active, created_at, updated_at)
VALUES (
    1,
    'L12',
    'Ligne Université',
    'Gare routière → Centre → Campus (démo SGITU G4)',
    TRUE,
    NOW(),
    NOW()
);

-- Arrêts avec coordonnées (détection auto retard / déviation)
INSERT INTO arrets (id, code, nom, latitude, longitude, ligne_id, created_at, updated_at)
VALUES
    (1, 'ARR-GARE', 'Gare routière', 35.5780000, -5.3680000, 1, NOW(), NOW()),
    (2, 'ARR-CENTRE', 'Centre ville', 35.5850000, -5.3600000, 1, NOW(), NOW()),
    (3, 'ARR-CAMPUS', 'Campus université', 35.5920000, -5.3520000, 1, NOW(), NOW());

INSERT INTO trajets (id, ligne_id, code, nom, sens, actif, created_at, updated_at)
VALUES (
    1,
    1,
    'L12-ALLER',
    'L12 sens aller matin',
    'ALLER',
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO trajet_arret (id, trajet_id, arret_id, sequence_order, created_at, updated_at)
VALUES
    (1, 1, 1, 1, NOW(), NOW()),
    (2, 1, 2, 2, NOW(), NOW()),
    (3, 1, 3, 3, NOW(), NOW());

-- Horaires passage (arrêt 2 = Centre — utilisé pour retard auto si GPS proche + heure dépassée)
INSERT INTO horaires (id, trajet_id, arret_id, heure_passage, jour_semaine, valid_from, valid_to, libelle, created_at, updated_at)
VALUES
    (1, 1, 1, '08:00:00', NULL, NULL, NULL, 'Passage gare', NOW(), NOW()),
    (2, 1, 2, '08:15:00', NULL, NULL, NULL, 'Passage centre', NOW(), NOW()),
    (3, 1, 3, '08:30:00', NULL, NULL, NULL, 'Passage campus', NOW(), NOW());

-- -----------------------------------------------------------------------------
-- 2. Véhicules G7 (référentiel local)
-- -----------------------------------------------------------------------------
-- UUID aligné contrat G7 / Postman
INSERT INTO vehicules_referentiel (
    vehicule_id, immatriculation, type_vehicule, statut_g7,
    disponible_pour_affectation, ligne_affectee_id, registered_at, updated_at
)
VALUES
    (
        '53c31262-591a-44d4-8872-51e84611ac5e',
        'BUS-G4-001',
        'BUS',
        'EN_SERVICE',
        FALSE,
        1,
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-4000-8000-000000000001',
        'BUS-DEMO-002',
        'BUS',
        'DISPONIBLE',
        TRUE,
        NULL,
        NOW(),
        NOW()
    );

-- -----------------------------------------------------------------------------
-- 3. Affectation + mission (démo flux complet)
-- -----------------------------------------------------------------------------
INSERT INTO affectations_vehicule_ligne (
    id, vehicule_id, chauffeur_id, ligne_id, date_debut, date_fin, statut, commentaire, created_at, updated_at
)
VALUES (
    1,
    '53c31262-591a-44d4-8872-51e84611ac5e',
    '42',
    1,
    NOW() - INTERVAL '1 hour',
    NULL,
    'ACTIF',
    'Affectation démo L12',
    NOW(),
    NOW()
);

INSERT INTO missions (
    id, vehicule_id, chauffeur_id, ligne_id, trajet_id, affectation_id,
    statut, planned_start, actual_start, ended_at, notes, created_at, updated_at
)
VALUES (
    1,
    '53c31262-591a-44d4-8872-51e84611ac5e',
    '42',
    1,
    1,
    1,
    'EN_COURS',
    DATE_TRUNC('day', NOW() AT TIME ZONE 'UTC') + TIME '08:00:00',
    NOW() - INTERVAL '30 minutes',
    NULL,
    'Mission démo — tests Kafka retard/déviation',
    NOW(),
    NOW()
);

-- Séquences PostgreSQL
SELECT setval(pg_get_serial_sequence('lignes', 'id'), (SELECT MAX(id) FROM lignes));
SELECT setval(pg_get_serial_sequence('arrets', 'id'), (SELECT MAX(id) FROM arrets));
SELECT setval(pg_get_serial_sequence('trajets', 'id'), (SELECT MAX(id) FROM trajets));
SELECT setval(pg_get_serial_sequence('trajet_arret', 'id'), (SELECT MAX(id) FROM trajet_arret));
SELECT setval(pg_get_serial_sequence('horaires', 'id'), (SELECT MAX(id) FROM horaires));
SELECT setval(pg_get_serial_sequence('affectations_vehicule_ligne', 'id'), (SELECT MAX(id) FROM affectations_vehicule_ligne));
SELECT setval(pg_get_serial_sequence('missions', 'id'), (SELECT MAX(id) FROM missions));

COMMIT;

-- =============================================================================
-- Tests Kafka vehicule-positions (exemples)
-- -----------------------------------------------------------------------------
-- RETARD auto : GPS proche arrêt Centre (35.585, -5.36), timestamp > 08:15 UTC + 5 min
-- {
--   "vehiculeId": "53c31262-591a-44d4-8872-51e84611ac5e",
--   "lat": 35.585,
--   "long": -5.360,
--   "ligneId": "L12",
--   "timestamp": "2026-05-20T08:25:00Z"
-- }
--
-- DEVIATION auto : GPS loin du tracé (> 150 m)
-- {
--   "vehiculeId": "53c31262-591a-44d4-8872-51e84611ac5e",
--   "lat": 35.650,
--   "long": -5.300,
--   "ligneId": "L12",
--   "timestamp": "2026-05-20T08:20:00Z"
-- }
-- =============================================================================
