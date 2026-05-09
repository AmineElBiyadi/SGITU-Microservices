package com.sgitu.g4.service;

import com.sgitu.g4.dto.MissionRequest;
import com.sgitu.g4.dto.MissionResponse;
import com.sgitu.g4.dto.MissionStatusResponse;
import com.sgitu.g4.dto.G3MissionLifecycleMessage;
import com.sgitu.g4.entity.AffectationVehiculeLigne;
import com.sgitu.g4.entity.Ligne;
import com.sgitu.g4.entity.Mission;
import com.sgitu.g4.entity.StatutMission;
import com.sgitu.g4.entity.Trajet;
import com.sgitu.g4.exception.BadRequestException;
import com.sgitu.g4.exception.ForbiddenOperationException;
import com.sgitu.g4.exception.ResourceNotFoundException;
import com.sgitu.g4.integration.G3BilletterieClient;
import com.sgitu.g4.integration.G7VehicleClient;
import com.sgitu.g4.mapper.EntityMapper;
import com.sgitu.g4.repository.AffectationRepository;
import com.sgitu.g4.repository.LigneRepository;
import com.sgitu.g4.repository.MissionRepository;
import com.sgitu.g4.repository.TrajetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissionService {

	private final MissionRepository missionRepository;
	private final LigneRepository ligneRepository;
	private final TrajetRepository trajetRepository;
	private final AffectationRepository affectationRepository;
	private final G3BilletterieClient g3BilletterieClient;
	private final G7VehicleClient g7VehicleClient;
	private final SupervisionLogService supervisionLogService;

	@Transactional
	public MissionResponse create(MissionRequest request) {
		Ligne ligne = ligneRepository.findById(request.getLigneId())
				.orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable : " + request.getLigneId()));
		Trajet trajet = resolveTrajet(request.getTrajetId(), ligne);
		AffectationVehiculeLigne affectation = resolveAffectation(request.getAffectationId(), ligne);
		Mission mission = Mission.builder()
				.vehiculeId(request.getVehiculeId().trim())
				.chauffeurId(trimToNull(request.getChauffeurId()))
				.ligne(ligne)
				.trajet(trajet)
				.affectation(affectation)
				.statut(request.getStatut())
				.plannedStart(request.getPlannedStart())
				.actualStart(request.getActualStart())
				.endedAt(request.getEndedAt())
				.referenceG3(request.getReferenceG3())
				.notes(request.getNotes())
				.build();
		Mission saved = missionRepository.save(mission);
		supervisionLogService.add("INFO", "MISSION", "Créée id=" + saved.getId());
		publishG3Lifecycle(saved, "MISSION_PLANIFIED", "DEBUT_MISSION");
		return EntityMapper.toDto(saved);
	}

	@Transactional(readOnly = true)
	public List<MissionResponse> findAll() {
		return missionRepository.findAll().stream().map(EntityMapper::toDto).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<MissionResponse> findActives() {
		return missionRepository.findByStatut(StatutMission.EN_COURS).stream().map(EntityMapper::toDto).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public MissionResponse findById(Long id) {
		return missionRepository.findById(id).map(EntityMapper::toDto)
				.orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + id));
	}

	@Transactional(readOnly = true)
	public MissionStatusResponse status(Long id) {
		Mission mission = missionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + id));
		g7VehicleClient.fetchStatus(mission.getVehiculeId());
		return EntityMapper.toStatus(mission);
	}

	@Transactional
	public MissionResponse update(Long id, MissionRequest request) {
		Mission mission = missionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + id));
		if (mission.getStatut() == StatutMission.CLOTUREE || mission.getStatut() == StatutMission.ANNULEE) {
			throw new ForbiddenOperationException("Mission terminée ou annulée");
		}
		StatutMission oldStatus = mission.getStatut();
		Ligne ligne = ligneRepository.findById(request.getLigneId())
				.orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable : " + request.getLigneId()));
		mission.setVehiculeId(request.getVehiculeId().trim());
		mission.setChauffeurId(trimToNull(request.getChauffeurId()));
		mission.setLigne(ligne);
		mission.setTrajet(resolveTrajet(request.getTrajetId(), ligne));
		mission.setAffectation(resolveAffectation(request.getAffectationId(), ligne));
		mission.setStatut(request.getStatut());
		mission.setPlannedStart(request.getPlannedStart());
		mission.setActualStart(request.getActualStart());
		mission.setEndedAt(request.getEndedAt());
		mission.setReferenceG3(request.getReferenceG3());
		mission.setNotes(request.getNotes());
		Mission saved = missionRepository.save(mission);
		if (oldStatus != StatutMission.EN_COURS && saved.getStatut() == StatutMission.EN_COURS) {
			publishG3Lifecycle(saved, "ON_GOING", "TRAJET_EN_COURS");
		}
		return EntityMapper.toDto(saved);
	}

	@Transactional
	public MissionResponse cloturer(Long id) {
		Mission mission = missionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + id));
		if (mission.getStatut() != StatutMission.EN_COURS) {
			throw new BadRequestException("Clôture possible uniquement pour EN_COURS");
		}
		mission.setStatut(StatutMission.CLOTUREE);
		mission.setEndedAt(Instant.now());
		Mission saved = missionRepository.save(mission);
		supervisionLogService.add("INFO", "MISSION", "Clôturée id=" + id);
		publishG3Lifecycle(saved, "MISSION_CLOSED", "FIN_MISSION");
		return EntityMapper.toDto(saved);
	}

	@Transactional
	public MissionResponse annuler(Long id) {
		return annuler(id, true);
	}

	@Transactional
	public MissionResponse annuler(Long id, boolean notifierG3) {
		Mission mission = missionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + id));
		if (mission.getStatut() == StatutMission.CLOTUREE) {
			throw new BadRequestException("Mission déjà clôturée");
		}
		mission.setStatut(StatutMission.ANNULEE);
		mission.setEndedAt(Instant.now());
		Mission saved = missionRepository.save(mission);
		supervisionLogService.add("WARN", "MISSION", "Annulée id=" + id);
		if (notifierG3) {
			publishG3Lifecycle(saved, "MISSION_CANCELLED", "ANNULATION");
		}
		return EntityMapper.toDto(saved);
	}

	private void publishG3Lifecycle(Mission mission, String eventType, String reason) {
		G3MissionLifecycleMessage msg = G3MissionLifecycleMessage.builder()
				.notificationId(UUID.randomUUID().toString())
				.eventType(eventType)
				.metadata(G3MissionLifecycleMessage.Metadata.builder()
						.reason(reason)
						.missionDetails(G3MissionLifecycleMessage.MissionDetails.builder()
								.missionId("M-" + mission.getId())
								.status(mapG3Status(mission.getStatut()))
								.horaire(Map.of(
										"depart", formatInstant(mission.getPlannedStart()),
										"arrivee", formatInstant(mission.getEndedAt())))
								.trajet(Map.of(
										"lieuDepart", mission.getLigne().getNom(),
										"lieuArrivee", mission.getTrajet() != null ? mission.getTrajet().getNom() : mission.getLigne().getNom()))
								.build())
						.variables(Map.of("vehiculeId", mission.getVehiculeId()))
						.build())
				.build();
		g3BilletterieClient.publishMissionLifecycleEvent(String.valueOf(mission.getId()), msg);
	}

	private String mapG3Status(StatutMission statut) {
		return switch (statut) {
			case PLANIFIEE -> "PLANIFIED";
			case EN_COURS -> "ON_GOING";
			case CLOTUREE -> "CLOSED";
			case ANNULEE -> "CANCELLED";
		};
	}

	private String formatInstant(Instant instant) {
		if (instant == null) {
			return "";
		}
		return DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC).format(instant);
	}

	private Trajet resolveTrajet(Long trajetId, Ligne ligne) {
		if (trajetId == null) {
			return null;
		}
		Trajet trajet = trajetRepository.findById(trajetId)
				.orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable : " + trajetId));
		if (!trajet.getLigne().getId().equals(ligne.getId())) {
			throw new BadRequestException("Trajet hors ligne");
		}
		return trajet;
	}

	private AffectationVehiculeLigne resolveAffectation(Long affectationId, Ligne ligne) {
		if (affectationId == null) {
			return null;
		}
		AffectationVehiculeLigne aff = affectationRepository.findById(affectationId)
				.orElseThrow(() -> new ResourceNotFoundException("Affectation introuvable : " + affectationId));
		if (!aff.getLigne().getId().equals(ligne.getId())) {
			throw new BadRequestException("Affectation hors ligne");
		}
		return aff;
	}

	private static String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
