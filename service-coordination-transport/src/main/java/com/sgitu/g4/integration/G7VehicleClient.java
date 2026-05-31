package com.sgitu.g4.integration;

import com.sgitu.g4.config.IntegrationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class G7VehicleClient {

	private final IntegrationProperties integrationProperties;

	@SuppressWarnings("unchecked")
	public Map<String, Object> fetchStatus(String vehiculeId) {
		try {
			return RestClient.create(integrationProperties.getG7BaseUrl())
					.get()
					.uri(uriBuilder -> uriBuilder.path("/api/internal/vehicles/{id}/status").build(vehiculeId))
					.retrieve()
					.body(Map.class);
		} catch (Exception ex) {
			log.debug("G7 mock pour {}: {}", vehiculeId, ex.getMessage());
			return Map.of("vehiculeId", vehiculeId, "statut", "INCONNU", "source", "MOCK");
		}
	}
}
