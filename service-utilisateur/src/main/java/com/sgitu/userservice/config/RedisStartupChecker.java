package com.sgitu.userservice.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

@Component
public class RedisStartupChecker {
    private static final Logger log = LoggerFactory.getLogger(RedisStartupChecker.class);

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @PostConstruct
    public void check() {
        log.info("Effective spring.redis.host='{}' spring.redis.port={}", redisHost, redisPort);
        // quick TCP check (non-blocking short timeout)
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(redisHost, redisPort), 1000);
            log.info("TCP connection to Redis {}:{} succeeded", redisHost, redisPort);
        } catch (IOException e) {
            log.error("TCP connection to Redis {}:{} failed: {}", redisHost, redisPort, e.getMessage());
        }
        // also print system/env for debugging
        log.info("System property spring.redis.host='{}' env SPRING_REDIS_HOST='{}'",
                System.getProperty("spring.redis.host"), System.getenv("SPRING_REDIS_HOST"));
    }
}
