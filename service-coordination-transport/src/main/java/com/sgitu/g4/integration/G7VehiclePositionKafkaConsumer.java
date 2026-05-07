package com.sgitu.g4.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgitu.g4.dto.CoordinationEventRequest;
import com.sgitu.g4.dto.G7VehiclePositionMessage;
import com.sgitu.g4.entity.CoordinationEventStatus;
import com.sgitu.g4.entity.CoordinationEventType;
import com.sgitu.g4.entity.Mission;
import com.sgitu.g4.entity.StatutMission;
import com.sgitu.g4.repository.MissionRepository;
import com.sgitu.g4.service.CoordinationEventService;
import com.sgitu.g4.service.SupervisionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sgitu.kafka", name = "enabled", havingValue = "true")
public class G7VehiclePositionKafkaConsumer {

	private final ObjectMapper objectMapper;
	private final MissionRepository missionRepository;
	private final CoordinationEventService coordinationEventService;
	private final SupervisionLogService supervisionLogService;

	@KafkaListener(
			topics = "${sgitu.kafka.topic-vehicule-positions:vehicule-positions}",
			groupId = "${sgitu.kafka.g7-consumer-group-id:g4-coordination-g7}"
	)
	public void onVehiclePosition(String rawMessage) {
		try {
			G7VehiclePositionMessage msg = objectMapper.readValue(rawMessage, G7VehiclePositionMessage.class);
			supervisionLogService.add("INFO", "KAFKA-G7", "Position reçue vehicule=" + msg.getVehiculeId());
			Optional<Mission> missionOpt = missionRepository.findByVehiculeIdOrderByCreatedAtDesc(msg.getVehiculeId())
					.stream()
					.filter(m -> m.getStatut() == StatutMission.EN_COURS)
					.findFirst();
			if (missionOpt.isEmpty()) {
				return;
			}
			Mission mission = missionOpt.get();
			if (msg.getLigneId() != null && !msg.getLigneId().equalsIgnoreCase(mission.getLigne().getCode())
					&& !msg.getLigneId().equalsIgnoreCase(mission.getLigne().getNom())) {
				CoordinationEventRequest eventRequest = new CoordinationEventRequest();
				eventRequest.setType(CoordinationEventType.DEVIATION);
				eventRequest.setStatus(CoordinationEventStatus.SIGNALE);
				eventRequest.setMissionId(mission.getId());
				eventRequest.setVehiculeId(msg.getVehiculeId());
				eventRequest.setDescription("Déviation détectée via G7");
				eventRequest.setOccurredAt(msg.getTimestamp() != null ? msg.getTimestamp() : Instant.now());
				eventRequest.setPayloadJson(objectMapper.writeValueAsString(buildPayload(msg)));
				coordinationEventService.create(eventRequest);
			}
		} catch (Exception ex) {
			log.warn("Message position G7 invalide: {}", ex.getMessage());
			supervisionLogService.add("WARN", "KAFKA-G7", "Payload rejeté: " + ex.getMessage());
		}
	}

	private Map<String, Object> buildPayload(G7VehiclePositionMessage msg) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("vehiculeId", msg.getVehiculeId());
		payload.put("ligneId", msg.getLigneId());
		payload.put("lat", msg.getLat());
		payload.put("long", msg.getLongitude());
		payload.put("vitesse", msg.getVitesse());
		payload.put("timestamp", msg.getTimestamp());
		return payload;
	}
}
