package com.sgitu.servicegestionincidents.messaging.consumer;

import com.sgitu.servicegestionincidents.messaging.constant.MessagingConstants;
import com.sgitu.servicegestionincidents.messaging.event.IncidentDetecteEvent;
import com.sgitu.servicegestionincidents.model.entity.Incident;
import com.sgitu.servicegestionincidents.model.entity.Localisation;
import com.sgitu.servicegestionincidents.model.enums.NiveauGravite;
import com.sgitu.servicegestionincidents.model.enums.StatutIncident;
import com.sgitu.servicegestionincidents.model.enums.TypeIncident;
import com.sgitu.servicegestionincidents.repository.IncidentRepository;
import com.sgitu.servicegestionincidents.service.NotificationService;
import com.sgitu.servicegestionincidents.util.ReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalytiqueConsumer {

    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;
    private final ReferenceGenerator referenceGenerator;

    @KafkaListener(topics = MessagingConstants.ANALYTIQUE_IN_TOPIC, groupId = MessagingConstants.GROUP_ID)
    public void recevoirIncidentAnalytique(IncidentDetecteEvent event) {
        log.info("Incident détecté par analytique - Type: {}, Ligne: {}", event.getType(), event.getLigneTransport());

        try {
            Localisation localisation = Localisation.builder()
                    .latitude(event.getLatitude())
                    .longitude(event.getLongitude())
                    .build();

            Incident incident = Incident.builder()
                    .reference(referenceGenerator.generate())
                    .type(TypeIncident.valueOf(event.getType()))
                    .description("[Détecté par analytique] " + event.getDescription())
                    .dateSignalement(LocalDateTime.now())
                    .dateIncident(event.getDateDetection())
                    .statut(StatutIncident.ANALYSE)
                    .gravite(NiveauGravite.valueOf(event.getGravite()))
                    .declarantId(0L) // Système automatique
                    .localisation(localisation)
                    .build();

            Incident saved = incidentRepository.save(incident);
            log.info("Incident analytique créé: {}", saved.getReference());

            notificationService.notifierActeurs(saved);

        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement analytique: {}", e.getMessage(), e);
        }
    }
}
