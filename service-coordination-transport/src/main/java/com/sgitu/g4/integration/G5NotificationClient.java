package com.sgitu.g4.integration;

import com.sgitu.g4.config.IntegrationProperties;
import com.sgitu.g4.dto.NotificationSendRequest;
import com.sgitu.g4.dto.NotificationSendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class G5NotificationClient {

	private final IntegrationProperties integrationProperties;

	public NotificationSendResponse dispatch(NotificationSendRequest request) {
		try {
			RestClient.create(integrationProperties.getG10GatewayUrl())
					.post()
					.uri(integrationProperties.getG5NotificationPath())
					.contentType(MediaType.APPLICATION_JSON)
					.body(request)
					.retrieve()
					.toBodilessEntity();
			return NotificationSendResponse.builder()
					.status("ACCEPTED")
					.correlationId(request.effectiveNotificationId())
					.build();
		} catch (RestClientResponseException ex) {
			log.warn("G5 HTTP {} : {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
			return NotificationSendResponse.builder()
					.status("ERROR")
					.detail(ex.getStatusCode() + " " + ex.getResponseBodyAsString())
					.build();
		} catch (ResourceAccessException ex) {
			return NotificationSendResponse.builder()
					.status("DEGRADED")
					.detail("Service G5 injoignable")
					.build();
		} catch (Exception ex) {
			return NotificationSendResponse.builder().status("ERROR").detail(ex.getMessage()).build();
		}
	}
}
