package com.sgitu.servicegestionincidents.scheduler;

import com.sgitu.servicegestionincidents.model.entity.Action;
import com.sgitu.servicegestionincidents.model.entity.Incident;
import com.sgitu.servicegestionincidents.model.enums.NiveauGravite;
import com.sgitu.servicegestionincidents.model.enums.StatutIncident;
import com.sgitu.servicegestionincidents.model.enums.TypeAction;
import com.sgitu.servicegestionincidents.repository.IncidentRepository;
import com.sgitu.servicegestionincidents.service.NotificationService;
import com.sgitu.servicegestionincidents.messaging.producer.TransportProducer;
import com.sgitu.servicegestionincidents.messaging.event.IncidentTransportEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IncidentSlaScheduler {

    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;
    private final TransportProducer transportProducer;

    /**
     * Tâche planifiée qui s'exécute toutes les minutes pour vérifier le SLA de 5 minutes
     * sur les nouveaux incidents non encore pris en charge (statut NOUVEAU et non escaladés).
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void verifierSlaIncidents() {
        log.debug("Exécution de la vérification du SLA d'affectation...");
        
        LocalDateTime limite = LocalDateTime.now().minusMinutes(5);
        List<Incident> incidentsEnDepassement = incidentRepository
                .findByStatutAndEscaladeFalseAndDateSignalementBefore(StatutIncident.NOUVEAU, limite);

        if (incidentsEnDepassement.isEmpty()) {
            return;
        }

        log.info("{} incident(s) en dépassement de SLA (5 min) détecté(s). Début de l'escalade automatique.", 
                incidentsEnDepassement.size());

        for (Incident incident : incidentsEnDepassement) {
            try {
                incident.setEscalade(true);
                incident.setGravite(NiveauGravite.CRITIQUE);

                // Enregistrer l'escalade automatique dans l'historique
                Action action = Action.builder()
                        .type(TypeAction.ESCALADE)
                        .description("Escalade automatique (dépassement du SLA de 5 minutes d'assignation)")
                        .auteurId(0L) // 0L représente le SYSTEME
                        .dateAction(LocalDateTime.now())
                        .ancienStatut(StatutIncident.NOUVEAU)
                        .nouveauStatut(StatutIncident.NOUVEAU)
                        .build();
                incident.addAction(action);

                incidentRepository.save(incident);
                log.info("Incident {} escaladé automatiquement pour dépassement de SLA.", incident.getReference());

                // Notifier les superviseurs (G5) via l'événement d'escalade
                notificationService.envoyerEscalade(incident, "Dépassement du SLA d'assignation de 5 minutes");

            } catch (Exception e) {
                log.error("Erreur lors de l'escalade automatique de l'incident {} : {}", 
                        incident.getReference(), e.getMessage(), e);
            }
        }
    }
}
