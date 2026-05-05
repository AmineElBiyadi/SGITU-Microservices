package com.serviceabonnement.producer;

import com.serviceabonnement.dto.AbonnementEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbonnementProducer {

    private final KafkaTemplate<String, AbonnementEvent> kafkaTemplate;

    @Value("${application.topics.souscription}")
    private String souscriptionTopic;

    @Value("${application.topics.rappel}")
    private String rappelTopic;

    @Value("${application.topics.renouvellement}")
    private String renouvellementTopic;

    @Value("${application.topics.annulation}")
    private String annulationTopic;

    @Value("${application.topics.suspension}")
    private String suspensionTopic;

    @Value("${application.topics.desactivation}")
    private String desactivationTopic;

    @Value("${application.topics.modification}")
    private String modificationTopic;

    public void sendSouscriptionEvent(AbonnementEvent event) {
        sendEvent(souscriptionTopic, event);
    }

    public void sendRappelEvent(AbonnementEvent event) {
        sendEvent(rappelTopic, event);
    }

    public void sendRenouvellementEvent(AbonnementEvent event) {
        sendEvent(renouvellementTopic, event);
    }

    public void sendAnnulationEvent(AbonnementEvent event) {
        sendEvent(annulationTopic, event);
    }

    public void sendSuspensionEvent(AbonnementEvent event) {
        sendEvent(suspensionTopic, event);
    }

    public void sendDesactivationEvent(AbonnementEvent event) {
        sendEvent(desactivationTopic, event);
    }

    public void sendModificationEvent(AbonnementEvent event) {
        sendEvent(modificationTopic, event);
    }

    private void sendEvent(String topic, AbonnementEvent event) {
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }
        log.info("Sending event {} to topic {}", event.getType(), topic);
        kafkaTemplate.send(topic, event);
    }
}
