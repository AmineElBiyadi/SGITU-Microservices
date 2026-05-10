package com.sgitu.servicegestionincidents.messaging.producer;

import com.sgitu.servicegestionincidents.messaging.constant.MessagingConstants;
import com.sgitu.servicegestionincidents.messaging.event.IncidentTransportEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransportProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void notifierTransport(IncidentTransportEvent event) {
        try {
            log.info("Publication événement transport (G4) pour incident {}", event.getReferenceIncident());
            kafkaTemplate.send(MessagingConstants.TRANSPORT_TOPIC, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Échec envoi transport G4 (Kafka indisponible): {}", ex.getMessage());
                        } else {
                            log.info("Événement transport publié avec succès");
                        }
                    });
        } catch (Exception e) {
            log.warn("Kafka indisponible — Événement transport G4 non envoyé: {}", e.getMessage());
        }
    }
}
