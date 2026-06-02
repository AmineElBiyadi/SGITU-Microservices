package com.sgitu.g4.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgitu.g4.dto.G7VehiclePositionMessage;
import com.sgitu.g4.service.SupervisionLogService;
import com.sgitu.g4.service.detection.VehiclePositionCoordinationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sgitu.kafka", name = "enabled", havingValue = "true")
public class G7VehiclePositionKafkaConsumer {

	private final ObjectMapper objectMapper;
	private final SupervisionLogService supervisionLogService;
	private final VehiclePositionCoordinationProcessor vehiclePositionCoordinationProcessor;

	@KafkaListener(
			topics = "${sgitu.kafka.topic-vehicule-positions:vehicule-positions}",
			groupId = "${sgitu.kafka.g7-consumer-group-id:g4-coordination-g7}"
	)
	public void onVehiclePosition(String rawMessage) {
		try {
			G7VehiclePositionMessage msg = objectMapper.readValue(rawMessage, G7VehiclePositionMessage.class);
			KafkaContractValidator.validateG7Position(msg);
			supervisionLogService.add("INFO", "KAFKA-G7", "Position reçue vehicule=" + msg.getVehiculeId());
			vehiclePositionCoordinationProcessor.process(msg);
		} catch (Exception ex) {
			log.warn("Message position G7 invalide: {}", ex.getMessage());
			supervisionLogService.add("WARN", "KAFKA-G7", "Payload rejeté: " + ex.getMessage());
		}
	}
}
