package com.sgitu.servicegestionincidents.messaging.producer;

import com.sgitu.servicegestionincidents.messaging.constant.MessagingConstants;
import com.sgitu.servicegestionincidents.messaging.event.IncidentAnalytiqueEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalytiqueProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void envoyerDonneesAnalytique(IncidentAnalytiqueEvent event) {
        try {
            log.info("Publication données analytique pour incident {}", event.getReference());
            kafkaTemplate.send(MessagingConstants.ANALYTIQUE_OUT_TOPIC, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Échec envoi analytique G8 (Kafka indisponible): {}", ex.getMessage());
                        } else {
                            log.info("Données analytique publiées avec succès");
                        }
                    });
        } catch (Exception e) {
            log.warn("Kafka indisponible — Données analytique G8 non envoyées: {}", e.getMessage());
        }
    }
}
