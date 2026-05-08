package com.sgitu.servicegestionincidents.service;

import com.sgitu.servicegestionincidents.dto.request.SignalementRequestDTO;
import com.sgitu.servicegestionincidents.dto.response.*;
import com.sgitu.servicegestionincidents.model.enums.StatutIncident;
import com.sgitu.servicegestionincidents.exception.IncidentNotFoundException;
import com.sgitu.servicegestionincidents.model.entity.Action;
import com.sgitu.servicegestionincidents.model.entity.Incident;
import com.sgitu.servicegestionincidents.repository.ActionRepository;
import com.sgitu.servicegestionincidents.repository.IncidentRepository;
import com.sgitu.servicegestionincidents.repository.LocalisationRepository;
import com.sgitu.servicegestionincidents.repository.PreuveRepository;
import com.sgitu.servicegestionincidents.model.entity.Localisation;
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
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final ActionRepository actionRepository;
    private final LocalisationRepository localisationRepository;
    private final PreuveRepository preuveRepository;
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;

    @Override
    public SignalementResponseDTO signalerIncident(SignalementRequestDTO request, Long declarantId) {
        log.info("Signalement d'un nouvel incident par l'utilisateur {}", declarantId);

        // 1. Création de la localisation
        Localisation localisation = Localisation.builder()
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        localisation = localisationRepository.save(localisation);

        // 2. Création de l'incident
        Incident incident = Incident.builder()
                .reference("INC-" + System.currentTimeMillis())
                .type(request.getType())
                .description(request.getDescription())
                .dateIncident(request.getDateIncident())
                .dateSignalement(LocalDateTime.now())
                .statut(StatutIncident.NOUVEAU)
                .gravite(com.sgitu.servicegestionincidents.model.enums.NiveauGravite.MOYEN) // Par défaut
                .declarantId(declarantId)
                .vehiculeId(request.getVehiculeId())
                .source("USER")
                .localisation(localisation)
                .build();

        // 3. Gestion des preuves
        if (request.getPreuves() != null && !request.getPreuves().isEmpty()) {
            for (com.sgitu.servicegestionincidents.dto.request.PreuveDTO preuveDTO : request.getPreuves()) {
                com.sgitu.servicegestionincidents.model.entity.Preuve preuve = com.sgitu.servicegestionincidents.model.entity.Preuve.builder()
                        .incident(incident)
                        .type(preuveDTO.getType())
                        .description(preuveDTO.getDescription())
                        .fichier(preuveDTO.getFichierBase64())
                        .dateAjout(LocalDateTime.now())
                        .build();
                incident.addPreuve(preuve);
            }
        }

        // 4. Sauvegarde de l'incident (cascade sur preuves)
        incident = incidentRepository.save(incident);

        // 5. Création de l'action initiale
        Action actionInitiale = Action.builder()
                .incident(incident)
                .type(com.sgitu.servicegestionincidents.model.enums.TypeAction.CREATION)
                .dateAction(LocalDateTime.now())
                .auteurId(declarantId)
                .description("Signalement initial de l'incident")
                .nouveauStatut(StatutIncident.NOUVEAU)
                .build();
        actionRepository.save(actionInitiale);

        // 6. Envoi de la notification
        try {
            notificationService.envoyerConfirmation(incident);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification: {}", e.getMessage());
        }

        return modelMapper.map(incident, SignalementResponseDTO.class);
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
        log.info("Clôture de l'incident ID: {}", id);
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident non trouvé"));

        StatutIncident ancienStatut = incident.getStatut();
        incident.setStatut(StatutIncident.CLOTURE);
        incidentRepository.save(incident);

        Action action = Action.builder()
                .incident(incident)
                .type(com.sgitu.servicegestionincidents.model.enums.TypeAction.CLOTURE)
                .dateAction(LocalDateTime.now())
                .auteurId(getCurrentUserId())
                .description("Clôture: " + motif)
                .ancienStatut(ancienStatut)
                .nouveauStatut(StatutIncident.CLOTURE)
                .build();
        actionRepository.save(action);
    }

    @Override
    public void escaladerIncident(Long id, String motif) {
        log.info("Escalade de l'incident ID: {}", id);
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident non trouvé"));

        StatutIncident ancienStatut = incident.getStatut();
        incident.setStatut(StatutIncident.ESCALADE);
        incidentRepository.save(incident);

        Action action = Action.builder()
                .incident(incident)
                .type(com.sgitu.servicegestionincidents.model.enums.TypeAction.ESCALADE)
                .dateAction(LocalDateTime.now())
                .auteurId(getCurrentUserId())
                .description("Escalade: " + motif)
                .ancienStatut(ancienStatut)
                .nouveauStatut(StatutIncident.ESCALADE)
                .build();
        actionRepository.save(action);

        notificationService.envoyerEscalade(incident, motif);
    }

    @Override
    public void affecterResponsable(Long id, Long responsableId) {
        log.info("Affectation du responsable {} à l'incident ID: {}", responsableId, id);
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident non trouvé"));

        incident.setResponsableId(responsableId);
        incident.setStatut(StatutIncident.ASSIGNE);
        incidentRepository.save(incident);

        Action action = Action.builder()
                .incident(incident)
                .type(com.sgitu.servicegestionincidents.model.enums.TypeAction.ASSIGNATION)
                .dateAction(LocalDateTime.now())
                .auteurId(getCurrentUserId())
                .description("Assignation au responsable ID: " + responsableId)
                .nouveauStatut(StatutIncident.ASSIGNE)
                .build();
        actionRepository.save(action);
    }

    @Override
    public void mettreAJourStatut(Long id, StatutIncident statut) {
        log.info("Mise à jour du statut de l'incident ID: {} vers {}", id, statut);
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident non trouvé"));

        StatutIncident ancienStatut = incident.getStatut();
        incident.setStatut(statut);
        incidentRepository.save(incident);

        Action action = Action.builder()
                .incident(incident)
                .type(com.sgitu.servicegestionincidents.model.enums.TypeAction.CHANGEMENT_STATUT)
                .dateAction(LocalDateTime.now())
                .auteurId(getCurrentUserId())
                .description("Changement de statut de " + ancienStatut + " à " + statut)
                .ancienStatut(ancienStatut)
                .nouveauStatut(statut)
                .build();
        actionRepository.save(action);

        notificationService.envoyerChangementStatut(incident, ancienStatut.name());
    }

    private Long getCurrentUserId() {
        try {
            String principal = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return Long.valueOf(principal);
        } catch (Exception e) {
            return 0L; // ID par défaut si non authentifié (dev mode)
        }
    }
}
