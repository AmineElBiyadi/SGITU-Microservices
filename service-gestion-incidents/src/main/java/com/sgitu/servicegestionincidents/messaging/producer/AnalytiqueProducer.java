package com.sgitu.servicegestionincidents.messaging.producer;

import com.sgitu.servicegestionincidents.messaging.constant.MessagingConstants;
import com.sgitu.servicegestionincidents.messaging.event.IncidentAnalytiqueEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalytiqueProducer {

    private final RabbitTemplate rabbitTemplate;

    public void envoyerDonneesAnalytique(IncidentAnalytiqueEvent event) {
        log.info("Publication données analytique pour incident {}", event.getReference());
        rabbitTemplate.convertAndSend(
                MessagingConstants.INCIDENT_EXCHANGE,
                MessagingConstants.ANALYTIQUE_OUT_ROUTING_KEY,
                event
        );
        log.info("Données analytique publiées avec succès");
    }
}
