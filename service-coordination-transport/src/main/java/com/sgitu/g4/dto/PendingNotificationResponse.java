package com.sgitu.g4.dto;

import com.sgitu.g4.entity.PendingNotificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PendingNotificationResponse {

	private Long id;
	private String notificationId;
	private PendingNotificationStatus status;
	private int attemptCount;
	private Instant createdAt;
	private Instant lastAttemptAt;
	private String lastError;
}
