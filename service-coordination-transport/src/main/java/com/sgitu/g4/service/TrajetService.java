package com.sgitu.g4.service;

import com.sgitu.g4.dto.TrajetArretSequenceItem;
import com.sgitu.g4.dto.TrajetRequest;
import com.sgitu.g4.dto.TrajetResponse;
import com.sgitu.g4.entity.Arret;
import com.sgitu.g4.entity.Ligne;
import com.sgitu.g4.entity.Trajet;
import com.sgitu.g4.entity.TrajetArret;
import com.sgitu.g4.exception.BadRequestException;
import com.sgitu.g4.exception.ResourceNotFoundException;
import com.sgitu.g4.mapper.EntityMapper;
import com.sgitu.g4.repository.ArretRepository;
import com.sgitu.g4.repository.LigneRepository;
import com.sgitu.g4.repository.TrajetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrajetService {

	private final TrajetRepository trajetRepository;
	private final LigneRepository ligneRepository;
	private final ArretRepository arretRepository;

	@Transactional
	public TrajetResponse create(TrajetRequest request) {
		Ligne ligne = ligneRepository.findById(request.getLigneId())
				.orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable : " + request.getLigneId()));
		Trajet trajet = Trajet.builder()
				.ligne(ligne)
				.code(request.getCode().trim())
				.nom(request.getNom().trim())
				.sens(request.getSens())
				.actif(request.getActif())
				.build();
		Trajet saved = trajetRepository.save(trajet);
		replaceSequence(saved, request.getArretSequence());
		saved = trajetRepository.save(saved);
		return EntityMapper.toDto(trajetRepository.findFetchedById(saved.getId()).orElse(saved));
	}

	@Transactional(readOnly = true)
	public List<TrajetResponse> findAll() {
		return trajetRepository.findAll().stream().map(EntityMapper::toDto).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public TrajetResponse findById(Long id) {
		Trajet t = trajetRepository.findFetchedById(id)
				.or(() -> trajetRepository.findById(id))
				.orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable : " + id));
		return EntityMapper.toDto(t);
	}

	@Transactional(readOnly = true)
	public List<com.sgitu.g4.dto.ArretResponse> arretsOrdered(Long trajetId) {
		if (!trajetRepository.existsById(trajetId)) {
			throw new ResourceNotFoundException("Trajet introuvable : " + trajetId);
		}
		Trajet t = trajetRepository.findFetchedById(trajetId).orElseGet(() ->
				trajetRepository.findById(trajetId).orElseThrow());
		return EntityMapper.arretsSortedFromTrajet(t);
	}

	@Transactional
	public TrajetResponse update(Long id, TrajetRequest request) {
		Trajet trajet = trajetRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable : " + id));
		Ligne ligne = ligneRepository.findById(request.getLigneId())
				.orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable : " + request.getLigneId()));
		trajet.setLigne(ligne);
		trajet.setCode(request.getCode().trim());
		trajet.setNom(request.getNom().trim());
		trajet.setSens(request.getSens());
		trajet.setActif(request.getActif());
		replaceSequence(trajet, request.getArretSequence());
		Trajet saved = trajetRepository.save(trajet);
		return EntityMapper.toDto(trajetRepository.findFetchedById(saved.getId()).orElse(saved));
	}

	@Transactional
	public void delete(Long id) {
		if (!trajetRepository.existsById(id)) {
			throw new ResourceNotFoundException("Trajet introuvable : " + id);
		}
		trajetRepository.deleteById(id);
	}

	private void replaceSequence(Trajet trajet, List<TrajetArretSequenceItem> items) {
		trajet.getArretsSequence().clear();
		if (items == null || items.isEmpty()) {
			return;
		}
		List<TrajetArretSequenceItem> sorted = items.stream()
				.sorted(Comparator.comparing(TrajetArretSequenceItem::getSequenceOrder))
				.toList();
		Set<Integer> orders = new HashSet<>();
		for (TrajetArretSequenceItem item : sorted) {
			if (!orders.add(item.getSequenceOrder())) {
				throw new BadRequestException("Ordre dupliqué : " + item.getSequenceOrder());
			}
			Arret arret = arretRepository.findById(item.getArretId())
					.orElseThrow(() -> new ResourceNotFoundException("Arrêt introuvable : " + item.getArretId()));
			if (!Objects.equals(arret.getLigne().getId(), trajet.getLigne().getId())) {
				throw new BadRequestException("Arrêt hors ligne du trajet");
			}
			trajet.getArretsSequence().add(TrajetArret.builder()
					.trajet(trajet)
					.arret(arret)
					.sequenceOrder(item.getSequenceOrder())
					.build());
		}
	}
}
