package com.sgitu.g4.controller;

import com.sgitu.g4.dto.NotificationSendRequest;
import com.sgitu.g4.dto.NotificationSendResponse;
import com.sgitu.g4.service.NotificationDispatchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notifications G5")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationDispatchService notificationDispatchService;

	@PostMapping("/send")
	public ResponseEntity<NotificationSendResponse> send(@Valid @RequestBody NotificationSendRequest request) {
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(notificationDispatchService.send(request));
	}
}
