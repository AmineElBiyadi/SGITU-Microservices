package com.sgitu.g4.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class NotificationSendRequest {

	@NotBlank
	@Size(max = 80)
	private String notificationId;

	@NotBlank
	@Size(max = 64)
	private String sourceService;

	@NotBlank
	@Size(max = 64)
	private String eventType;

	@NotBlank
	@Size(max = 32)
	private String channel;

	@Valid
	@NotNull
	private Recipient recipient;

	@Valid
	@NotNull
	private Metadata metadata;

	public String effectiveNotificationId() {
		return notificationId == null || notificationId.isBlank() ? UUID.randomUUID().toString() : notificationId;
	}

	@Data
	public static class Recipient {
		@NotBlank
		@Size(max = 120)
		private String userId;

		@NotBlank
		@Size(max = 200)
		private String email;
	}

	@Data
	public static class Metadata {
		@NotBlank
		@Size(max = 128)
		private String lineId;

		@NotBlank
		@Size(max = 128)
		private String reason;

		private Map<String, String> variables;
	}
}
