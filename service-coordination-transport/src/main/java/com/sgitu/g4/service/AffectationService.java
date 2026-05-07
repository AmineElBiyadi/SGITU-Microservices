package com.sgitu.g4.service;

import com.sgitu.g4.dto.AffectationRequest;
import com.sgitu.g4.dto.AffectationResponse;
import com.sgitu.g4.entity.AffectationVehiculeLigne;
import com.sgitu.g4.entity.Ligne;
import com.sgitu.g4.exception.BadRequestException;
import com.sgitu.g4.exception.ResourceNotFoundException;
import com.sgitu.g4.mapper.EntityMapper;
import com.sgitu.g4.repository.AffectationRepository;
import com.sgitu.g4.repository.LigneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AffectationService {

	private final AffectationRepository affectationRepository;
	private final LigneRepository ligneRepository;

	@Transactional
	public AffectationResponse create(AffectationRequest request) {
		Ligne ligne = ligneRepository.findById(request.getLigneId())
				.orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable : " + request.getLigneId()));
		validateDates(request);
		AffectationVehiculeLigne entity = AffectationVehiculeLigne.builder()
				.vehiculeId(request.getVehiculeId().trim())
				.ligne(ligne)
				.dateDebut(request.getDateDebut())
				.dateFin(request.getDateFin())
				.statut(request.getStatut())
				.commentaire(request.getCommentaire())
				.build();
		return EntityMapper.toDto(affectationRepository.save(entity));
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
		return affectationRepository.findByVehiculeIdOrderByDateDebutDesc(vehiculeId).stream()
				.map(EntityMapper::toDto)
				.collect(Collectors.toList());
	}

	@Transactional
	public AffectationResponse update(Long id, AffectationRequest request) {
		AffectationVehiculeLigne entity = affectationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Affectation introuvable : " + id));
		Ligne ligne = ligneRepository.findById(request.getLigneId())
				.orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable : " + request.getLigneId()));
		validateDates(request);
		entity.setVehiculeId(request.getVehiculeId().trim());
		entity.setLigne(ligne);
		entity.setDateDebut(request.getDateDebut());
		entity.setDateFin(request.getDateFin());
		entity.setStatut(request.getStatut());
		entity.setCommentaire(request.getCommentaire());
		return EntityMapper.toDto(affectationRepository.save(entity));
	}

	@Transactional
	public void delete(Long id) {
		if (!affectationRepository.existsById(id)) {
			throw new ResourceNotFoundException("Affectation introuvable : " + id);
		}
		affectationRepository.deleteById(id);
	}

	private static void validateDates(AffectationRequest request) {
		if (request.getDateFin() != null && request.getDateFin().isBefore(request.getDateDebut())) {
			throw new BadRequestException("dateFin invalide");
		}
	}
}
