package com.sgitu.userservice.service;

import com.sgitu.userservice.entity.EventStatus;
import com.sgitu.userservice.entity.FailedEvent;
import com.sgitu.userservice.repository.FailedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background scheduler that periodically retries sending failed events stored in the database.
 * Part of the Chaos Monkey Outbox Pattern.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FailedEventRetryScheduler {

    private final FailedEventRepository failedEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ChaosMonkeyService chaosMonkeyService;

    private static final int MAX_RETRIES = 10;

    /**
     * Runs every 30 seconds to retry sending PENDING events.
     */
    @Scheduled(fixedDelayString = "${chaos.scheduler.kafka-retry-delay-ms:30000}")
    public void retryFailedEvents() {
        // If Kafka is currently simulated as DOWN, skip retry to avoid unnecessary attempts
        if (chaosMonkeyService.isKafkaDown()) {
            log.debug("[RETRY SCHEDULER] Kafka is currently simulated as DOWN — skipping retry cycle");
            return;
        }

        List<FailedEvent> pendingEvents = failedEventRepository.findByStatusOrderByCreatedAtAsc(EventStatus.PENDING);
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("[RETRY SCHEDULER] Found {} failed events to retry...", pendingEvents.size());

        for (FailedEvent event : pendingEvents) {
            try {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setLastRetryAt(LocalDateTime.now());

                log.info("[RETRY SCHEDULER] Attempting to resend event id={}, topic={}, type={}, retryCount={}",
                        event.getId(), event.getTopic(), event.getEventType(), event.getRetryCount());

                // Send to Kafka
                kafkaTemplate.send(event.getTopic(), event.getPayload());

                // On success
                event.setStatus(EventStatus.SENT);
                failedEventRepository.save(event);
                log.info("[RETRY SCHEDULER] Successfully resent event id={}", event.getId());

            } catch (Exception ex) {
                log.error("[RETRY SCHEDULER] Failed to resend event id={}: {}", event.getId(), ex.getMessage());
                
                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.setStatus(EventStatus.DEAD_LETTER);
                    log.error("[RETRY SCHEDULER] Event id={} has reached max retries ({}) — marked as DEAD_LETTER", event.getId(), MAX_RETRIES);
                }
                
                failedEventRepository.save(event);
            }
        }
    }
}
