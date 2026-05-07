package com.sgitu.g4.service;

import com.sgitu.g4.dto.ArretRequest;
import com.sgitu.g4.dto.ArretResponse;
import com.sgitu.g4.entity.Arret;
import com.sgitu.g4.entity.Ligne;
import com.sgitu.g4.exception.BadRequestException;
import com.sgitu.g4.exception.ResourceNotFoundException;
import com.sgitu.g4.mapper.EntityMapper;
import com.sgitu.g4.repository.ArretRepository;
import com.sgitu.g4.repository.LigneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArretService {

	private final ArretRepository arretRepository;
	private final LigneRepository ligneRepository;

	@Transactional
	public ArretResponse create(ArretRequest request) {
		Ligne ligne = ligneRepository.findById(request.getLigneId())
				.orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable : " + request.getLigneId()));
		Arret entity = Arret.builder()
				.code(request.getCode().trim())
				.nom(request.getNom().trim())
				.latitude(request.getLatitude())
				.longitude(request.getLongitude())
				.ligne(ligne)
				.build();
		return EntityMapper.toDto(arretRepository.save(entity));
	}

	@Transactional(readOnly = true)
	public List<ArretResponse> findAll() {
		return arretRepository.findAll().stream().map(EntityMapper::toDto).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public ArretResponse findById(Long id) {
		return arretRepository.findById(id).map(EntityMapper::toDto)
				.orElseThrow(() -> new ResourceNotFoundException("Arrêt introuvable : " + id));
	}

	@Transactional
	public ArretResponse update(Long id, ArretRequest request) {
		Arret entity = arretRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Arrêt introuvable : " + id));
		Ligne ligne = ligneRepository.findById(request.getLigneId())
				.orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable : " + request.getLigneId()));
		entity.setCode(request.getCode().trim());
		entity.setNom(request.getNom().trim());
		entity.setLatitude(request.getLatitude());
		entity.setLongitude(request.getLongitude());
		entity.setLigne(ligne);
		return EntityMapper.toDto(arretRepository.save(entity));
	}

	@Transactional
	public void delete(Long id) {
		if (!arretRepository.existsById(id)) {
			throw new ResourceNotFoundException("Arrêt introuvable : " + id);
		}
		try {
			arretRepository.deleteById(id);
		} catch (Exception ex) {
			throw new BadRequestException("Impossible de supprimer cet arrêt (référencé ?)");
		}
	}

	@Transactional(readOnly = true)
	public List<ArretResponse> findByLigne(Long ligneId) {
		if (!ligneRepository.existsById(ligneId)) {
			throw new ResourceNotFoundException("Ligne introuvable : " + ligneId);
		}
		return arretRepository.findByLigneIdOrderByNomAsc(ligneId).stream().map(EntityMapper::toDto).collect(Collectors.toList());
	}
}
