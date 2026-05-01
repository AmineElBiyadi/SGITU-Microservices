package com.ensate.billetterie.event.interfaces;

public interface EventPublisher<T> {
    void publish(String topic, T event);
}
