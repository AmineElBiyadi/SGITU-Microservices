-- Table impacts incident G9 sur missions G4 (séparée de coordination_events)
CREATE TABLE IF NOT EXISTS mission_incident_impacts (
    id BIGSERIAL PRIMARY KEY,
    mission_id BIGINT REFERENCES missions(id),
    g9_reference_incident VARCHAR(128) NOT NULL,
    vehicule_id VARCHAR(64),
    g9_type VARCHAR(64),
    g9_statut VARCHAR(64),
    description VARCHAR(4000),
    payload_json TEXT,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mission_incident_impacts_mission ON mission_incident_impacts(mission_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_mission_incident_impacts_g9_ref ON mission_incident_impacts(g9_reference_incident);
