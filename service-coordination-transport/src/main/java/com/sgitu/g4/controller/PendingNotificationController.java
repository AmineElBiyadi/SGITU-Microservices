package com.sgitu.g4.controller;

import com.sgitu.g4.dto.PendingNotificationResponse;
import com.sgitu.g4.service.PendingNotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "G4 — Résilience notifications")
@RestController
@RequestMapping("/api/g4/pending-notifications")
@RequiredArgsConstructor
public class PendingNotificationController {

	private final PendingNotificationService pendingNotificationService;

	@GetMapping
	public List<PendingNotificationResponse> listPending() {
		return pendingNotificationService.listPending();
	}

	@PostMapping("/retry")
	public Map<String, Object> retryNow() {
		int sent = pendingNotificationService.retryAllNow();
		return Map.of(
				"retried", sent,
				"remainingPending", pendingNotificationService.countPending()
		);
	}
}
