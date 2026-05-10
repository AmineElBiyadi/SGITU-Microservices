package com.sgitu.g4.integration;

import com.sgitu.g4.config.IntegrationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class G9IncidentClient {

	private final IntegrationProperties integrationProperties;

	public void acknowledgeCorrelation(String incidentReference, Long coordinationEventId) {
		var body = Map.of("incidentReference", incidentReference, "coordinationEventId", coordinationEventId);
		try {
			RestClient.create(integrationProperties.getG9BaseUrl())
					.post()
					.uri("/api/internal/incidents/correlation")
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.toBodilessEntity();
		} catch (Exception ex) {
			log.debug("G9 ack: {}", ex.getMessage());
		}
	}
}
