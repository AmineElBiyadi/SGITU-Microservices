package com.sgitu.userservice.controller;

import com.sgitu.userservice.entity.EventStatus;
import com.sgitu.userservice.entity.FailedEvent;
import com.sgitu.userservice.repository.FailedEventRepository;
import com.sgitu.userservice.security.RedisTokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight REST controller for monitoring the resilience status.
 * Exposes outbox statistics and local blacklist size for observability.
 *
 * Chaos Monkey control is handled by the standard /actuator/chaosmonkey/** endpoints.
 */
@RestController
@RequestMapping("/resilience")
@RequiredArgsConstructor
@Tag(name = "Resilience Monitoring", description = "Endpoints de monitoring de la résilience — Outbox, Circuit Breakers, Cache local")
public class ResilienceStatusController {

    private final FailedEventRepository failedEventRepository;
    private final RedisTokenBlacklistService redisTokenBlacklistService;

    @Operation(summary = "Obtenir l'état de la résilience (outbox, cache local)")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();

        // Failed events statistics (Kafka Outbox)
        Map<String, Object> failedEventsStats = new HashMap<>();
        failedEventsStats.put("pending", failedEventRepository.countByStatus(EventStatus.PENDING));
        failedEventsStats.put("sent", failedEventRepository.countByStatus(EventStatus.SENT));
        failedEventsStats.put("deadLetter", failedEventRepository.countByStatus(EventStatus.DEAD_LETTER));
        status.put("failedEvents", failedEventsStats);

        // Redis local fallback cache size
        status.put("localBlacklistSize", redisTokenBlacklistService.getLocalBlacklist().size());

        // Hint about Chaos Monkey control endpoint
        status.put("chaosMonkeyEndpoint", "/actuator/chaosmonkey");

        return ResponseEntity.ok(status);
    }

    @Operation(summary = "Lister les events Kafka échoués (Outbox)")
    @GetMapping("/failed-events")
    public ResponseEntity<List<FailedEvent>> getFailedEvents() {
        return ResponseEntity.ok(failedEventRepository.findAll());
    }
}
