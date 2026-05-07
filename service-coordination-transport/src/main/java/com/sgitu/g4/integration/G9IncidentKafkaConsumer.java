package com.sgitu.g4.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgitu.g4.config.KafkaAppProperties;
import com.sgitu.g4.dto.CoordinationEventRequest;
import com.sgitu.g4.dto.G9IncidentKafkaMessage;
import com.sgitu.g4.entity.CoordinationEventStatus;
import com.sgitu.g4.entity.CoordinationEventType;
import com.sgitu.g4.service.CoordinationEventService;
import com.sgitu.g4.service.SupervisionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sgitu.kafka", name = "enabled", havingValue = "true")
public class G9IncidentKafkaConsumer {

	private final ObjectMapper objectMapper;
	private final CoordinationEventService coordinationEventService;
	private final SupervisionLogService supervisionLogService;
	private final KafkaAppProperties kafkaAppProperties;

	@KafkaListener(
			topics = "${sgitu.kafka.topic-incident-inbound:incident.transport.topic}",
			groupId = "${sgitu.kafka.g9-consumer-group-id:g4-coordination-g9}"
	)
	public void onIncidentMessage(String rawMessage) {
		try {
			G9IncidentKafkaMessage message = objectMapper.readValue(rawMessage, G9IncidentKafkaMessage.class);
			CoordinationEventRequest eventRequest = mapToCoordinationEvent(message, rawMessage);
			coordinationEventService.create(eventRequest);
			supervisionLogService.add("INFO", "KAFKA-G9",
					"Incident reçu ref=" + message.getReferenceIncident() + " topic=" + kafkaAppProperties.getTopicIncidentInbound());
		} catch (Exception e) {
			log.warn("Message incident G9 invalide: {}", e.getMessage());
			supervisionLogService.add("WARN", "KAFKA-G9", "Payload rejeté: " + e.getMessage());
		}
	}

	private CoordinationEventRequest mapToCoordinationEvent(G9IncidentKafkaMessage message, String rawMessage) {
		CoordinationEventRequest request = new CoordinationEventRequest();
		request.setType(mapType(message.getType()));
		request.setStatus(mapStatus(message.getStatut()));
		request.setMissionId(null);
		request.setVehiculeId(message.getVehiculeId());
		request.setDescription(message.getDescription());
		request.setOccurredAt(message.getTimestamp() != null
				? message.getTimestamp().toInstant(ZoneOffset.UTC)
				: Instant.now());
		request.setPayloadJson(buildPayloadJson(message, rawMessage));
		return request;
	}

	private String buildPayloadJson(G9IncidentKafkaMessage message, String rawMessage) {
		try {
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("referenceIncident", message.getReferenceIncident());
			payload.put("g9Type", message.getType());
			payload.put("g9Statut", message.getStatut());
			payload.put("vehiculeId", message.getVehiculeId());
			payload.put("ligneId", message.getLigneId());
			payload.put("description", message.getDescription());
			payload.put("latitude", message.getLatitude());
			payload.put("longitude", message.getLongitude());
			payload.put("timestamp", message.getTimestamp());
			payload.put("rawMessage", rawMessage);
			return objectMapper.writeValueAsString(payload);
		} catch (Exception e) {
			return rawMessage;
		}
	}

	private CoordinationEventType mapType(String g9Type) {
		if (g9Type == null) {
			return CoordinationEventType.INCIDENT;
		}
		return switch (g9Type.trim().toUpperCase()) {
			case "PANNE_VEHICULE" -> CoordinationEventType.PANNE;
			case "RETARD", "ENCOMBREMENT" -> CoordinationEventType.RETARD;
			default -> CoordinationEventType.INCIDENT;
		};
	}

	private CoordinationEventStatus mapStatus(String g9Status) {
		if (g9Status == null) {
			return CoordinationEventStatus.SIGNALE;
		}
		return switch (g9Status.trim().toUpperCase()) {
			case "CONFIRME" -> CoordinationEventStatus.CONFIRME;
			case "RESOLU" -> CoordinationEventStatus.TRAITE;
			case "REJETE" -> CoordinationEventStatus.ANNULE;
			default -> CoordinationEventStatus.SIGNALE;
		};
	}
}
