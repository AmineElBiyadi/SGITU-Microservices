package com.g7suivivehicules.kafka;

import com.g7suivivehicules.entity.PositionGPS;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.position}")
    private String positionTopic;

    public void envoyerPosition(PositionGPS position) {
        log.info("Envoi position au topic {}: {}", positionTopic, position);
        kafkaTemplate.send(positionTopic, position.getVehiculeId().toString(), position);
    }

    public void envoyerEvenementVehicule(String topic, Object vehicule) {
        log.info("Envoi evenement vehicule au topic {}: {}", topic, vehicule);
        kafkaTemplate.send(topic, vehicule);
    }
}

