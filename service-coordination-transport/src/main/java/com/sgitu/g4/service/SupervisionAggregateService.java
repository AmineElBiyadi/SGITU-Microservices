package com.sgitu.g4.service;

import com.sgitu.g4.dto.G4HealthResponse;
import com.sgitu.g4.dto.OperatorStatusResponse;
import com.sgitu.g4.entity.StatutMission;
import com.sgitu.g4.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupervisionAggregateService {

	private final DataSource dataSource;
	private final MissionRepository missionRepository;
	private final IntegrationHealthProbe integrationHealthProbe;

	@Value("${info.app.version:0.0.1-SNAPSHOT}")
	private String buildVersion;

	public G4HealthResponse health() {
		Map<String, String> components = new LinkedHashMap<>();
		String db = "DOWN";
		try (Connection c = dataSource.getConnection()) {
			if (c.isValid(2)) {
				db = "UP";
			}
		} catch (Exception ex) {
			db = "DOWN: " + ex.getMessage();
		}
		components.put("database", db);
		String status = db.startsWith("UP") ? "UP" : "DEGRADED";
		return G4HealthResponse.builder()
				.status(status)
				.checkedAt(Instant.now())
				.components(components)
				.buildVersion(buildVersion)
				.build();
	}

	public OperatorStatusResponse operatorStatus() {
		long actives = missionRepository.findByStatut(StatutMission.EN_COURS).size();
		Map<String, String> integrations = integrationHealthProbe.probeAll();
		String mode = integrations.values().stream().allMatch(v -> v.startsWith("UP")) ? "NOMINAL" : "DEGRADE";
		return OperatorStatusResponse.builder()
				.mode(mode)
				.missionsActives(actives)
				.integrations(integrations)
				.generatedAt(Instant.now())
				.build();
	}
}
