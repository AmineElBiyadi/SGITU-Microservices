package com.ensate.billetterie.event.publisher;

import com.ensate.billetterie.event.interfaces.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher<T> implements EventPublisher<T> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(String topic, T event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, event);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent event=[{}] to topic=[{}] with offset=[{}]", 
                        event.getClass().getSimpleName(), topic, result.getRecordMetadata().offset());
            } else {
                log.error("Unable to send event=[{}] to topic=[{}] due to: {}", 
                        event.getClass().getSimpleName(), topic, ex.getMessage());
            }
        });
    }
}
