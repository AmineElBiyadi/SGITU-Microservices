package com.sgitu.g4.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgitu.g4.config.CoordinationKafkaPublisher;
import com.sgitu.g4.dto.CancelMissionEventRequest;
import com.sgitu.g4.dto.CoordinationEventRequest;
import com.sgitu.g4.dto.CoordinationEventResponse;
import com.sgitu.g4.dto.DetectBreakdownRequest;
import com.sgitu.g4.dto.DetectDelayRequest;
import com.sgitu.g4.dto.DetectDeviationRequest;
import com.sgitu.g4.entity.CoordinationEventEntity;
import com.sgitu.g4.entity.CoordinationEventStatus;
import com.sgitu.g4.entity.CoordinationEventType;
import com.sgitu.g4.entity.Mission;
import com.sgitu.g4.entity.StatutMission;
import com.sgitu.g4.exception.BadRequestException;
import com.sgitu.g4.exception.ResourceNotFoundException;
import com.sgitu.g4.integration.G7VehicleClient;
import com.sgitu.g4.mapper.EntityMapper;
import com.sgitu.g4.repository.CoordinationEventRepository;
import com.sgitu.g4.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoordinationEventService {

	private final CoordinationEventRepository eventRepository;
	private final MissionRepository missionRepository;
	private final MissionService missionService;
	private final CoordinationKafkaPublisher kafkaPublisher;
	private final ObjectMapper objectMapper;
	private final G7VehicleClient g7VehicleClient;
	private final G1MissionLifecyclePublisher g1MissionLifecyclePublisher;
	private final G5ContractNotificationService g5ContractNotificationService;
	private final SupervisionLogService supervisionLogService;

	@Transactional
	public CoordinationEventResponse create(CoordinationEventRequest request) {
		CoordinationEventEntity saved = eventRepository.save(mapRequestToEntity(request));
		return finalize(EntityMapper.toDto(saved));
	}

	@Transactional(readOnly = true)
	public List<CoordinationEventResponse> findAll() {
		return eventRepository.findAllByOrderByOccurredAtDesc().stream().map(EntityMapper::toDto).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public CoordinationEventResponse findById(Long id) {
		return eventRepository.findById(id).map(EntityMapper::toDto)
				.orElseThrow(() -> new ResourceNotFoundException("Événement introuvable : " + id));
	}

	@Transactional(readOnly = true)
	public List<CoordinationEventResponse> byType(CoordinationEventType type) {
		return eventRepository.findByTypeOrderByOccurredAtDesc(type).stream().map(EntityMapper::toDto).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<CoordinationEventResponse> byStatus(CoordinationEventStatus status) {
		return eventRepository.findByStatusOrderByOccurredAtDesc(status).stream().map(EntityMapper::toDto).collect(Collectors.toList());
	}

	@Transactional
	public CoordinationEventResponse detectDelay(DetectDelayRequest request) {
		Mission mission = missionRepository.findById(request.getMissionId())
				.orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + request.getMissionId()));
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("retardMinutes", request.getRetardMinutes());
		payload.put("cause", request.getCause());
		payload.put("vehiculeStatut", g7VehicleClient.fetchStatus(mission.getVehiculeId()));
		CoordinationEventResponse response = persistDetection(CoordinationEventType.RETARD, mission, request.getCause(), payload);
		String arret = StringUtils.hasText(request.getArret()) ? request.getArret() : request.getCause();
		g1MissionLifecyclePublisher.publishDelayAlert(mission, request.getRetardMinutes(), arret);
		g5ContractNotificationService.postDelayAlert(mission, request.getRetardMinutes(), arret);
		return response;
	}

	@Transactional
	public CoordinationEventResponse detectDeviation(DetectDeviationRequest request) {
		Mission mission = missionRepository.findById(request.getMissionId())
				.orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + request.getMissionId()));
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("details", request.getDetails());
		payload.put("ecartMetres", request.getEcartMetres());
		payload.put("vehiculeStatut", g7VehicleClient.fetchStatus(mission.getVehiculeId()));
		CoordinationEventResponse response = persistDetection(CoordinationEventType.DEVIATION, mission, request.getDetails(), payload);
		g1MissionLifecyclePublisher.publishRouteDeviation(mission, request.getDetails());
		g5ContractNotificationService.postRouteDeviation(mission, request.getDetails());
		return response;
	}

	@Transactional
	public CoordinationEventResponse detectBreakdown(DetectBreakdownRequest request) {
		Mission mission = request.getMissionId() == null ? null : missionRepository.findById(request.getMissionId())
				.orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + request.getMissionId()));
		Map<String, Object> payload = Map.of(
				"symptomes", request.getSymptomes(),
				"vehiculeStatut", g7VehicleClient.fetchStatus(request.getVehiculeId())
		);
		CoordinationEventEntity entity = CoordinationEventEntity.builder()
				.type(CoordinationEventType.PANNE)
				.status(CoordinationEventStatus.SIGNALE)
				.mission(mission)
				.vehiculeId(request.getVehiculeId())
				.description(request.getSymptomes())
				.payloadJson(writeJson(new LinkedHashMap<>(payload)))
				.occurredAt(Instant.now())
				.build();
		CoordinationEventResponse response = finalize(EntityMapper.toDto(eventRepository.save(entity)));
		g5ContractNotificationService.postVehicleBreakdown(request.getVehiculeId(), mission);
		return response;
	}

	@Transactional
	public CoordinationEventResponse cancelMissionEvent(CancelMissionEventRequest request) {
		Mission mission = missionRepository.findById(request.getMissionId())
				.orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + request.getMissionId()));
		if (mission.getStatut() == StatutMission.CLOTUREE) {
			throw new BadRequestException("Mission déjà clôturée");
		}
		missionService.annuler(mission.getId(), request.isNotifierG1());
		Mission refreshed = missionRepository.findById(request.getMissionId()).orElseThrow();
		Map<String, Object> p = new LinkedHashMap<>();
		p.put("motif", request.getMotif());
		p.put("notifierG1", request.isNotifierG1());
		CoordinationEventEntity entity = CoordinationEventEntity.builder()
				.type(CoordinationEventType.ANNULATION_MISSION)
				.status(CoordinationEventStatus.TRAITE)
				.mission(refreshed)
				.vehiculeId(refreshed.getVehiculeId())
				.description(request.getMotif())
				.payloadJson(writeJson(p))
				.occurredAt(Instant.now())
				.build();
		return finalize(EntityMapper.toDto(eventRepository.save(entity)));
	}

	private CoordinationEventResponse persistDetection(CoordinationEventType type, Mission mission, String description,
			Map<String, Object> payload) {
		CoordinationEventEntity entity = CoordinationEventEntity.builder()
				.type(type)
				.status(CoordinationEventStatus.SIGNALE)
				.mission(mission)
				.vehiculeId(mission.getVehiculeId())
				.description(description)
				.payloadJson(writeJson(payload))
				.occurredAt(Instant.now())
				.build();
		return finalize(EntityMapper.toDto(eventRepository.save(entity)));
	}

	private CoordinationEventResponse finalize(CoordinationEventResponse dto) {
		supervisionLogService.add("INFO", "EVENT", dto.getType() + " id=" + dto.getId());
		kafkaPublisher.publish(dto);
		return dto;
	}

	private CoordinationEventEntity mapRequestToEntity(CoordinationEventRequest request) {
		Mission mission = null;
		if (request.getMissionId() != null) {
			mission = missionRepository.findById(request.getMissionId())
					.orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + request.getMissionId()));
		}
		CoordinationEventStatus status = request.getStatus() != null ? request.getStatus() : CoordinationEventStatus.SIGNALE;
		return CoordinationEventEntity.builder()
				.type(request.getType())
				.status(status)
				.mission(mission)
				.vehiculeId(request.getVehiculeId())
				.description(request.getDescription())
				.payloadJson(request.getPayloadJson())
				.occurredAt(request.getOccurredAt() != null ? request.getOccurredAt() : Instant.now())
				.build();
	}

	private String writeJson(Map<String, Object> payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("Payload JSON : " + e.getOriginalMessage());
		}
	}
}
