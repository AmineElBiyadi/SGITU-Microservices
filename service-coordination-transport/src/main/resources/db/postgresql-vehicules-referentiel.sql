-- Référentiel véhicules G7 (alimenté par Kafka vehicle.registered ou sync REST G4)
CREATE TABLE IF NOT EXISTS vehicules_referentiel (
    vehicule_id VARCHAR(64) PRIMARY KEY,
    immatriculation VARCHAR(32),
    type_vehicule VARCHAR(32),
    statut_g7 VARCHAR(32) NOT NULL,
    disponible_pour_affectation BOOLEAN NOT NULL DEFAULT TRUE,
    ligne_affectee_id BIGINT,
    registered_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ
);
