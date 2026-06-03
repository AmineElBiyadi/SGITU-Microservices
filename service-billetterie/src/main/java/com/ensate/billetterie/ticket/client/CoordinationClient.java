package com.ensate.billetterie.ticket.client;

import com.ensate.billetterie.ticket.dto.response.MissionDTO;
import com.ensate.billetterie.validation.exceptions.ValidationException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Client for calling the Coordination microservice using RestTemplate.
 * Securely managed by Resilience4j (Circuit Breaker, Retry, Rate Limiting).
 */
@Slf4j
@Component
public class CoordinationClient {

    private final RestTemplate restTemplate;
    private final String coordinationServiceUrl;

    public CoordinationClient(RestTemplate restTemplate,
                               @Value("${coordination-service.url}") String coordinationServiceUrl) {
        this.restTemplate = restTemplate;
        this.coordinationServiceUrl = coordinationServiceUrl;
    }

    /**
     * Retrieve a mission by its ID from the Coordination microservice.
     * <p>
     * Securely managed by Resilience4j.
     * Fail-closed strategy: if the Coordination Service is unreachable,
     * ticket validation is blocked (safer than letting anyone through).
     */
    @CircuitBreaker(name = "coordinationService", fallbackMethod = "getMissionFallback")
    @Retry(name = "coordinationService")
    @RateLimiter(name = "coordinationService", fallbackMethod = "getMissionRateLimitFallback")
    public MissionDTO getMission(String missionId) {
        String url = UriComponentsBuilder.fromUriString(coordinationServiceUrl)
                .pathSegment("missions", missionId)
                .toUriString();

        try {
            return restTemplate.getForObject(url, MissionDTO.class);

        } catch (HttpClientErrorException.NotFound ex) {
            throw new ValidationException(
                    "EventActiveStep",
                    "Mission introuvable : " + missionId
            );

        } catch (RestClientException ex) {
            throw new ValidationException(
                    "EventActiveStep",
                    "Service de coordination indisponible — validation bloquée par sécurité",
                    ex
            );
        }
    }

    // ==========================================
    //            METHODES DE FALLBACK (Fail-Closed)
    // ==========================================

    /**
     * Fallback triggered when the Circuit Breaker is open or on general network failure.
     */
    public MissionDTO getMissionFallback(String missionId, Throwable t) {
        log.error("Coordination fallback triggered due to: {}", t.getMessage());
        throw new ValidationException(
                "EventActiveStep",
                "Validation suspendue par sécurité : Le service de coordination est momentanément indisponible (Circuit Breaker ouvert / Erreur réseau).",
                t
        );
    }

    /**
     * Fallback triggered when the Rate Limiter limit is exceeded.
     */
    public MissionDTO getMissionRateLimitFallback(String missionId, Throwable t) {
        log.error("Coordination rate limit exceeded: {}", t.getMessage());
        throw new ValidationException(
                "EventActiveStep",
                "Trop de requêtes de validation en cours sur le service de coordination. Veuillez réessayer dans quelques instants.",
                t
        );
    }
}
