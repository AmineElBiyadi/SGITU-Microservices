package com.sgitu.userservice.security;

import com.sgitu.userservice.service.ChaosMonkeyService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for token revocation and blacklist checking.
 *
 * Resilience4j Circuit Breaker handles Redis outages (or simulated DOWN).
 * If Redis is unavailable, falls back to a ConcurrentHashMap in-memory local cache.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final ChaosMonkeyService chaosMonkeyService;

    // Local in-memory fallback cache
    private final ConcurrentHashMap<String, Instant> localBlacklist = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, Instant> getLocalBlacklist() {
        return localBlacklist;
    }

    @CircuitBreaker(name = "redisBlacklist", fallbackMethod = "revokeTokenFallback")
    public void revokeToken(String token, Duration ttl) {
        chaosMonkeyService.checkRedis();
        if (token == null || token.isBlank() || ttl == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(token, "revoked", ttl);
        log.info("Token successfully revoked in Redis");
    }

    public void revokeTokenFallback(String token, Duration ttl, Throwable t) {
        log.warn("Redis DOWN (fallback triggered). Storing revoked token in local in-memory cache. Error: {}", t.getMessage());
        if (token != null && !token.isBlank() && ttl != null && !ttl.isNegative() && !ttl.isZero()) {
            localBlacklist.put(token, Instant.now().plus(ttl));
        }
    }

    @CircuitBreaker(name = "redisBlacklist", fallbackMethod = "isTokenRevokedFallback")
    public boolean isTokenRevoked(String token) {
        chaosMonkeyService.checkRedis();
        if (token == null || token.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(token));
    }

    public boolean isTokenRevokedFallback(String token, Throwable t) {
        log.warn("Redis DOWN (fallback triggered). Checking local in-memory cache for token. Error: {}", t.getMessage());
        if (token == null || token.isBlank()) {
            return false;
        }
        Instant expiry = localBlacklist.get(token);
        if (expiry == null) {
            return false;
        }
        if (Instant.now().isAfter(expiry)) {
            localBlacklist.remove(token);
            return false;
        }
        return true;
    }
}
