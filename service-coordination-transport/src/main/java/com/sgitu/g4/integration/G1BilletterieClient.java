package com.sgitu.g4.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgitu.g4.config.KafkaAppProperties;
import com.sgitu.g4.dto.G1MissionLifecycleMessage;
import com.sgitu.g4.service.SupervisionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class G1BilletterieClient {

	private final KafkaAppProperties kafkaAppProperties;
	private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;
	private final ObjectMapper objectMapper;
	private final SupervisionLogService supervisionLogService;

	public void publishMissionLifecycleEvent(String key, G1MissionLifecycleMessage event) {
		if (!kafkaAppProperties.isEnabled()) {
			supervisionLogService.add("WARN", "G1-KAFKA", "Kafka désactivé: message lifecycle non publié");
			return;
		}
		KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
		if (kafkaTemplate == null) {
			supervisionLogService.add("WARN", "G1-KAFKA", "KafkaTemplate absent");
			return;
		}
		try {
			String payload = objectMapper.writeValueAsString(event);
			kafkaTemplate.send(kafkaAppProperties.getTopicMissionLifecycle(), key, payload);
		} catch (Exception ex) {
			log.warn("Publication lifecycle G1 (billetterie) échouée: {}", ex.getMessage());
			supervisionLogService.add("WARN", "G1-KAFKA", ex.getMessage());
		}
	}
}
