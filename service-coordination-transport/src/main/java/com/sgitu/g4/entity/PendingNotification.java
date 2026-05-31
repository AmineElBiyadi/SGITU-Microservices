package com.sgitu.g4.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "pending_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingNotification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 80)
	private String notificationId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String payloadJson;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private PendingNotificationStatus status;

	@Column(nullable = false)
	private int attemptCount;

	@Column(nullable = false)
	private Instant createdAt;

	private Instant lastAttemptAt;

	@Column(length = 512)
	private String lastError;
}
