package com.sgitu.g4.service;

import com.sgitu.g4.dto.AffectationRequest;
import com.sgitu.g4.dto.AffectationResponse;
import com.sgitu.g4.entity.AffectationVehiculeLigne;
import com.sgitu.g4.entity.Ligne;
import com.sgitu.g4.entity.StatutAffectation;
import com.sgitu.g4.exception.BadRequestException;
import com.sgitu.g4.exception.ResourceNotFoundException;
import com.sgitu.g4.integration.G7VehicleClient;
import com.sgitu.g4.mapper.EntityMapper;
import com.sgitu.g4.repository.AffectationRepository;
import com.sgitu.g4.repository.LigneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AffectationService {

	private final AffectationRepository affectationRepository;
	private final LigneRepository ligneRepository;
	private final VehiculeReferentielService vehiculeReferentielService;
	private final G7VehicleClient g7VehicleClient;
	private final SupervisionLogService supervisionLogService;

	@Transactional
	public AffectationResponse create(AffectationRequest request) {
		String vehiculeId = VehiculeReferentielService.normalizeVehiculeId(request.getVehiculeId());
		vehiculeReferentielService.assertKnownAndDisponibleForAffectation(vehiculeId);
		Ligne ligne = ligneRepository.findById(request.getLigneId())
				.orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable : " + request.getLigneId()));
		validateDates(request);
		AffectationVehiculeLigne entity = AffectationVehiculeLigne.builder()
				.vehiculeId(vehiculeId)
				.chauffeurId(trimToNull(request.getChauffeurId()))
				.ligne(ligne)
				.dateDebut(request.getDateDebut())
				.dateFin(request.getDateFin())
				.statut(request.getStatut())
				.commentaire(request.getCommentaire())
				.build();
		AffectationVehiculeLigne saved = affectationRepository.save(entity);
		if (saved.getStatut() == StatutAffectation.ACTIF) {
			g7VehicleClient.notifyEnService(vehiculeId);
			vehiculeReferentielService.markEnServiceAfterAffectation(vehiculeId, ligne.getId());
			supervisionLogService.add("INFO", "AFFECTATION",
					"Véhicule " + vehiculeId + " → ligne " + ligne.getId() + ", G7 EN_SERVICE");
		}
		return EntityMapper.toDto(saved);
	}

	@Transactional(readOnly = true)
	public List<AffectationResponse> findAll() {
		return affectationRepository.findAll().stream().map(EntityMapper::toDto).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public AffectationResponse findById(Long id) {
		return affectationRepository.findById(id).map(EntityMapper::toDto)
				.orElseThrow(() -> new ResourceNotFoundException("Affectation introuvable : " + id));
	}

	@Transactional(readOnly = true)
	public List<AffectationResponse> byVehicule(String vehiculeId) {
		String normalized = VehiculeReferentielService.normalizeVehiculeId(vehiculeId);
		return affectationRepository.findByVehiculeIdOrderByDateDebutDesc(normalized).stream()
				.map(EntityMapper::toDto)
				.collect(Collectors.toList());
	}

	@Transactional
	public AffectationResponse update(Long id, AffectationRequest request) {
		AffectationVehiculeLigne entity = affectationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Affectation introuvable : " + id));
		StatutAffectation previousStatut = entity.getStatut();
		String previousVehiculeId = entity.getVehiculeId();
		String vehiculeId = VehiculeReferentielService.normalizeVehiculeId(request.getVehiculeId());
		Ligne ligne = ligneRepository.findById(request.getLigneId())
				.orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable : " + request.getLigneId()));
		validateDates(request);
		entity.setVehiculeId(vehiculeId);
		entity.setLigne(ligne);
		entity.setDateDebut(request.getDateDebut());
		entity.setDateFin(request.getDateFin());
		entity.setStatut(request.getStatut());
		entity.setCommentaire(request.getCommentaire());
		AffectationVehiculeLigne saved = affectationRepository.save(entity);
		if (saved.getStatut() == StatutAffectation.ACTIF && previousStatut != StatutAffectation.ACTIF) {
			g7VehicleClient.notifyEnService(vehiculeId);
			vehiculeReferentielService.markEnServiceAfterAffectation(vehiculeId, ligne.getId());
			supervisionLogService.add("INFO", "AFFECTATION",
					"Véhicule " + vehiculeId + " → ligne " + ligne.getId() + ", G7 EN_SERVICE (mise à jour)");
		}
		if (saved.getStatut() == StatutAffectation.TERMINE) {
			vehiculeReferentielService.tryReleaseVehicleIfIdle(vehiculeId);
		}
		if (previousStatut == StatutAffectation.ACTIF && !previousVehiculeId.equals(vehiculeId)) {
			vehiculeReferentielService.tryReleaseVehicleIfIdle(previousVehiculeId);
		}
		return EntityMapper.toDto(saved);
	}

	@Transactional
	public void delete(Long id) {
		AffectationVehiculeLigne entity = affectationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Affectation introuvable : " + id));
		String vehiculeId = entity.getVehiculeId();
		affectationRepository.deleteById(id);
		vehiculeReferentielService.tryReleaseVehicleIfIdle(vehiculeId);
	}

	private static void validateDates(AffectationRequest request) {
		if (request.getDateFin() != null && request.getDateFin().isBefore(request.getDateDebut())) {
			throw new BadRequestException("dateFin invalide");
		}
	}

	private static String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
