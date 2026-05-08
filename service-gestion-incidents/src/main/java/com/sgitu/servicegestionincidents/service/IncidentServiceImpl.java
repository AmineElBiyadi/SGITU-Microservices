package com.sgitu.servicegestionincidents.service;

import com.sgitu.servicegestionincidents.dto.request.SignalementRequestDTO;
import com.sgitu.servicegestionincidents.dto.response.*;
import com.sgitu.servicegestionincidents.model.enums.StatutIncident;
import com.sgitu.servicegestionincidents.exception.IncidentNotFoundException;
import com.sgitu.servicegestionincidents.model.entity.Action;
import com.sgitu.servicegestionincidents.model.entity.Incident;
import com.sgitu.servicegestionincidents.repository.ActionRepository;
import com.sgitu.servicegestionincidents.repository.IncidentRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final ActionRepository actionRepository;
    private final ModelMapper modelMapper;

    @Override
    public SignalementResponseDTO signalerIncident(SignalementRequestDTO request, Long declarantId) {
        // TODO
        return null;
    }

    @Override
    public IncidentResponseDTO consulterIncident(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident non trouvé avec l'ID: " + id));
        
        return mapToDTO(incident);
    }

    @Override
    public List<ActionDTO> consulterSuivi(Long incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new IncidentNotFoundException("Incident non trouvé avec l'ID: " + incidentId);
        }
        
        List<Action> actions = actionRepository.findByIncidentIdOrderByDateActionDesc(incidentId);
        return actions.stream()
                .map(action -> modelMapper.map(action, ActionDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<IncidentResponseDTO> filtrerIncidents(Map<String, Object> criteres) {
        Specification<Incident> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (criteres.get("statut") != null && !criteres.get("statut").toString().isEmpty()) {
                predicates.add(cb.equal(root.get("statut"), criteres.get("statut")));
            }
            
            if (criteres.get("gravite") != null && !criteres.get("gravite").toString().isEmpty()) {
                predicates.add(cb.equal(root.get("gravite"), criteres.get("gravite")));
            }
            
            if (criteres.get("declarantId") != null && !criteres.get("declarantId").toString().isEmpty()) {
                predicates.add(cb.equal(root.get("declarantId"), criteres.get("declarantId")));
            }
            
            if (criteres.get("ligne") != null && !criteres.get("ligne").toString().isEmpty()) {
                predicates.add(cb.equal(root.get("localisation").get("ligneTransport"), criteres.get("ligne")));
            }
            
            if (criteres.get("dateDebut") != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateSignalement"), (LocalDateTime) criteres.get("dateDebut")));
            }
            
            if (criteres.get("dateFin") != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateSignalement"), (LocalDateTime) criteres.get("dateFin")));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        List<Incident> incidents = incidentRepository.findAll(spec);
        return incidents.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private IncidentResponseDTO mapToDTO(Incident incident) {
        IncidentResponseDTO dto = modelMapper.map(incident, IncidentResponseDTO.class);
        if (incident.getLocalisation() != null) {
            dto.setLatitude(incident.getLocalisation().getLatitude());
            dto.setLongitude(incident.getLocalisation().getLongitude());
            dto.setLigneTransport(incident.getLocalisation().getLigneTransport());
        }
        return dto;
    }

    @Override
    public void cloturerIncident(Long id, String motif) {
        // TODO
    }

    @Override
    public void escaladerIncident(Long id, String motif) {
        // TODO
    }

    @Override
    public void affecterResponsable(Long id, Long responsableId) {
        // TODO
    }

    @Override
    public void mettreAJourStatut(Long id, StatutIncident statut) {
        // TODO
    }
}
