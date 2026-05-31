package com.sgitu.g4.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgitu.g4.dto.DetectIncidentRequest;
import com.sgitu.g4.dto.G9IncidentKafkaMessage;
import com.sgitu.g4.dto.IncidentImpactResponse;
import com.sgitu.g4.entity.Mission;
import com.sgitu.g4.entity.MissionIncidentImpact;
import com.sgitu.g4.entity.StatutMission;
import com.sgitu.g4.exception.BadRequestException;
import com.sgitu.g4.exception.ResourceNotFoundException;
import com.sgitu.g4.integration.G7VehicleClient;
import com.sgitu.g4.integration.G9IncidentClient;
import com.sgitu.g4.mapper.EntityMapper;
import com.sgitu.g4.repository.MissionIncidentImpactRepository;
import com.sgitu.g4.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncidentImpactService {

	private final MissionIncidentImpactRepository impactRepository;
	private final MissionRepository missionRepository;
	private final ObjectMapper objectMapper;
	private final G7VehicleClient g7VehicleClient;
	private final G9IncidentClient g9IncidentClient;
	private final SupervisionLogService supervisionLogService;

	@Transactional
	public IncidentImpactResponse recordFromRequest(DetectIncidentRequest request) {
		Mission mission = resolveMission(request.getMissionId(), request.getVehiculeId());
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("incidentReference", request.getIncidentReference());
		payload.put("resume", request.getResume());
		payload.put("vehiculeStatut", g7VehicleClient.fetchStatus(request.getVehiculeId()));
		MissionIncidentImpact saved = persist(
				mission,
				request.getIncidentReference(),
				request.getVehiculeId(),
				null,
				null,
				request.getResume(),
				writeJson(payload),
				Instant.now());
		IncidentImpactResponse dto = EntityMapper.toDto(saved);
		g9IncidentClient.acknowledgeCorrelation(request.getIncidentReference(), dto.getId());
		return dto;
	}

	@Transactional
	public IncidentImpactResponse recordFromG9Kafka(G9IncidentKafkaMessage message, String rawMessage) {
		if (!StringUtils.hasText(message.getReferenceIncident())) {
			throw new BadRequestException("referenceIncident G9 obligatoire");
		}
		Mission mission = resolveMission(null, message.getVehiculeId());
		Instant occurredAt = message.getTimestamp() != null
				? message.getTimestamp().toInstant(ZoneOffset.UTC)
				: Instant.now();
		MissionIncidentImpact saved = persist(
				mission,
				message.getReferenceIncident().trim(),
				message.getVehiculeId(),
				message.getType(),
				message.getStatut(),
				message.getDescription(),
				buildPayloadJson(message, rawMessage),
				occurredAt);
		return EntityMapper.toDto(saved);
	}

	@Transactional(readOnly = true)
	public List<IncidentImpactResponse> findAll() {
		return impactRepository.findAllByOrderByOccurredAtDesc().stream()
				.map(EntityMapper::toDto)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public IncidentImpactResponse findById(Long id) {
		return impactRepository.findById(id)
				.map(EntityMapper::toDto)
				.orElseThrow(() -> new ResourceNotFoundException("Impact incident introuvable : " + id));
	}

	@Transactional(readOnly = true)
	public List<IncidentImpactResponse> findByMissionId(Long missionId) {
		if (!missionRepository.existsById(missionId)) {
			throw new ResourceNotFoundException("Mission introuvable : " + missionId);
		}
		return impactRepository.findByMissionIdOrderByOccurredAtDesc(missionId).stream()
				.map(EntityMapper::toDto)
				.collect(Collectors.toList());
	}

	private MissionIncidentImpact persist(
			Mission mission,
			String g9Reference,
			String vehiculeId,
			String g9Type,
			String g9Statut,
			String description,
			String payloadJson,
			Instant occurredAt) {
		MissionIncidentImpact entity = MissionIncidentImpact.builder()
				.mission(mission)
				.g9ReferenceIncident(g9Reference)
				.vehiculeId(vehiculeId)
				.g9Type(g9Type)
				.g9Statut(g9Statut)
				.description(description)
				.payloadJson(payloadJson)
				.occurredAt(occurredAt)
				.build();
		MissionIncidentImpact saved = impactRepository.save(entity);
		supervisionLogService.add("INFO", "INCIDENT-G9",
				"Impact mission=" + (mission != null ? mission.getId() : "n/a")
						+ " ref=" + g9Reference);
		return saved;
	}

	private Mission resolveMission(Long missionId, String vehiculeId) {
		if (missionId != null) {
			return missionRepository.findById(missionId)
					.orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + missionId));
		}
		if (!StringUtils.hasText(vehiculeId)) {
			return null;
		}
		return missionRepository.findByVehiculeIdOrderByCreatedAtDesc(vehiculeId.trim())
				.stream()
				.filter(m -> m.getStatut() == StatutMission.EN_COURS)
				.findFirst()
				.orElse(null);
	}

	private String buildPayloadJson(G9IncidentKafkaMessage message, String rawMessage) {
		try {
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("referenceIncident", message.getReferenceIncident());
			payload.put("g9Type", message.getType());
			payload.put("g9Statut", message.getStatut());
			payload.put("vehiculeId", message.getVehiculeId());
			payload.put("ligneId", message.getLigneId());
			payload.put("description", message.getDescription());
			payload.put("latitude", message.getLatitude());
			payload.put("longitude", message.getLongitude());
			payload.put("timestamp", message.getTimestamp());
			payload.put("rawMessage", rawMessage);
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			return rawMessage;
		}
	}

	private String writeJson(Map<String, Object> payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("Payload JSON : " + e.getOriginalMessage());
		}
	}
}
