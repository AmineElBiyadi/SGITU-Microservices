package com.sgitu.g4.integration;

import com.sgitu.g4.config.IntegrationProperties;
import com.sgitu.g4.dto.NotificationSendRequest;
import com.sgitu.g4.dto.NotificationSendResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Appel G5 via G10 — circuit breaker Resilience4j + fallback DEGRADED (Chaos Monkey / prof).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class G5NotificationClient {

	private final IntegrationProperties integrationProperties;
	private final G5NotificationWireAdapter wireAdapter;

	@CircuitBreaker(name = "g5Notification", fallbackMethod = "dispatchFallback")
	public NotificationSendResponse dispatch(NotificationSendRequest request) {
		try {
			RestClient.create(integrationProperties.getG10GatewayUrl())
					.post()
					.uri(integrationProperties.getG5NotificationPath())
					.contentType(MediaType.APPLICATION_JSON)
					.body(wireAdapter.toWirePayload(request))
					.retrieve()
					.toBodilessEntity();
			return NotificationSendResponse.builder()
					.status("ACCEPTED")
					.correlationId(request.effectiveNotificationId())
					.build();
		} catch (RestClientResponseException ex) {
			log.warn("G5 HTTP {} : {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
			return mapHttpError(ex);
		}
	}

	@SuppressWarnings("unused")
	private NotificationSendResponse dispatchFallback(NotificationSendRequest request, Throwable cause) {
		log.warn("G5 fallback (circuit open ou service down): {}", cause.getMessage());
		return NotificationSendResponse.builder()
				.status("DEGRADED")
				.correlationId(request.effectiveNotificationId())
				.detail("Service G5 injoignable — notification mise en attente côté G4")
				.build();
	}

	/** Erreurs HTTP métier G5 (4xx/5xx) — pas de crash applicatif. */
	public NotificationSendResponse mapHttpError(RestClientResponseException ex) {
		return NotificationSendResponse.builder()
				.status("ERROR")
				.detail(ex.getStatusCode() + " " + ex.getResponseBodyAsString())
				.build();
	}
}
