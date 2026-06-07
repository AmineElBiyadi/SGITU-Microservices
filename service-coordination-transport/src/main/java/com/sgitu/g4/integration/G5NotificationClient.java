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
 * Appel G5 en direct (promo §3) — {@code SGITU_G5_URL} + JWT service G4.
 * Circuit breaker Resilience4j + fallback DEGRADED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class G5NotificationClient {

	private final IntegrationProperties integrationProperties;
	private final G5NotificationWireAdapter wireAdapter;
	private final InterServiceHttpAuth interServiceHttpAuth;

	@CircuitBreaker(name = "g5Notification", fallbackMethod = "dispatchFallback")
	public NotificationSendResponse dispatch(NotificationSendRequest request) {
		try {
			RestClient.RequestBodySpec post = RestClient.create(integrationProperties.getG5BaseUrl())
					.post()
					.uri(integrationProperties.getG5NotificationPath())
					.contentType(MediaType.APPLICATION_JSON);
			post = (RestClient.RequestBodySpec) interServiceHttpAuth.apply(post, InterServiceHttpAuth.Peer.G5);
			post.body(wireAdapter.toWirePayload(request))
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

	public NotificationSendResponse mapHttpError(RestClientResponseException ex) {
		return NotificationSendResponse.builder()
				.status("ERROR")
				.detail(ex.getStatusCode() + " " + ex.getResponseBodyAsString())
				.build();
	}
}
