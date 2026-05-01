package com.ensate.billetterie.event.publisher;

import com.ensate.billetterie.event.interfaces.EventPublisher;

public class KafkaEventPublisher<T> implements EventPublisher<T> {
    @Override
    public void publish(String topic, T event) {

    }
}
