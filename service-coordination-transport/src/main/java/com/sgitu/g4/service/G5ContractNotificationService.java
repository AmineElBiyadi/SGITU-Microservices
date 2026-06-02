package com.sgitu.g4.service;

import com.sgitu.g4.config.G5NotificationProperties;
import com.sgitu.g4.dto.NotificationSendRequest;
import com.sgitu.g4.entity.Ligne;
import com.sgitu.g4.entity.Mission;
import com.sgitu.g4.notification.G5NotificationEventType;
import com.sgitu.g4.notification.G5NotificationReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Déclenche le contrat G4↔G5 : même {@code POST /api/notifications/send} que Postman,
 * appelé en interne après un événement métier ; {@code recipient} rempli via G3 si broadcast activé.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class G5ContractNotificationService {

	private final G5NotificationProperties properties;
	private final G5RecipientBroadcastService g5RecipientBroadcastService;
	private final SupervisionLogService supervisionLogService;

	public void postDelayAlert(Mission mission, int retardMinutes, String arret) {
		if (!enabled()) {
			return;
		}
		Map<String, String> variables = baseVehicleVariables(mission);
		variables.put("valeur", String.valueOf(retardMinutes));
		variables.put("arret", defaultText(arret, "arrêt non précisé"));
		post(mission, G5NotificationEventType.DELAY_ALERT, G5NotificationReason.RETARD_SIGNIFICATIF, variables);
	}

	public void postRouteDeviation(Mission mission, String lieu) {
		if (!enabled()) {
			return;
		}
		Map<String, String> variables = baseVehicleVariables(mission);
		variables.put("lieu", defaultText(lieu, "secteur non précisé"));
		post(mission, G5NotificationEventType.ROUTE_DEVIATION, G5NotificationReason.DEVIATION_TRAJET, variables);
	}

	public void postVehicleBreakdown(String vehiculeId, Mission mission) {
		if (!enabled()) {
			return;
		}
		Map<String, String> variables = new LinkedHashMap<>();
		variables.put("vehiculeId", vehiculeId);
		String lineId = mission != null && mission.getLigne() != null ? lineCode(mission.getLigne()) : "N/A";
		postRaw(lineId, G5NotificationEventType.VEHICLE_BREAKDOWN, G5NotificationReason.PANNE_TECHNIQUE, variables);
	}

	public void postIncidentConfirmed(String lineId, String lieu) {
		if (!enabled()) {
			return;
		}
		Map<String, String> variables = new LinkedHashMap<>();
		variables.put("ligneId", defaultText(lineId, "N/A"));
		variables.put("lieu", defaultText(lieu, "zone non précisée"));
		postRaw(variables.get("ligneId"), G5NotificationEventType.INCIDENT_CONFIRMED,
				G5NotificationReason.INCIDENT_VOIRIE, variables);
	}

	public void postMissionCancelledArretNonDesservi(Mission mission, String arret) {
		if (!enabled()) {
			return;
		}
		Map<String, String> variables = baseVehicleVariables(mission);
		variables.put("arret", defaultText(arret, "arrêt non précisé"));
		post(mission, G5NotificationEventType.MISSION_CANCELLED, G5NotificationReason.ARRET_NON_DESSERVI, variables);
	}

	public void postMissionFinDeService(Mission mission) {
		if (!enabled()) {
			return;
		}
		Map<String, String> variables = baseVehicleVariables(mission);
		variables.put("ligneId", lineCode(mission.getLigne()));
		post(mission, G5NotificationEventType.MISSION_CANCELLED, G5NotificationReason.FIN_DE_SERVICE, variables);
	}

	private void post(Mission mission, G5NotificationEventType eventType, G5NotificationReason reason,
			Map<String, String> variables) {
		postRaw(lineCode(mission.getLigne()), eventType, reason, variables);
	}

	private void postRaw(String lineId, G5NotificationEventType eventType, G5NotificationReason reason,
			Map<String, String> variables) {
		NotificationSendRequest request = buildRequest(lineId, eventType, reason, variables);
		g5RecipientBroadcastService.dispatch(request);
		supervisionLogService.add("INFO", "G5-POST",
				"POST /api/notifications/send " + eventType.name() + " id=" + request.getNotificationId());
		log.info("Contrat G5 POST {} {} lineId={}", eventType, reason, lineId);
	}

	private NotificationSendRequest buildRequest(String lineId, G5NotificationEventType eventType,
			G5NotificationReason reason, Map<String, String> variables) {
		NotificationSendRequest request = new NotificationSendRequest();
		request.setNotificationId(UUID.randomUUID().toString());
		request.setSourceService(properties.getSourceService());
		request.setEventType(eventType.name());
		request.setChannel(properties.getChannel());
		NotificationSendRequest.Metadata metadata = new NotificationSendRequest.Metadata();
		metadata.setLineId(lineId);
		metadata.setReason(reason.name());
		metadata.setVariables(variables);
		request.setMetadata(metadata);
		return request;
	}

	private static Map<String, String> baseVehicleVariables(Mission mission) {
		Map<String, String> variables = new LinkedHashMap<>();
		variables.put("vehiculeId", mission.getVehiculeId());
		return variables;
	}

	private static String lineCode(Ligne ligne) {
		return ligne != null && StringUtils.hasText(ligne.getCode()) ? ligne.getCode() : String.valueOf(ligne.getId());
	}

	private static String defaultText(String value, String fallback) {
		return StringUtils.hasText(value) ? value.trim() : fallback;
	}

	private boolean enabled() {
		return properties.isPostOnEventEnabled();
	}
}
