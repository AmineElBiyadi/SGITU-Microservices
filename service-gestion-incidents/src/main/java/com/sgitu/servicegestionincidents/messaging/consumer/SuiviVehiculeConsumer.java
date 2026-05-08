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
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SuiviVehiculeConsumer {

    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;
    private final ReferenceGenerator referenceGenerator;

    @RabbitListener(queues = MessagingConstants.SUIVI_VEHICULE_QUEUE)
    public void recevoirIncidentVehicule(IncidentDetecteEvent event) {
        log.info("Incident détecté par IoT - Type: {}, Véhicule: {}", event.getType(), event.getVehiculeId());

        try {
            Localisation localisation = Localisation.builder()
                    .latitude(event.getLatitude())
                    .longitude(event.getLongitude())
                    .build();

            Incident incident = Incident.builder()
                    .reference(referenceGenerator.generate())
                    .type(TypeIncident.valueOf(event.getType()))
                    .description(event.getDescription())
                    .dateSignalement(LocalDateTime.now())
                    .dateIncident(event.getDateDetection())
                    .statut(StatutIncident.NOUVEAU)
                    .gravite(NiveauGravite.valueOf(event.getGravite()))
                    .declarantId(0L) // Système automatique
                    .localisation(localisation)
                    .build();

            Incident saved = incidentRepository.save(incident);
            log.info("Incident créé automatiquement: {}", saved.getReference());

            notificationService.notifierActeurs(saved);

        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement IoT: {}", e.getMessage(), e);
        }
    }
}
