package com.sgitu.servicegestionincidents.messaging.producer;

import com.sgitu.servicegestionincidents.messaging.constant.MessagingConstants;
import com.sgitu.servicegestionincidents.messaging.event.IncidentTransportEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransportProducer {

    private final RabbitTemplate rabbitTemplate;

    public void notifierTransport(IncidentTransportEvent event) {
        log.info("Publication événement transport pour incident {}", event.getReference());
        rabbitTemplate.convertAndSend(
                MessagingConstants.INCIDENT_EXCHANGE,
                MessagingConstants.TRANSPORT_ROUTING_KEY,
                event
        );
        log.info("Événement transport publié avec succès");
    }
}
