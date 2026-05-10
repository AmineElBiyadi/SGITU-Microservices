package com.sgitu.g4.service;

import com.sgitu.g4.dto.HoraireRequest;
import com.sgitu.g4.dto.HoraireResponse;
import com.sgitu.g4.entity.Arret;
import com.sgitu.g4.entity.Horaire;
import com.sgitu.g4.entity.Trajet;
import com.sgitu.g4.exception.BadRequestException;
import com.sgitu.g4.exception.ResourceNotFoundException;
import com.sgitu.g4.mapper.EntityMapper;
import com.sgitu.g4.repository.ArretRepository;
import com.sgitu.g4.repository.HoraireRepository;
import com.sgitu.g4.repository.TrajetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HoraireService {

	private final HoraireRepository horaireRepository;
	private final TrajetRepository trajetRepository;
	private final ArretRepository arretRepository;

	@Transactional
	public HoraireResponse create(HoraireRequest request) {
		Trajet trajet = trajetRepository.findById(request.getTrajetId())
				.orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable : " + request.getTrajetId()));
		Arret arret = null;
		if (request.getArretId() != null) {
			arret = arretRepository.findById(request.getArretId())
					.orElseThrow(() -> new ResourceNotFoundException("Arrêt introuvable : " + request.getArretId()));
			if (!Objects.equals(arret.getLigne().getId(), trajet.getLigne().getId())) {
				throw new BadRequestException("Arrêt incompatible avec la ligne du trajet");
			}
		}
		validateJour(request.getJourSemaine());
		Horaire entity = Horaire.builder()
				.trajet(trajet)
				.arret(arret)
				.heurePassage(request.getHeurePassage())
				.jourSemaine(request.getJourSemaine())
				.validFrom(request.getValidFrom())
				.validTo(request.getValidTo())
				.libelle(request.getLibelle())
				.build();
		return EntityMapper.toDto(horaireRepository.save(entity));
	}

	@Transactional(readOnly = true)
	public List<HoraireResponse> findAll() {
		return horaireRepository.findAll().stream().map(EntityMapper::toDto).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<HoraireResponse> findByLigne(Long ligneId) {
		return horaireRepository.findByTrajetLigneIdOrderByHeurePassageAsc(ligneId).stream()
				.map(EntityMapper::toDto)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public HoraireResponse findById(Long id) {
		return horaireRepository.findById(id).map(EntityMapper::toDto)
				.orElseThrow(() -> new ResourceNotFoundException("Horaire introuvable : " + id));
	}

	@Transactional
	public HoraireResponse update(Long id, HoraireRequest request) {
		Horaire entity = horaireRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Horaire introuvable : " + id));
		Trajet trajet = trajetRepository.findById(request.getTrajetId())
				.orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable : " + request.getTrajetId()));
		Arret arret = null;
		if (request.getArretId() != null) {
			arret = arretRepository.findById(request.getArretId())
					.orElseThrow(() -> new ResourceNotFoundException("Arrêt introuvable : " + request.getArretId()));
			if (!Objects.equals(arret.getLigne().getId(), trajet.getLigne().getId())) {
				throw new BadRequestException("Arrêt incompatible avec la ligne du trajet");
			}
		}
		validateJour(request.getJourSemaine());
		entity.setTrajet(trajet);
		entity.setArret(arret);
		entity.setHeurePassage(request.getHeurePassage());
		entity.setJourSemaine(request.getJourSemaine());
		entity.setValidFrom(request.getValidFrom());
		entity.setValidTo(request.getValidTo());
		entity.setLibelle(request.getLibelle());
		return EntityMapper.toDto(horaireRepository.save(entity));
	}

	@Transactional
	public void delete(Long id) {
		if (!horaireRepository.existsById(id)) {
			throw new ResourceNotFoundException("Horaire introuvable : " + id);
		}
		horaireRepository.deleteById(id);
	}

	private static void validateJour(Integer j) {
		if (j != null && (j < 1 || j > 7)) {
			throw new BadRequestException("jourSemaine entre 1 et 7");
		}
	}
}
