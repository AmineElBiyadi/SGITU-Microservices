package com.sgitu.apigateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${token-blacklist.enabled:false}")
    private boolean blacklistEnabled;

    public Mono<Boolean> isRevoked(String token) {
        if (!blacklistEnabled || token == null || token.isBlank()) {
            return Mono.just(false);
        }

        return redisTemplate.hasKey(token)
                .doOnError(error -> log.warn(
                        "Token blacklist check failed. Falling back to signature validation only: {}",
                        error.getMessage()))
                .onErrorReturn(false);
    }
}
