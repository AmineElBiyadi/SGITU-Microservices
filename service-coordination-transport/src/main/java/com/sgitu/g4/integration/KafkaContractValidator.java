package com.sgitu.g4.integration;

import com.sgitu.g4.dto.G7VehiclePositionMessage;
import com.sgitu.g4.dto.G9IncidentKafkaMessage;
import org.springframework.util.StringUtils;

/**
 * Validation minimale des payloads Kafka pour garantir l'alignement contrat producteur/consommateur.
 */
public final class KafkaContractValidator {

	private KafkaContractValidator() {
	}

	public static void validateG7Position(G7VehiclePositionMessage message) {
		if (message == null) {
			throw new IllegalArgumentException("Message G7 vide");
		}
		if (!StringUtils.hasText(message.getVehiculeId())) {
			throw new IllegalArgumentException("G7: vehiculeId obligatoire");
		}
		if (message.getLat() == null) {
			throw new IllegalArgumentException("G7: lat obligatoire");
		}
		if (message.getLongitude() == null) {
			throw new IllegalArgumentException("G7: long obligatoire");
		}
	}

	public static void validateG9Incident(G9IncidentKafkaMessage message) {
		if (message == null) {
			throw new IllegalArgumentException("Message G9 vide");
		}
		if (!StringUtils.hasText(message.getReferenceIncident())) {
			throw new IllegalArgumentException("G9: referenceIncident obligatoire");
		}
	}
}
