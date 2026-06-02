package com.sgitu.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sgitu.userservice.exception.ChaosMonkeyException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central service for the Chaos Monkey Challenge.
 * Provides flags to simulate Kafka and Redis outages at runtime.
 * Activated via ChaosMonkeyController (REST / Postman).
 */
@Slf4j
@Service
public class ChaosMonkeyService {

    private final AtomicBoolean kafkaDown = new AtomicBoolean(false);
    private final AtomicBoolean redisDown = new AtomicBoolean(false);

    // ── Kafka chaos ─────────────────────────────────────────────

    public void enableKafkaChaos() {
        kafkaDown.set(true);
        log.warn("CHAOS MONKEY: Kafka is now simulated as DOWN");
    }

    public void disableKafkaChaos() {
        kafkaDown.set(false);
        log.info("CHAOS MONKEY: Kafka simulation OFF — service restored");
    }

    public boolean isKafkaDown() {
        return kafkaDown.get();
    }

    /**
     * Call before any Kafka operation. Throws if Kafka chaos is active.
     */
    public void checkKafka() {
        if (kafkaDown.get()) {
            throw new ChaosMonkeyException("Kafka");
        }
    }

    // ── Redis chaos ─────────────────────────────────────────────

    public void enableRedisChaos() {
        redisDown.set(true);
        log.warn("CHAOS MONKEY: Redis is now simulated as DOWN");
    }

    public void disableRedisChaos() {
        redisDown.set(false);
        log.info("CHAOS MONKEY: Redis simulation OFF — service restored");
    }

    public boolean isRedisDown() {
        return redisDown.get();
    }

    /**
     * Call before any Redis operation. Throws if Redis chaos is active.
     */
    public void checkRedis() {
        if (redisDown.get()) {
            throw new ChaosMonkeyException("Redis");
        }
    }
}
