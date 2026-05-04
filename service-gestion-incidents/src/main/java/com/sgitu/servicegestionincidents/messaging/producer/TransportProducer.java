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
        log.info("Publication événement transport pour incident {}", event.getReference());
        kafkaTemplate.send(MessagingConstants.TRANSPORT_TOPIC, event);
        log.info("Événement transport publié avec succès");
    }
}
