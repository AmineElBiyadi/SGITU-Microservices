package com.sgitu.g4.service.detection;

import com.sgitu.g4.config.DetectionProperties;
import com.sgitu.g4.dto.DetectBreakdownRequest;
import com.sgitu.g4.dto.DetectDelayRequest;
import com.sgitu.g4.dto.DetectDeviationRequest;
import com.sgitu.g4.dto.G7VehiclePositionMessage;
import com.sgitu.g4.integration.G7VehicleClient;
import com.sgitu.g4.util.CoordinationDetectionUtils;
import com.sgitu.g4.entity.CoordinationEventType;
import com.sgitu.g4.entity.Mission;
import com.sgitu.g4.entity.StatutMission;
import com.sgitu.g4.repository.MissionRepository;
import com.sgitu.g4.service.CoordinationEventService;
import com.sgitu.g4.service.SupervisionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehiclePositionCoordinationProcessor {

	private final DetectionProperties detectionProperties;
	private final MissionRepository missionRepository;
	private final RouteDeviationDetector routeDeviationDetector;
	private final ScheduleDelayDetector scheduleDelayDetector;
	private final DetectionCooldownService detectionCooldownService;
	private final CoordinationEventService coordinationEventService;
	private final G7VehicleClient g7VehicleClient;
	private final SupervisionLogService supervisionLogService;

	@Transactional
	public void process(G7VehiclePositionMessage msg) {
		if (!detectionProperties.isEnabled()) {
			return;
		}
		Optional<Mission> missionOpt = missionRepository.findByVehiculeIdOrderByCreatedAtDesc(msg.getVehiculeId())
				.stream()
				.filter(m -> m.getStatut() == StatutMission.EN_COURS)
				.findFirst();
		if (missionOpt.isEmpty()) {
			return;
		}
		Mission mission = missionOpt.get();
		Instant occurredAt = msg.getTimestamp() != null ? msg.getTimestamp() : Instant.now();

		routeDeviationDetector.detect(mission, msg.getLat(), msg.getLongitude()).ifPresent(deviation -> {
			if (!detectionCooldownService.canEmit(mission.getId(), CoordinationEventType.DEVIATION)) {
				return;
			}
			DetectDeviationRequest request = new DetectDeviationRequest();
			request.setMissionId(mission.getId());
			request.setEcartMetres((int) Math.round(deviation.distanceMeters()));
			request.setDetails(deviation.details());
			coordinationEventService.detectDeviation(request);
			supervisionLogService.add("INFO", "DETECT-DEVIATION",
					"Auto mission=" + mission.getId() + " ecart=" + request.getEcartMetres() + "m");
			log.info("Déviation auto missionId={} distanceM={}", mission.getId(), deviation.distanceMeters());
		});

		scheduleDelayDetector.detect(mission, msg.getLat(), msg.getLongitude(), occurredAt).ifPresent(delay -> {
			if (!detectionCooldownService.canEmit(mission.getId(), CoordinationEventType.RETARD)) {
				return;
			}
			DetectDelayRequest request = new DetectDelayRequest();
			request.setMissionId(mission.getId());
			request.setRetardMinutes(delay.retardMinutes());
			request.setCause(delay.cause());
			request.setArret(delay.arretNom());
			coordinationEventService.detectDelay(request);
			supervisionLogService.add("INFO", "DETECT-RETARD",
					"Auto mission=" + mission.getId() + " retard=" + delay.retardMinutes() + "min");
			log.info("Retard auto missionId={} minutes={}", mission.getId(), delay.retardMinutes());
		});

		checkG7BreakdownStatus(mission, msg.getVehiculeId());
	}

	private void checkG7BreakdownStatus(Mission mission, String vehiculeId) {
		if (!detectionCooldownService.canEmit(mission.getId(), CoordinationEventType.PANNE)) {
			return;
		}
		Object statutObj = g7VehicleClient.fetchStatus(vehiculeId).get("statut");
		String statut = statutObj != null ? String.valueOf(statutObj) : "";
		if (!CoordinationDetectionUtils.isG7BreakdownStatut(statut)) {
			return;
		}
		DetectBreakdownRequest request = new DetectBreakdownRequest();
		request.setMissionId(mission.getId());
		request.setVehiculeId(vehiculeId);
		request.setSymptomes("Statut G7 indiquant une panne : " + statut);
		coordinationEventService.detectBreakdown(request);
		supervisionLogService.add("INFO", "DETECT-PANNE",
				"Auto mission=" + mission.getId() + " statutG7=" + statut);
		log.info("Panne auto missionId={} statutG7={}", mission.getId(), statut);
	}
}
