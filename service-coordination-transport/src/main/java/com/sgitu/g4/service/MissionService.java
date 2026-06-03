package com.sgitu.g4.service;

import com.sgitu.g4.dto.MissionRequest;
import com.sgitu.g4.dto.MissionResponse;
import com.sgitu.g4.dto.MissionStatusResponse;
import com.sgitu.g4.entity.AffectationVehiculeLigne;
import com.sgitu.g4.entity.Ligne;
import com.sgitu.g4.entity.Mission;
import com.sgitu.g4.entity.StatutMission;
import com.sgitu.g4.entity.Trajet;
import com.sgitu.g4.exception.BadRequestException;
import com.sgitu.g4.exception.ConflictException;
import com.sgitu.g4.exception.ForbiddenOperationException;
import com.sgitu.g4.exception.ResourceNotFoundException;
import com.sgitu.g4.integration.G3UserClient;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissionService {

	private final MissionRepository missionRepository;
	private final LigneRepository ligneRepository;
	private final TrajetRepository trajetRepository;
	private final AffectationRepository affectationRepository;
	private final G1MissionLifecyclePublisher g1MissionLifecyclePublisher;
	private final G7VehicleClient g7VehicleClient;
	private final G3UserClient g3UserClient;
	private final VehiculeReferentielService vehiculeReferentielService;
	private final G5ContractNotificationService g5ContractNotificationService;
	private final SupervisionLogService supervisionLogService;

	@Transactional
	public MissionResponse create(MissionRequest request) {
		String vehiculeId = VehiculeReferentielService.normalizeVehiculeId(request.getVehiculeId());
		assertVehicleAvailableForActiveMission(vehiculeId, null, request.getStatut());
		vehiculeReferentielService.assertReadyForMission(vehiculeId, request.getLigneId());
		g3UserClient.assertDriverExistsIfEnabled(request.getChauffeurId());
		Ligne ligne = ligneRepository.findById(request.getLigneId())
				.orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable : " + request.getLigneId()));
		Trajet trajet = resolveTrajet(request.getTrajetId(), ligne);
		AffectationVehiculeLigne affectation = resolveAffectation(request.getAffectationId(), ligne);
		Mission mission = Mission.builder()
				.vehiculeId(vehiculeId)
				.chauffeurId(trimToNull(request.getChauffeurId()))
				.ligne(ligne)
				.trajet(trajet)
				.affectation(affectation)
				.statut(request.getStatut())
				.plannedStart(request.getPlannedStart())
				.actualStart(request.getActualStart())
				.endedAt(request.getEndedAt())
				.notes(request.getNotes())
				.build();
		Mission saved = missionRepository.save(mission);
		supervisionLogService.add("INFO", "MISSION", "Créée id=" + saved.getId());
		g1MissionLifecyclePublisher.publish(saved, "MISSION_PLANIFIED", "DEBUT_MISSION");
		if (saved.getStatut() == StatutMission.EN_COURS) {
			g1MissionLifecyclePublisher.publish(saved, "ON_GOING", "TRAJET_EN_COURS");
		}
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
		Map<String, Object> vehiculeStatut = g7VehicleClient.fetchStatus(mission.getVehiculeId());
		MissionStatusResponse response = EntityMapper.toStatus(mission);
		response.setVehiculeStatutG7(vehiculeStatut);
		return response;
	}

	@Transactional
	public MissionResponse update(Long id, MissionRequest request) {
		Mission mission = missionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + id));
		if (mission.getStatut() == StatutMission.CLOTUREE || mission.getStatut() == StatutMission.ANNULEE) {
			throw new ForbiddenOperationException("Mission terminée ou annulée");
		}
		StatutMission oldStatus = mission.getStatut();
		String vehiculeId = VehiculeReferentielService.normalizeVehiculeId(request.getVehiculeId());
		assertVehicleAvailableForActiveMission(vehiculeId, id, request.getStatut());
		vehiculeReferentielService.assertReadyForMission(vehiculeId, request.getLigneId());
		g3UserClient.assertDriverExistsIfEnabled(request.getChauffeurId());
		Ligne ligne = ligneRepository.findById(request.getLigneId())
				.orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable : " + request.getLigneId()));
		mission.setVehiculeId(vehiculeId);
		mission.setChauffeurId(trimToNull(request.getChauffeurId()));
		mission.setLigne(ligne);
		mission.setTrajet(resolveTrajet(request.getTrajetId(), ligne));
		mission.setAffectation(resolveAffectation(request.getAffectationId(), ligne));
		mission.setStatut(request.getStatut());
		mission.setPlannedStart(request.getPlannedStart());
		mission.setActualStart(request.getActualStart());
		mission.setEndedAt(request.getEndedAt());
		mission.setNotes(request.getNotes());
		Mission saved = missionRepository.save(mission);
		if (oldStatus != StatutMission.EN_COURS && saved.getStatut() == StatutMission.EN_COURS) {
			g1MissionLifecyclePublisher.publish(saved, "ON_GOING", "TRAJET_EN_COURS");
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
		g1MissionLifecyclePublisher.publish(saved, "MISSION_CLOSED", "FIN_MISSION");
		vehiculeReferentielService.tryReleaseVehicleIfIdle(saved.getVehiculeId());
		g5ContractNotificationService.postMissionFinDeService(saved);
		return EntityMapper.toDto(saved);
	}

	@Transactional
	public MissionResponse annuler(Long id) {
		return annuler(id, true);
	}

	@Transactional
	public MissionResponse annuler(Long id, boolean notifierG1) {
		Mission mission = missionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + id));
		if (mission.getStatut() == StatutMission.CLOTUREE) {
			throw new BadRequestException("Mission déjà clôturée");
		}
		mission.setStatut(StatutMission.ANNULEE);
		mission.setEndedAt(Instant.now());
		Mission saved = missionRepository.save(mission);
		supervisionLogService.add("WARN", "MISSION", "Annulée id=" + id);
		if (notifierG1) {
			g1MissionLifecyclePublisher.publish(saved, "MISSION_CANCELLED", "ANNULATION");
		}
		vehiculeReferentielService.tryReleaseVehicleIfIdle(saved.getVehiculeId());
		g5ContractNotificationService.postMissionCancelledArretNonDesservi(saved, null);
		return EntityMapper.toDto(saved);
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

	private void assertVehicleAvailableForActiveMission(String vehiculeId, Long excludeMissionId, StatutMission targetStatut) {
		if (targetStatut != StatutMission.EN_COURS) {
			return;
		}
		boolean conflict = excludeMissionId == null
				? missionRepository.existsByVehiculeIdAndStatut(vehiculeId, StatutMission.EN_COURS)
				: missionRepository.existsByVehiculeIdAndStatutAndIdNot(vehiculeId, StatutMission.EN_COURS, excludeMissionId);
		if (conflict) {
			throw new ConflictException(
					"Le véhicule " + vehiculeId + " est déjà affecté à une mission EN_COURS");
		}
	}

	private static String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
