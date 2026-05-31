package com.sgitu.g4.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgitu.g4.config.KafkaAppProperties;
import com.sgitu.g4.dto.G9IncidentKafkaMessage;
import com.sgitu.g4.service.IncidentImpactService;
import com.sgitu.g4.service.SupervisionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sgitu.kafka", name = "enabled", havingValue = "true")
public class G9IncidentKafkaConsumer {

	private final ObjectMapper objectMapper;
	private final IncidentImpactService incidentImpactService;
	private final SupervisionLogService supervisionLogService;
	private final KafkaAppProperties kafkaAppProperties;

	@KafkaListener(
			topics = "${sgitu.kafka.topic-incident-inbound:incident.transport.topic}",
			groupId = "${sgitu.kafka.g9-consumer-group-id:g4-coordination-g9}"
	)
	public void onIncidentMessage(String rawMessage) {
		try {
			G9IncidentKafkaMessage message = objectMapper.readValue(rawMessage, G9IncidentKafkaMessage.class);
			KafkaContractValidator.validateG9Incident(message);
			incidentImpactService.recordFromG9Kafka(message, rawMessage);
			supervisionLogService.add("INFO", "KAFKA-G9",
					"Impact incident ref=" + message.getReferenceIncident()
							+ " topic=" + kafkaAppProperties.getTopicIncidentInbound());
		} catch (Exception e) {
			log.warn("Message incident G9 invalide: {}", e.getMessage());
			supervisionLogService.add("WARN", "KAFKA-G9", "Payload rejeté: " + e.getMessage());
		}
	}
}
