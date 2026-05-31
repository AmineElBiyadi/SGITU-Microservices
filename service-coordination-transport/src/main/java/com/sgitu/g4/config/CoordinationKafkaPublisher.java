package com.sgitu.g4.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgitu.g4.dto.CoordinationEventResponse;
import com.sgitu.g4.service.SupervisionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoordinationKafkaPublisher {

	private final KafkaAppProperties kafkaAppProperties;
	private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;
	private final ObjectMapper objectMapper;
	private final SupervisionLogService supervisionLogService;

	public void publish(CoordinationEventResponse event) {
		if (!kafkaAppProperties.isEnabled()) {
			return;
		}
		KafkaTemplate<String, String> kt = kafkaTemplateProvider.getIfAvailable();
		if (kt == null) {
			supervisionLogService.add("WARN", "KAFKA", "KafkaTemplate absent");
			return;
		}
		try {
			String json = objectMapper.writeValueAsString(event);
			kt.send(kafkaAppProperties.getTopicCoordination(), String.valueOf(event.getId()), json);
			supervisionLogService.add("INFO", "KAFKA", "Pub événement id=" + event.getId());
		} catch (Exception e) {
			log.warn("Kafka: {}", e.getMessage());
			supervisionLogService.add("WARN", "KAFKA", e.getMessage());
		}
	}
}
