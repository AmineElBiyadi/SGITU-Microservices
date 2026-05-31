package com.sgitu.g4.service;

import com.sgitu.g4.dto.LigneRequest;
import com.sgitu.g4.dto.LigneResponse;
import com.sgitu.g4.dto.TrajetResponse;
import com.sgitu.g4.entity.Ligne;
import com.sgitu.g4.exception.BadRequestException;
import com.sgitu.g4.exception.ResourceNotFoundException;
import com.sgitu.g4.mapper.EntityMapper;
import com.sgitu.g4.repository.LigneRepository;
import com.sgitu.g4.repository.TrajetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LigneService {

	private final LigneRepository ligneRepository;
	private final TrajetRepository trajetRepository;

	@Transactional
	public LigneResponse create(LigneRequest request) {
		if (ligneRepository.existsByCodeIgnoreCase(request.getCode())) {
			throw new BadRequestException("Code ligne déjà utilisé : " + request.getCode());
		}
		Ligne entity = Ligne.builder()
				.code(request.getCode().trim())
				.nom(request.getNom().trim())
				.description(request.getDescription())
				.active(request.getActive())
				.build();
		return EntityMapper.toDto(ligneRepository.save(entity));
	}

	@Transactional(readOnly = true)
	public List<LigneResponse> findAll() {
		return ligneRepository.findAll().stream().map(EntityMapper::toDto).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<LigneResponse> findActives() {
		return ligneRepository.findByActiveTrue().stream().map(EntityMapper::toDto).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public LigneResponse findById(Long id) {
		return ligneRepository.findById(id).map(EntityMapper::toDto)
				.orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable : " + id));
	}

	@Transactional
	public LigneResponse update(Long id, LigneRequest request) {
		Ligne entity = ligneRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable : " + id));
		ligneRepository.findByCodeIgnoreCase(request.getCode().trim()).ifPresent(other -> {
			if (!other.getId().equals(id)) {
				throw new BadRequestException("Code ligne déjà utilisé : " + request.getCode());
			}
		});
		entity.setCode(request.getCode().trim());
		entity.setNom(request.getNom().trim());
		entity.setDescription(request.getDescription());
		entity.setActive(request.getActive());
		return EntityMapper.toDto(ligneRepository.save(entity));
	}

	@Transactional
	public void delete(Long id) {
		if (!ligneRepository.existsById(id)) {
			throw new ResourceNotFoundException("Ligne introuvable : " + id);
		}
		ligneRepository.deleteById(id);
	}

	@Transactional(readOnly = true)
	public List<TrajetResponse> trajetsForLigne(Long ligneId) {
		if (!ligneRepository.existsById(ligneId)) {
			throw new ResourceNotFoundException("Ligne introuvable : " + ligneId);
		}
		return trajetRepository.findByLigneIdOrderByCodeAsc(ligneId).stream()
				.map(EntityMapper::toDto)
				.collect(Collectors.toList());
	}
}
