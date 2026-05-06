package com.g7suivivehicules.kafka;

import com.g7suivivehicules.entity.PositionGPS;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.position}")
    private String positionTopic;

    public void envoyerPosition(PositionGPS position) {
        if (kafkaTemplate != null) {
            log.info("Envoi position au topic {}: {}", positionTopic, position);
            kafkaTemplate.send(positionTopic, position.getVehiculeId().toString(), position);
        } else {
            log.warn("Kafka non disponible. Position non envoyée.");
        }
    }

    public void envoyerEvenementVehicule(String topic, Object vehicule) {
        if (kafkaTemplate != null) {
            log.info("Envoi evenement vehicule au topic {}: {}", topic, vehicule);
            kafkaTemplate.send(topic, vehicule);
        } else {
            log.warn("Kafka non disponible. Evenement non envoye.");
        }
    }

}

