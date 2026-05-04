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
        log.info("Publication données analytique pour incident {}", event.getReference());
        kafkaTemplate.send(MessagingConstants.ANALYTIQUE_OUT_TOPIC, event);
        log.info("Données analytique publiées avec succès");
    }
}
