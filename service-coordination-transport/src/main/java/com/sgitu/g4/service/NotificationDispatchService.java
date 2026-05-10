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

	public NotificationSendResponse send(NotificationSendRequest request) {
		NotificationSendResponse response = g5NotificationClient.dispatch(request);
		String level = "ERROR".equals(response.getStatus()) ? "ERROR" : "DEGRADED".equals(response.getStatus()) ? "WARN" : "INFO";
		supervisionLogService.add(level, "NOTIFICATION", request.getEventType() + " -> " + response.getStatus());
		return response;
	}
}
