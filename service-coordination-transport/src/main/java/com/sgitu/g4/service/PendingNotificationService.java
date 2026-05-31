package com.sgitu.g4.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgitu.g4.dto.NotificationSendRequest;
import com.sgitu.g4.dto.NotificationSendResponse;
import com.sgitu.g4.dto.PendingNotificationResponse;
import com.sgitu.g4.entity.PendingNotification;
import com.sgitu.g4.entity.PendingNotificationStatus;
import com.sgitu.g4.integration.G5NotificationClient;
import com.sgitu.g4.repository.PendingNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * File d'attente locale lorsque G5 est injoignable (Chaos Monkey / prof).
 * Les notifications sont renvoyées automatiquement dès que G5 remonte.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PendingNotificationService {

	private static final int MAX_ATTEMPTS = 5;

	private final PendingNotificationRepository repository;
	private final G5NotificationClient g5NotificationClient;
	private final ObjectMapper objectMapper;
	private final SupervisionLogService supervisionLogService;

	@Transactional
	public PendingNotification enqueue(NotificationSendRequest request) {
		String json = toJson(request);
		PendingNotification pending = PendingNotification.builder()
				.notificationId(request.effectiveNotificationId())
				.payloadJson(json)
				.status(PendingNotificationStatus.PENDING)
				.attemptCount(0)
				.createdAt(Instant.now())
				.build();
		PendingNotification saved = repository.save(pending);
		supervisionLogService.add("WARN", "NOTIFICATION_PENDING",
				"G5 down — notification " + saved.getNotificationId() + " stockée localement");
		return saved;
	}

	public long countPending() {
		return repository.countByStatus(PendingNotificationStatus.PENDING);
	}

	public List<PendingNotificationResponse> listPending() {
		return repository.findTop20ByStatusOrderByCreatedAtAsc(PendingNotificationStatus.PENDING).stream()
				.map(this::toResponse)
				.toList();
	}

	@Scheduled(fixedDelayString = "${app.resilience.pending-notification-retry-ms:30000}")
	@Transactional
	public void retryPending() {
		List<PendingNotification> batch = repository
				.findTop20ByStatusOrderByCreatedAtAsc(PendingNotificationStatus.PENDING);
		if (batch.isEmpty()) {
			return;
		}
		for (PendingNotification pending : batch) {
			retryOne(pending);
		}
	}

	@Transactional
	public int retryAllNow() {
		List<PendingNotification> batch = repository
				.findTop20ByStatusOrderByCreatedAtAsc(PendingNotificationStatus.PENDING);
		int sent = 0;
		for (PendingNotification pending : batch) {
			if (retryOne(pending)) {
				sent++;
			}
		}
		return sent;
	}

	private boolean retryOne(PendingNotification pending) {
		if (pending.getAttemptCount() >= MAX_ATTEMPTS) {
			pending.setStatus(PendingNotificationStatus.FAILED);
			pending.setLastError("Nombre maximal de tentatives atteint");
			repository.save(pending);
			return false;
		}
		NotificationSendRequest request = fromJson(pending.getPayloadJson());
		pending.setAttemptCount(pending.getAttemptCount() + 1);
		pending.setLastAttemptAt(Instant.now());
		NotificationSendResponse response = g5NotificationClient.dispatch(request);
		if ("ACCEPTED".equals(response.getStatus())) {
			pending.setStatus(PendingNotificationStatus.SENT);
			pending.setLastError(null);
			repository.save(pending);
			supervisionLogService.add("INFO", "NOTIFICATION_RETRY",
					pending.getNotificationId() + " -> ACCEPTED");
			log.info("Notification {} renvoyée avec succès vers G5", pending.getNotificationId());
			return true;
		}
		if ("DEGRADED".equals(response.getStatus())) {
			pending.setLastError(response.getDetail());
		} else {
			pending.setLastError(response.getDetail() != null ? response.getDetail() : response.getStatus());
		}
		repository.save(pending);
		return false;
	}

	private String toJson(NotificationSendRequest request) {
		try {
			return objectMapper.writeValueAsString(request);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Sérialisation notification impossible", ex);
		}
	}

	private NotificationSendRequest fromJson(String json) {
		try {
			return objectMapper.readValue(json, NotificationSendRequest.class);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Désérialisation notification impossible", ex);
		}
	}

	private PendingNotificationResponse toResponse(PendingNotification entity) {
		return PendingNotificationResponse.builder()
				.id(entity.getId())
				.notificationId(entity.getNotificationId())
				.status(entity.getStatus())
				.attemptCount(entity.getAttemptCount())
				.createdAt(entity.getCreatedAt())
				.lastAttemptAt(entity.getLastAttemptAt())
				.lastError(entity.getLastError())
				.build();
	}
}
