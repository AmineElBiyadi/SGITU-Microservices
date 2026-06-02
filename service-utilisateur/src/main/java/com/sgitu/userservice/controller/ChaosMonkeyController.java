package com.sgitu.userservice.controller;

import com.sgitu.userservice.entity.EventStatus;
import com.sgitu.userservice.entity.FailedEvent;
import com.sgitu.userservice.repository.FailedEventRepository;
import com.sgitu.userservice.security.RedisTokenBlacklistService;
import com.sgitu.userservice.service.ChaosMonkeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for simulating and monitoring outages (Postman-only, zero frontend).
 * Part of the Chaos Monkey Challenge.
 */
@RestController
@RequestMapping("/chaos")
@RequiredArgsConstructor
@Tag(name = "Chaos Monkey", description = "Endpoints de simulation de pannes pour le challenge de résilience")
public class ChaosMonkeyController {

    private final ChaosMonkeyService chaosMonkeyService;
    private final FailedEventRepository failedEventRepository;
    private final RedisTokenBlacklistService redisTokenBlacklistService;

    @Operation(summary = "Simuler une panne de Kafka")
    @PostMapping("/kafka/down")
    public ResponseEntity<Map<String, Object>> kafkaDown() {
        chaosMonkeyService.enableKafkaChaos();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Panne de Kafka simulée avec succès");
        response.put("kafkaSimulatedStatus", "DOWN");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Rétablir le fonctionnement normal de Kafka")
    @PostMapping("/kafka/up")
    public ResponseEntity<Map<String, Object>> kafkaUp() {
        chaosMonkeyService.disableKafkaChaos();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Fonctionnement normal de Kafka rétabli");
        response.put("kafkaSimulatedStatus", "UP");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Simuler une panne de Redis")
    @PostMapping("/redis/down")
    public ResponseEntity<Map<String, Object>> redisDown() {
        chaosMonkeyService.enableRedisChaos();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Panne de Redis simulée avec succès");
        response.put("redisSimulatedStatus", "DOWN");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Rétablir le fonctionnement normal de Redis")
    @PostMapping("/redis/up")
    public ResponseEntity<Map<String, Object>> redisUp() {
        chaosMonkeyService.disableRedisChaos();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Fonctionnement normal de Redis rétabli");
        response.put("redisSimulatedStatus", "UP");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtenir l'état de la résilience et du chaos")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();

        Map<String, Object> kafkaStatus = new HashMap<>();
        kafkaStatus.put("simulatedChaos", chaosMonkeyService.isKafkaDown());
        kafkaStatus.put("status", chaosMonkeyService.isKafkaDown() ? "DOWN" : "UP");
        status.put("kafka", kafkaStatus);

        Map<String, Object> redisStatus = new HashMap<>();
        redisStatus.put("simulatedChaos", chaosMonkeyService.isRedisDown());
        redisStatus.put("status", chaosMonkeyService.isRedisDown() ? "DOWN" : "UP");
        status.put("redis", redisStatus);

        Map<String, Object> failedEventsStats = new HashMap<>();
        failedEventsStats.put("pending", failedEventRepository.countByStatus(EventStatus.PENDING));
        failedEventsStats.put("sent", failedEventRepository.countByStatus(EventStatus.SENT));
        failedEventsStats.put("deadLetter", failedEventRepository.countByStatus(EventStatus.DEAD_LETTER));
        status.put("failedEvents", failedEventsStats);

        status.put("localBlacklistSize", redisTokenBlacklistService.getLocalBlacklist().size());

        return ResponseEntity.ok(status);
    }

    @Operation(summary = "Lister les events Kafka échoués (Outbox)")
    @GetMapping("/failed-events")
    public ResponseEntity<List<FailedEvent>> getFailedEvents() {
        return ResponseEntity.ok(failedEventRepository.findAll());
    }
}
