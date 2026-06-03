package com.sgitu.g4.controller;

import com.sgitu.g4.dto.NotificationSendRequest;
import com.sgitu.g4.dto.NotificationSendResponse;
import com.sgitu.g4.service.G5RecipientBroadcastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

	private final G5RecipientBroadcastService g5RecipientBroadcastService;

	@PostMapping("/send")
	@Operation(summary = "Envoyer une notification vers G5 (via gateway)")
	@ApiResponses({
			@ApiResponse(responseCode = "202", description = "Notification acceptée ou DEGRADED si G5 injoignable"),
			@ApiResponse(responseCode = "401", description = "Non authentifié"),
			@ApiResponse(responseCode = "403", description = "Rôle DISPATCHER requis")
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = """
			{
			  "notificationId": "11111111-1111-4111-8111-111111111111",
			  "sourceService": "COORDINATION",
			  "eventType": "DELAY_ALERT",
			  "channel": "EMAIL",
			  "recipient": { "userId": "42", "email": "dispatcher@campus.fr" },
			  "metadata": {
			    "lineId": "L12",
			    "reason": "RETARD_SIGNIFICATIF",
			    "variables": {"vehiculeId": "00000000-0000-4000-8000-000000000001", "valeur": "12", "arret": "Gare Sud"}
			  }
			}
			""")))
	public ResponseEntity<NotificationSendResponse> send(@Valid @RequestBody NotificationSendRequest request) {
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(g5RecipientBroadcastService.dispatch(request));
	}
}
