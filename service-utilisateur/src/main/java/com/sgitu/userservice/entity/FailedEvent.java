package com.sgitu.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Outbox table for Kafka events that failed to send.
 * The FailedEventRetryScheduler periodically retries PENDING events.
 * After {@code MAX_RETRIES}, events move to DEAD_LETTER.
 */
@Entity
@Table(name = "failed_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kafka topic name (e.g. "user-events", "g8-user-events"). */
    @Column(nullable = false)
    private String topic;

    /** Full JSON payload to resend. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    /** Human-readable event type (WELCOME, ACCOUNT_DEACTIVATED, USER_STATUS, etc.). */
    @Column(nullable = false)
    private String eventType;

    /** Current status in the retry lifecycle. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    /** When the event was first persisted. */
    private LocalDateTime createdAt;

    /** When the last retry attempt occurred. */
    private LocalDateTime lastRetryAt;

    /** Number of retry attempts so far. */
    @Builder.Default
    private int retryCount = 0;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = EventStatus.PENDING;
    }
}
