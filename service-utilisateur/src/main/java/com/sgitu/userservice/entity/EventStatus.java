package com.sgitu.userservice.entity;

/**
 * Status lifecycle for failed Kafka events (Outbox Pattern).
 * PENDING → SENT (on successful retry) or DEAD_LETTER (after max retries).
 */
public enum EventStatus {
    PENDING,
    SENT,
    DEAD_LETTER
}
