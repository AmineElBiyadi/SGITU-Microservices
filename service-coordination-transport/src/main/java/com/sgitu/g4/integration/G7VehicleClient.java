package com.sgitu.g4.integration;

import com.sgitu.g4.config.IntegrationProperties;
import com.sgitu.g4.entity.StatutVehiculeG7;
import com.sgitu.g4.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class G7VehicleClient {

	private final IntegrationProperties integrationProperties;

	public Map<String, Object> fetchVehiculeOrThrow(String vehiculeId) {
		return fetchVehicule(vehiculeId)
				.orElseThrow(() -> new BadRequestException("Véhicule introuvable chez G7 : " + vehiculeId));
	}

	@SuppressWarnings("unchecked")
	public Optional<Map<String, Object>> fetchVehicule(String vehiculeId) {
		try {
			Map<String, Object> body = RestClient.create(integrationProperties.getG7BaseUrl())
					.get()
					.uri(integrationProperties.getG7VehiculesPath() + "/{id}", vehiculeId)
					.retrieve()
					.body(Map.class);
			return Optional.ofNullable(body);
		} catch (HttpClientErrorException.NotFound ex) {
			return Optional.empty();
		} catch (Exception ex) {
			log.warn("G7 GET véhicule {} indisponible: {}", vehiculeId, ex.getMessage());
			if (integrationProperties.isG7FlowStrict()) {
				throw new BadRequestException("G7 injoignable : " + ex.getMessage());
			}
			return Optional.empty();
		}
	}

	public boolean exists(String vehiculeId) {
		return fetchVehicule(vehiculeId).isPresent();
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> fetchStatus(String vehiculeId) {
		Optional<Map<String, Object>> remote = fetchVehicule(vehiculeId);
		if (remote.isPresent()) {
			Map<String, Object> map = new LinkedHashMap<>(remote.get());
			map.put("source", "G7");
			if (!map.containsKey("vehiculeId")) {
				map.put("vehiculeId", vehiculeId);
			}
			return map;
		}
		log.debug("G7 mock statut pour {}", vehiculeId);
		return Map.of(
				"vehiculeId", vehiculeId,
				"statut", StatutVehiculeG7.INCONNU.name(),
				"source", "MOCK");
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> updateStatut(String vehiculeId, String statutG7) {
		try {
			Map<String, Object> body = RestClient.create(integrationProperties.getG7BaseUrl())
					.put()
					.uri(uriBuilder -> uriBuilder
							.path(integrationProperties.getG7VehiculesPath() + "/{id}/statut")
							.queryParam("statut", statutG7)
							.build(vehiculeId))
					.retrieve()
					.body(Map.class);
			log.info("G7 statut mis à jour vehiculeId={} statut={}", vehiculeId, statutG7);
			return body != null ? body : Map.of("vehiculeId", vehiculeId, "statut", statutG7);
		} catch (Exception ex) {
			log.warn("G7 PUT statut échoué pour {}: {}", vehiculeId, ex.getMessage());
			if (integrationProperties.isG7FlowStrict()) {
				throw new BadRequestException(
						"Impossible de notifier G7 (statut " + statutG7 + ") : " + ex.getMessage());
			}
			return Map.of("vehiculeId", vehiculeId, "statut", statutG7, "source", "MOCK", "warning", ex.getMessage());
		}
	}

	public void notifyEnService(String vehiculeId) {
		updateStatut(vehiculeId, "EN_SERVICE");
	}

	public void notifyDisponible(String vehiculeId) {
		updateStatut(vehiculeId, "DISPONIBLE");
	}
}
