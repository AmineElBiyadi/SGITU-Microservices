package com.sgitu.servicegestionincidents.service;

import com.sgitu.servicegestionincidents.dto.request.SignalementRequestDTO;
import com.sgitu.servicegestionincidents.dto.response.*;
import com.sgitu.servicegestionincidents.exception.IllegalStateTransitionException;
import com.sgitu.servicegestionincidents.exception.IncidentNotFoundException;
import com.sgitu.servicegestionincidents.exception.InvalidRequestException;
import com.sgitu.servicegestionincidents.messaging.event.IncidentAnalytiqueEvent;
import com.sgitu.servicegestionincidents.messaging.event.IncidentTransportEvent;
import com.sgitu.servicegestionincidents.messaging.producer.AnalytiqueProducer;
import com.sgitu.servicegestionincidents.messaging.producer.TransportProducer;
import com.sgitu.servicegestionincidents.model.entity.Action;
import com.sgitu.servicegestionincidents.model.entity.Incident;
import com.sgitu.servicegestionincidents.model.entity.Localisation;
import com.sgitu.servicegestionincidents.model.entity.Preuve;
import com.sgitu.servicegestionincidents.model.enums.*;
import com.sgitu.servicegestionincidents.repository.IncidentRepository;
import com.sgitu.servicegestionincidents.util.ReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;
    private final TransportProducer transportProducer;
    private final AnalytiqueProducer analytiqueProducer;
    private final ReferenceGenerator referenceGenerator;
    private final ModelMapper modelMapper;

    @Value("${incident.duplicate.radius-meters:500}")
    private Double duplicateRadiusMeters;

    // ============================================================
    // Task 1.1 — signalerIncident()
    // Dual duplicate detection + default gravity per type + source tracking
    // ============================================================
    @Override
    public SignalementResponseDTO signalerIncident(SignalementRequestDTO request) {
        log.info("Nouveau signalement reçu - Type: {}, DeclarantId: {}, Role: {}",
                request.getType(), request.getDeclarantId(), request.getRole());

        // Determine source based on role
        String source = determinerSource(request.getRole());

        // --- DUPLICATE DETECTION ---
        Optional<Incident> doublon = detecterDoublon(request);

        if (doublon.isPresent()) {
            return traiterDoublon(doublon.get(), request, source);
        }

        // --- NO DUPLICATE: CREATE NEW INCIDENT ---
        return creerNouvelIncident(request, source);
    }

    // ============================================================
    // Task 1.2 — affecterResponsable()
    // Validate ANALYSE status → ASSIGNE, fire G4 CONFIRME + G5
    // ============================================================
    @Override
    public void affecterResponsable(Long id, Long responsableId) {
        log.info("Affectation du responsable {} à l'incident {}", responsableId, id);

        Incident incident = trouverIncidentOuErreur(id);

        // L'affectation ne peut se faire que depuis ANALYSE ou ESCALADE
        if (incident.getStatut() != StatutIncident.ANALYSE &&
                incident.getStatut() != StatutIncident.ESCALADE) {
            throw new IllegalStateTransitionException(
                    String.format("Impossible d'affecter un responsable : l'incident %s est en statut %s. " +
                            "Le statut doit être ANALYSE ou ESCALADE.", incident.getReference(), incident.getStatut()));
        }

        StatutIncident ancienStatut = incident.getStatut();
        incident.setResponsableId(responsableId);
        incident.setStatut(StatutIncident.ASSIGNE);

        // Enregistrer l'action dans l'historique
        Action action = Action.builder()
                .type(TypeAction.ASSIGNATION)
                .description(String.format("Incident assigné au responsable %d", responsableId))
                .auteurId(responsableId) // Le superviseur qui assigne
                .dateAction(LocalDateTime.now())
                .ancienStatut(ancienStatut)
                .nouveauStatut(StatutIncident.ASSIGNE)
                .build();
        incident.addAction(action);

        // Calcul du SLA basé sur la gravité
        incident.setDateLimiteResolution(
                LocalDateTime.now().plusHours(incident.getGravite().getDelaiMaxTraitement()));

        incidentRepository.save(incident);
        log.info("Incident {} assigné au responsable {} — Statut: ASSIGNE", incident.getReference(), responsableId);

        // Déclencher G4 (Transport) — statut CONFIRME
        envoyerEvenementTransport(incident, "CONFIRME");
        incident.setTransportNotifie(true);
        incidentRepository.save(incident);

        // Déclencher G5 (Notification) — changement de statut
        notificationService.envoyerChangementStatut(incident, ancienStatut.name());
    }

    // ============================================================
    // Task 1.3 — mettreAJourStatut()
    // Transitions EN_TRAITEMENT et RESOLU avec vérification
    // ============================================================
    @Override
    public void mettreAJourStatut(Long id, StatutIncident nouveauStatut) {
        log.info("Mise à jour du statut de l'incident {} vers {}", id, nouveauStatut);

        Incident incident = trouverIncidentOuErreur(id);
        StatutIncident ancienStatut = incident.getStatut();

        // Vérifier les transitions autorisées
        validerTransition(ancienStatut, nouveauStatut);

        incident.setStatut(nouveauStatut);

        // Si résolu, enregistrer la date de résolution
        if (nouveauStatut == StatutIncident.RESOLU) {
            incident.setDateResolution(LocalDateTime.now());
        }

        // Enregistrer l'action dans l'historique
        Action action = Action.builder()
                .type(TypeAction.CHANGEMENT_STATUT)
                .description(String.format("Statut changé de %s à %s", ancienStatut, nouveauStatut))
                .auteurId(0L) // TODO: sera rempli par le controller via JWT (Membre 2)
                .dateAction(LocalDateTime.now())
                .ancienStatut(ancienStatut)
                .nouveauStatut(nouveauStatut)
                .build();
        incident.addAction(action);

        incidentRepository.save(incident);
        log.info("Incident {} — Statut mis à jour: {} → {}", incident.getReference(), ancienStatut, nouveauStatut);

        // Déclencher G4 (Transport) si RESOLU
        if (nouveauStatut == StatutIncident.RESOLU) {
            envoyerEvenementTransport(incident, "RESOLU");
        }

        // Déclencher G5 (Notification) — changement de statut
        notificationService.envoyerChangementStatut(incident, ancienStatut.name());
    }

    // ============================================================
    // Task 1.4 — escaladerIncident()
    // Gravité → CRITIQUE, Statut → ESCALADE, G5 alerte urgence
    // ============================================================
    @Override
    public void escaladerIncident(Long id, String motif) {
        log.info("Escalade de l'incident {} — Motif: {}", id, motif);

        Incident incident = trouverIncidentOuErreur(id);

        if (!incident.isEscaladable()) {
            throw new IllegalStateTransitionException(
                    String.format("Impossible d'escalader l'incident %s en statut %s. " +
                            "L'escalade n'est possible que depuis ASSIGNE ou EN_TRAITEMENT.",
                            incident.getReference(), incident.getStatut()));
        }

        StatutIncident ancienStatut = incident.getStatut();
        incident.setStatut(StatutIncident.ESCALADE);
        incident.setGravite(NiveauGravite.CRITIQUE);

        // Enregistrer l'escalade dans l'historique
        Action action = Action.builder()
                .type(TypeAction.ESCALADE)
                .description(String.format("Incident escaladé — Motif: %s", motif))
                .auteurId(0L) // TODO: sera rempli par le controller via JWT (Membre 2)
                .dateAction(LocalDateTime.now())
                .ancienStatut(ancienStatut)
                .nouveauStatut(StatutIncident.ESCALADE)
                .build();
        incident.addAction(action);

        incidentRepository.save(incident);
        log.info("Incident {} escaladé — Gravité: CRITIQUE, Statut: ESCALADE", incident.getReference());

        // Déclencher G5 (Notification) — alerte rouge vers la direction
        notificationService.envoyerEscalade(incident, motif);
    }

    // ============================================================
    // Task 1.5 — cloturerIncident()
    // Vérification RESOLU → CLOTURE, envoi G8 analytique
    // ============================================================
    @Override
    public void cloturerIncident(Long id, String motif) {
        log.info("Clôture de l'incident {} — Motif: {}", id, motif);

        Incident incident = trouverIncidentOuErreur(id);

        if (!incident.isCloturable()) {
            throw new IllegalStateTransitionException(
                    String.format("Impossible de clôturer l'incident %s en statut %s. " +
                            "L'incident doit être en statut RESOLU.", incident.getReference(), incident.getStatut()));
        }

        StatutIncident ancienStatut = incident.getStatut();
        incident.setStatut(StatutIncident.CLOTURE);

        // Enregistrer la clôture dans l'historique
        Action action = Action.builder()
                .type(TypeAction.CLOTURE)
                .description(String.format("Incident clôturé — Motif: %s", motif))
                .auteurId(0L) // TODO: sera rempli par le controller via JWT (Membre 2)
                .dateAction(LocalDateTime.now())
                .ancienStatut(ancienStatut)
                .nouveauStatut(StatutIncident.CLOTURE)
                .build();
        incident.addAction(action);

        incidentRepository.save(incident);
        log.info("Incident {} clôturé avec succès", incident.getReference());

        // Déclencher G8 (Analytique) — envoi du dossier complet
        envoyerEvenementAnalytique(incident);

        // Déclencher G5 (Notification) — changement de statut
        notificationService.envoyerChangementStatut(incident, ancienStatut.name());
    }

    // ============================================================
    // Task 1.6 — annulerIncident()
    // ANNULE + G4 REJETE si transportNotifie + G8 analytique
    // ============================================================
    @Override
    public void annulerIncident(Long id, String motif) {
        log.info("Annulation de l'incident {} — Motif: {}", id, motif);

        Incident incident = trouverIncidentOuErreur(id);

        if (!incident.isAnnulable()) {
            throw new IllegalStateTransitionException(
                    String.format("Impossible d'annuler l'incident %s en statut %s. " +
                            "L'annulation n'est possible que depuis NOUVEAU, ANALYSE ou ASSIGNE.",
                            incident.getReference(), incident.getStatut()));
        }

        StatutIncident ancienStatut = incident.getStatut();
        incident.setStatut(StatutIncident.ANNULE);

        // Enregistrer l'annulation dans l'historique
        Action action = Action.builder()
                .type(TypeAction.CHANGEMENT_STATUT)
                .description(String.format("Incident annulé (fausse alerte) — Motif: %s", motif))
                .auteurId(0L) // TODO: sera rempli par le controller via JWT (Membre 2)
                .dateAction(LocalDateTime.now())
                .ancienStatut(ancienStatut)
                .nouveauStatut(StatutIncident.ANNULE)
                .build();
        incident.addAction(action);

        incidentRepository.save(incident);
        log.info("Incident {} annulé", incident.getReference());

        // Edge-case: Si G4 avait déjà été notifié, envoyer REJETE
        if (incident.isTransportNotifie()) {
            log.info("G4 avait été notifié — envoi du statut REJETE pour l'incident {}", incident.getReference());
            envoyerEvenementTransport(incident, "REJETE");
        }

        // Envoyer G8 (Analytique) — enregistrer la fausse alerte
        envoyerEvenementAnalytique(incident);

        // Déclencher G5 (Notification) — changement de statut
        notificationService.envoyerChangementStatut(incident, ancienStatut.name());
    }

    // ============================================================
    // consulterIncident() — Lecture simple (implémenté pour complétude)
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public IncidentResponseDTO consulterIncident(Long id) {
        log.info("Consultation de l'incident {}", id);
        Incident incident = trouverIncidentOuErreur(id);
        return mapToIncidentResponseDTO(incident);
    }

    // ============================================================
    // consulterSuivi() — Historique des actions
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public List<ActionDTO> consulterSuivi(Long incidentId) {
        log.info("Consultation du suivi de l'incident {}", incidentId);
        Incident incident = trouverIncidentOuErreur(incidentId);

        return incident.getActions().stream()
                .map(action -> modelMapper.map(action, ActionDTO.class))
                .collect(Collectors.toList());
    }

    // ============================================================
    // filtrerIncidents() — Filtrage basique par critères
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public List<IncidentResponseDTO> filtrerIncidents(Map<String, Object> criteres) {
        log.info("Filtrage des incidents avec critères: {}", criteres);

        List<Incident> incidents;

        Object statutObj = criteres.get("statut");
        Object declarantIdObj = criteres.get("declarantId");

        boolean hasStatut = statutObj != null && statutObj instanceof StatutIncident;
        boolean hasDeclarant = declarantIdObj != null && declarantIdObj instanceof Long;

        if (hasStatut && hasDeclarant) {
            incidents = incidentRepository.findByStatutAndDeclarantId(
                    (StatutIncident) statutObj, (Long) declarantIdObj);
        } else if (hasStatut) {
            incidents = incidentRepository.findByStatut((StatutIncident) statutObj);
        } else if (hasDeclarant) {
            incidents = incidentRepository.findByDeclarantId((Long) declarantIdObj);
        } else {
            incidents = incidentRepository.findAll();
        }

        return incidents.stream()
                .map(this::mapToIncidentResponseDTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // PRIVATE HELPER METHODS
    // ============================================================

    /**
     * Détermine la source du signalement en fonction du rôle.
     */
    private String determinerSource(String role) {
        if (role == null) {
            return "USER";
        }
        return switch (role.toUpperCase()) {
            case "CONDUCTEUR" -> "CONDUCTEUR";
            case "VOYAGEUR" -> "VOYAGEUR";
            default -> "USER";
        };
    }

    /**
     * Détection de doublon : par vehiculeId si présent, sinon par localisation +
     * type.
     */
    private Optional<Incident> detecterDoublon(SignalementRequestDTO request) {
        // Stratégie 1 : Doublon par vehiculeId
        if (request.getVehiculeId() != null && !request.getVehiculeId().isBlank()) {
            log.debug("Recherche de doublon par vehiculeId: {}", request.getVehiculeId());
            Optional<Incident> doublon = incidentRepository.trouverIncidentNonResoluParVehicule(
                    request.getVehiculeId());
            if (doublon.isPresent()) {
                log.info("Doublon détecté par vehiculeId {} — Incident existant: {}",
                        request.getVehiculeId(), doublon.get().getReference());
                return doublon;
            }
        }

        // Stratégie 2 : Doublon par localisation + type (pour incidents sans
        // vehiculeId)
        if (request.getVehiculeId() == null || request.getVehiculeId().isBlank()) {
            log.debug("Recherche de doublon par localisation+type: type={}, lat={}, lng={}, radius={}m",
                    request.getType(), request.getLatitude(), request.getLongitude(), duplicateRadiusMeters);
            Optional<Incident> doublon = incidentRepository.trouverIncidentNonResoluParLocalisationEtType(
                    request.getType(), request.getLatitude(), request.getLongitude(), duplicateRadiusMeters);
            if (doublon.isPresent()) {
                log.info("Doublon détecté par localisation+type — Incident existant: {}",
                        doublon.get().getReference());
                return doublon;
            }
        }

        return Optional.empty();
    }

    /**
     * Traite un doublon : enrichit l'incident existant avec une action de
     * confirmation.
     */
    private SignalementResponseDTO traiterDoublon(Incident existing, SignalementRequestDTO request, String source) {
        String description = String.format("Signalement supplémentaire reçu de %s (déclarant %d)",
                source, request.getDeclarantId());

        Action action = Action.builder()
                .type(TypeAction.COMMENTAIRE)
                .description(description)
                .auteurId(request.getDeclarantId())
                .dateAction(LocalDateTime.now())
                .build();
        existing.addAction(action);

        incidentRepository.save(existing);
        log.info("Doublon enrichi — Incident {} reçoit une confirmation supplémentaire de {}",
                existing.getReference(), source);

        return SignalementResponseDTO.builder()
                .incidentId(existing.getId())
                .reference(existing.getReference())
                .statut(existing.getStatut())
                .dateSignalement(existing.getDateSignalement())
                .message("Signalement enregistré comme confirmation d'un incident existant.")
                .doublon(true)
                .build();
    }

    /**
     * Crée un nouvel incident à partir du signalement.
     */
    private SignalementResponseDTO creerNouvelIncident(SignalementRequestDTO request, String source) {
        // Gravité par défaut basée sur le type d'incident
        NiveauGravite gravite = request.getType().getGraviteParDefaut();

        Localisation localisation = Localisation.builder()
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .ligneTransport(request.getLigneTransport())
                .build();

        Incident incident = Incident.builder()
                .reference(referenceGenerator.generate())
                .type(request.getType())
                .description(request.getDescription())
                .dateSignalement(LocalDateTime.now())
                .dateIncident(request.getDateIncident())
                .statut(StatutIncident.NOUVEAU)
                .gravite(gravite)
                .declarantId(request.getDeclarantId())
                .vehiculeId(request.getVehiculeId())
                .source(source)
                .localisation(localisation)
                .dateLimiteResolution(LocalDateTime.now().plusHours(gravite.getDelaiMaxTraitement()))
                .build();

        // Ajouter les preuves si fournies
        if (request.getPreuves() != null) {
            request.getPreuves().forEach(preuveDTO -> {
                Preuve preuve = Preuve.builder()
                        .type(preuveDTO.getType())
                        .fichier(preuveDTO.getFichierBase64())
                        .description(preuveDTO.getDescription())
                        .dateAjout(LocalDateTime.now())
                        .build();
                incident.addPreuve(preuve);
            });
        }

        // Action de création dans l'historique
        Action actionCreation = Action.builder()
                .type(TypeAction.CREATION)
                .description(String.format("Incident signalé par %s (source: %s) — Gravité par défaut: %s",
                        request.getDeclarantId(), source, gravite))
                .auteurId(request.getDeclarantId())
                .dateAction(LocalDateTime.now())
                .nouveauStatut(StatutIncident.NOUVEAU)
                .build();
        incident.addAction(actionCreation);

        Incident saved = incidentRepository.save(incident);
        log.info("Nouvel incident créé: {} — Type: {}, Gravité: {}, Source: {}",
                saved.getReference(), saved.getType(), saved.getGravite(), source);

        // Notification G5 — confirmation au déclarant
        notificationService.envoyerConfirmation(saved);

        return SignalementResponseDTO.builder()
                .incidentId(saved.getId())
                .reference(saved.getReference())
                .statut(saved.getStatut())
                .dateSignalement(saved.getDateSignalement())
                .message("Incident signalé avec succès. Référence: " + saved.getReference())
                .doublon(false)
                .build();
    }

    /**
     * Valide les transitions de statut autorisées selon la machine à états.
     * Seules EN_TRAITEMENT et RESOLU sont gérées ici (les autres ont des méthodes
     * dédiées).
     */
    private void validerTransition(StatutIncident ancien, StatutIncident nouveau) {
        boolean valide = switch (nouveau) {
            case ANALYSE -> ancien == StatutIncident.NOUVEAU;
            case EN_TRAITEMENT -> ancien == StatutIncident.ASSIGNE;
            case RESOLU -> ancien == StatutIncident.EN_TRAITEMENT;
            default -> throw new IllegalStateTransitionException(
                    String.format("La transition vers %s n'est pas gérée par cette méthode. " +
                            "Utilisez les endpoints dédiés (affecter, escalader, cloturer, annuler).", nouveau));
        };

        if (!valide) {
            throw new IllegalStateTransitionException(
                    String.format("Transition invalide: %s → %s. Cette transition n'est pas autorisée.", ancien,
                            nouveau));
        }
    }

    /**
     * Construit et envoie un événement vers G4 (Transport).
     */
    private void envoyerEvenementTransport(Incident incident, String statut) {
        IncidentTransportEvent event = IncidentTransportEvent.builder()
                .referenceIncident(incident.getReference())
                .type(incident.getType().name())
                .statut(statut)
                .vehiculeId(incident.getVehiculeId())
                .ligneId(incident.getLocalisation().getLigneTransport())
                .description(incident.getDescription())
                .latitude(incident.getLocalisation().getLatitude())
                .longitude(incident.getLocalisation().getLongitude())
                .timestamp(LocalDateTime.now())
                .build();

        transportProducer.notifierTransport(event);
        log.info("Événement G4 envoyé — Incident: {}, Statut: {}", incident.getReference(), statut);
    }

    /**
     * Construit et envoie un événement vers G8 (Analytique).
     */
    private void envoyerEvenementAnalytique(Incident incident) {
        IncidentAnalytiqueEvent event = IncidentAnalytiqueEvent.builder()
                .reference(incident.getReference())
                .source(incident.getSource())
                .type(incident.getType().name())
                .gravite(incident.getGravite().name())
                .statut(incident.getStatut().name())
                .vehiculeId(incident.getVehiculeId())
                .ligneTransport(incident.getLocalisation().getLigneTransport())
                .declarantId(incident.getDeclarantId())
                .responsableId(incident.getResponsableId())
                .description(incident.getDescription())
                .latitude(incident.getLocalisation().getLatitude())
                .longitude(incident.getLocalisation().getLongitude())
                .dateSignalement(incident.getDateSignalement())
                .dateIncident(incident.getDateIncident())
                .dateResolution(incident.getDateResolution())
                .dateLimiteResolution(incident.getDateLimiteResolution())
                .build();

        analytiqueProducer.envoyerDonneesAnalytique(event);
        log.info("Événement G8 envoyé — Incident: {}, Statut: {}", incident.getReference(), incident.getStatut());
    }

    /**
     * Recherche un incident par ID ou lève une exception 404.
     */
    private Incident trouverIncidentOuErreur(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException(
                        String.format("L'incident avec l'ID %d n'existe pas dans le système.", id)));
    }

    /**
     * Mappe une entité Incident vers un IncidentResponseDTO.
     */
    private IncidentResponseDTO mapToIncidentResponseDTO(Incident incident) {
        return IncidentResponseDTO.builder()
                .id(incident.getId())
                .reference(incident.getReference())
                .type(incident.getType())
                .description(incident.getDescription())
                .dateSignalement(incident.getDateSignalement())
                .dateIncident(incident.getDateIncident())
                .dateLimiteResolution(incident.getDateLimiteResolution())
                .dateResolution(incident.getDateResolution())
                .statut(incident.getStatut())
                .gravite(incident.getGravite())
                .declarantId(incident.getDeclarantId())
                .responsableId(incident.getResponsableId())
                .vehiculeId(incident.getVehiculeId())
                .source(incident.getSource())
                .ligneTransport(
                        incident.getLocalisation() != null ? incident.getLocalisation().getLigneTransport() : null)
                .latitude(incident.getLocalisation() != null ? incident.getLocalisation().getLatitude() : null)
                .longitude(incident.getLocalisation() != null ? incident.getLocalisation().getLongitude() : null)
                .incidentParentRef(incident.getIncidentParentRef())
                .build();
    }
}
