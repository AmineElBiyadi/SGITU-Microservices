package com.sgitu.g4.service;

import com.sgitu.g4.dto.G1MissionLifecycleMessage;
import com.sgitu.g4.entity.Mission;
import com.sgitu.g4.entity.StatutMission;
import com.sgitu.g4.integration.G1BilletterieClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Contrat G4 ↔ G1 : publication Kafka {@code missions-lifecycle}.
 */
@Service
@RequiredArgsConstructor
public class G1MissionLifecyclePublisher {

	private final G1BilletterieClient g1BilletterieClient;
	private final SupervisionLogService supervisionLogService;

	public void publish(Mission mission, String eventType, String reason) {
		publish(mission, eventType, reason, Map.of("vehiculeId", mission.getVehiculeId()));
	}

	public void publish(Mission mission, String eventType, String reason, Map<String, Object> variables) {
		G1MissionLifecycleMessage msg = G1MissionLifecycleMessage.builder()
				.notificationId(UUID.randomUUID().toString())
				.eventType(eventType)
				.metadata(G1MissionLifecycleMessage.Metadata.builder()
						.reason(reason)
						.missionDetails(G1MissionLifecycleMessage.MissionDetails.builder()
								.missionId(formatMissionId(mission.getId()))
								.status(mapBilletterieLifecycleStatus(mission.getStatut()))
								.horaire(Map.of(
										"depart", formatInstant(mission.getPlannedStart()),
										"arrivee", formatInstant(mission.getEndedAt())))
								.trajet(Map.of(
										"lieuDepart", mission.getLigne().getNom(),
										"lieuArrivee", mission.getTrajet() != null
												? mission.getTrajet().getNom()
												: mission.getLigne().getNom()))
								.build())
						.variables(new LinkedHashMap<>(variables))
						.build())
				.build();
		g1BilletterieClient.publishMissionLifecycleEvent(formatMissionId(mission.getId()), msg);
		supervisionLogService.add("INFO", "G1-KAFKA", eventType + " " + formatMissionId(mission.getId()));
	}

	public void publishDelayAlert(Mission mission, int retardMinutes, String arret) {
		Map<String, Object> variables = new LinkedHashMap<>();
		variables.put("vehiculeId", mission.getVehiculeId());
		variables.put("retardMinutes", retardMinutes);
		if (StringUtils.hasText(arret)) {
			variables.put("arret", arret.trim());
		}
		publish(mission, "DELAY_ALERT", "RETARD_SIGNIFICATIF", variables);
	}

	public void publishRouteDeviation(Mission mission, String lieu) {
		Map<String, Object> variables = new LinkedHashMap<>();
		variables.put("vehiculeId", mission.getVehiculeId());
		if (StringUtils.hasText(lieu)) {
			variables.put("lieu", lieu.trim());
		}
		publish(mission, "ROUTE_DEVIATION", "DEVIATION_TRAJET", variables);
	}

	public static String formatMissionId(Long missionId) {
		return "M-" + missionId;
	}

	private static String mapBilletterieLifecycleStatus(StatutMission statut) {
		return switch (statut) {
			case PLANIFIEE -> "PLANIFIED";
			case EN_COURS -> "ON_GOING";
			case CLOTUREE -> "CLOSED";
			case ANNULEE -> "CANCELLED";
		};
	}

	private static String formatInstant(Instant instant) {
		if (instant == null) {
			return "";
		}
		return DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC).format(instant);
	}
}
