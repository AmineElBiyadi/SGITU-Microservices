package com.sgitu.userservice.service;

import com.sgitu.userservice.security.RedisTokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Background scheduler that flushes locally blacklisted tokens to Redis when it recovers.
 * Part of the Chaos Monkey Redis Fallback system.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSyncScheduler {

    private final RedisTokenBlacklistService blacklistService;
    private final StringRedisTemplate redisTemplate;
    private final ChaosMonkeyService chaosMonkeyService;

    /**
     * Runs every 60 seconds to synchronize local blacklist to Redis.
     */
    @Scheduled(fixedDelayString = "${chaos.scheduler.redis-sync-delay-ms:60000}")
    public void syncLocalBlacklistToRedis() {
        if (chaosMonkeyService.isRedisDown()) {
            log.debug("[REDIS SYNC] Redis is currently simulated as DOWN — skipping sync cycle");
            return;
        }

        ConcurrentHashMap<String, Instant> localBlacklist = blacklistService.getLocalBlacklist();
        if (localBlacklist.isEmpty()) {
            return;
        }

        log.info("[REDIS SYNC] Found {} tokens in local blacklist to sync to Redis...", localBlacklist.size());

        Instant now = Instant.now();
        for (Map.Entry<String, Instant> entry : localBlacklist.entrySet()) {
            String token = entry.getKey();
            Instant expiry = entry.getValue();

            // 1. Clean up if expired
            if (now.isAfter(expiry)) {
                localBlacklist.remove(token);
                log.info("[REDIS SYNC] Removed expired token from local blacklist");
                continue;
            }

            // 2. Try to sync to Redis
            try {
                long remainingTtlSeconds = Duration.between(now, expiry).toSeconds();
                if (remainingTtlSeconds > 0) {
                    redisTemplate.opsForValue().set(token, "revoked", Duration.ofSeconds(remainingTtlSeconds));
                    localBlacklist.remove(token);
                    log.info("[REDIS SYNC] Successfully synced token to Redis and removed from local cache");
                } else {
                    localBlacklist.remove(token);
                }
            } catch (Exception ex) {
                log.warn("[REDIS SYNC] Redis is still unreachable, keeping token in local cache. Error: {}", ex.getMessage());
                // Break because Redis is unreachable
                break;
            }
        }
    }
}
