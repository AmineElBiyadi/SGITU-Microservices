package com.sgitu.g4.service;

import com.sgitu.g4.config.IntegrationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IntegrationHealthProbe {

	private final IntegrationProperties integrationProperties;

	public Map<String, String> probeAll() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("G1_BILLETTERIE", ping(integrationProperties.getG1BaseUrl()));
		map.put("G5_NOTIFICATIONS", ping(integrationProperties.getG5BaseUrl()));
		map.put("G7_FLOTTE", ping(integrationProperties.getG7BaseUrl()));
		map.put("G9_INCIDENTS", ping(integrationProperties.getG9BaseUrl()));
		map.put("G10_GATEWAY", ping(integrationProperties.getG10GatewayUrl()));
		return map;
	}

	private String ping(String baseUrl) {
		try {
			RestClient.create(baseUrl).get().uri("/actuator/health").retrieve().toBodilessEntity();
			return "UP";
		} catch (Exception ex) {
			return "DOWN";
		}
	}
}
