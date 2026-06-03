package com.sgitu.g4.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgitu.g4.dto.G7VehicleRegisteredMessage;
import com.sgitu.g4.service.SupervisionLogService;
import com.sgitu.g4.service.VehiculeReferentielService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sgitu.kafka", name = "enabled", havingValue = "true")
public class G7VehicleRegisteredKafkaConsumer {

	private final ObjectMapper objectMapper;
	private final VehiculeReferentielService vehiculeReferentielService;
	private final SupervisionLogService supervisionLogService;

	@KafkaListener(
			topics = "${sgitu.kafka.topic-vehicle-registered:vehicle.registered}",
			groupId = "${sgitu.kafka.g7-registered-consumer-group-id:g4-coordination-g7-registered}"
	)
	public void onVehicleRegistered(String rawMessage) {
		try {
			G7VehicleRegisteredMessage message = objectMapper.readValue(rawMessage, G7VehicleRegisteredMessage.class);
			KafkaContractValidator.validateG7VehicleRegistered(message);
			vehiculeReferentielService.registerFromKafka(message);
		} catch (Exception ex) {
			log.warn("Message vehicle.registered invalide: {}", ex.getMessage());
			supervisionLogService.add("WARN", "KAFKA-G7-REGISTER", "Payload rejeté: " + ex.getMessage());
		}
	}
}
