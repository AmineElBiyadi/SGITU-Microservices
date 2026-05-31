package com.sgitu.g4.entity;

/**
 * Perturbations opérationnelles G4 / détection G7 — pas les incidents métier G9
 * (voir {@link MissionIncidentImpact}).
 */
public enum CoordinationEventType {
	RETARD,
	DEVIATION,
	PANNE,
	ANNULATION_MISSION
}
