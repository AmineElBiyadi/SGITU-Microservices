package com.sgitu.servicegestionincidents.messaging.consumer;

import com.sgitu.servicegestionincidents.messaging.constant.MessagingConstants;
import com.sgitu.servicegestionincidents.messaging.event.IncidentDetecteEvent;
import com.sgitu.servicegestionincidents.model.entity.Action;
import com.sgitu.servicegestionincidents.model.entity.Incident;
import com.sgitu.servicegestionincidents.model.entity.Localisation;
import com.sgitu.servicegestionincidents.model.enums.StatutIncident;
import com.sgitu.servicegestionincidents.model.enums.TypeAction;
import com.sgitu.servicegestionincidents.model.enums.TypeIncident;
import com.sgitu.servicegestionincidents.repository.IncidentRepository;
import com.sgitu.servicegestionincidents.service.NotificationService;
import com.sgitu.servicegestionincidents.util.ReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SuiviVehiculeConsumer {

    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;
    private final ReferenceGenerator referenceGenerator;

    @KafkaListener(topics = MessagingConstants.SUIVI_VEHICULE_TOPIC, groupId = MessagingConstants.GROUP_ID)
    public void recevoirIncidentVehicule(IncidentDetecteEvent event) {
        log.info("Incident détecté par IoT - Type: {}, Véhicule: {}", event.getType(), event.getVehiculeId());

        try {
            // --- DUPLICATE DETECTION (par vehiculeId) ---
            if (event.getVehiculeId() != null && !event.getVehiculeId().isBlank()) {
                Optional<Incident> existant = incidentRepository.trouverIncidentNonResoluParVehicule(
                        event.getVehiculeId());

                if (existant.isPresent()) {
                    // Doublon détecté : enrichir l'incident existant avec la confirmation IoT
                    Incident incident = existant.get();
                    log.info("Doublon IoT détecté pour véhicule {} — Enrichissement de l'incident {}",
                            event.getVehiculeId(), incident.getReference());

                    Action action = Action.builder()
                            .type(TypeAction.COMMENTAIRE)
                            .description(String.format("Confirmation IoT supplémentaire reçue — %s", event.getDescription()))
                            .auteurId(0L) // 0L = Système IoT
                            .dateAction(LocalDateTime.now())
                            .build();
                    incident.addAction(action);

                    incidentRepository.save(incident);
                    return; // Ne pas créer de nouvel incident
                }
            }

            // --- NO DUPLICATE: CREATE NEW INCIDENT ---
            TypeIncident typeIncident = TypeIncident.valueOf(event.getType());

            Localisation localisation = Localisation.builder()
                    .latitude(event.getLatitude())
                    .longitude(event.getLongitude())
                    .ligneTransport(event.getLigneTransport())
                    .build();

            // Gravité par défaut basée sur le type d'incident
            Incident incident = Incident.builder()
                    .reference(referenceGenerator.generate())
                    .source("IOT")
                    .type(typeIncident)
                    .statut(StatutIncident.NOUVEAU)
                    .gravite(typeIncident.getGraviteParDefaut())
                    .vehiculeId(event.getVehiculeId())
                    .declarantId(0L) // 0L = Système automatique IoT
                    .description(event.getDescription())
                    .localisation(localisation)
                    .dateSignalement(LocalDateTime.now())
                    .dateIncident(event.getDateDetection())
                    .dateLimiteResolution(LocalDateTime.now().plusHours(
                            typeIncident.getGraviteParDefaut().getDelaiMaxTraitement()))
                    .build();

            // Action de création dans l'historique
            Action actionCreation = Action.builder()
                    .type(TypeAction.CREATION)
                    .description(String.format("Incident créé automatiquement par IoT (G7) — %s", event.getDescription()))
                    .auteurId(0L) // 0L = Système IoT
                    .dateAction(LocalDateTime.now())
                    .nouveauStatut(StatutIncident.NOUVEAU)
                    .build();
            incident.addAction(actionCreation);

            Incident saved = incidentRepository.save(incident);
            log.info("Incident créé automatiquement: {} — Type: {}, Gravité: {}",
                    saved.getReference(), saved.getType(), saved.getGravite());

            // Notification prioritaire au superviseur via G5
            notificationService.envoyerAlerteIoT(saved);

        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement IoT: {}", e.getMessage(), e);
        }
    }
}
