package com.sgitu.g4.service;

import com.sgitu.g4.dto.NotificationSendRequest;
import com.sgitu.g4.dto.NotificationSendResponse;
import com.sgitu.g4.integration.G5NotificationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

	private final G5NotificationClient g5NotificationClient;
	private final SupervisionLogService supervisionLogService;
	private final PendingNotificationService pendingNotificationService;

	public NotificationSendResponse send(NotificationSendRequest request) {
		NotificationSendResponse response = g5NotificationClient.dispatch(request);
		if ("DEGRADED".equals(response.getStatus())) {
			pendingNotificationService.enqueue(request);
			response = NotificationSendResponse.builder()
					.status("DEGRADED")
					.correlationId(request.effectiveNotificationId())
					.detail("Service G5 injoignable — notification stockée localement (PENDING), renvoi automatique")
					.build();
		}
		String level = "ERROR".equals(response.getStatus()) ? "ERROR"
				: "DEGRADED".equals(response.getStatus()) ? "WARN" : "INFO";
		supervisionLogService.add(level, "NOTIFICATION", request.getEventType() + " -> " + response.getStatus());
		return response;
	}
}
