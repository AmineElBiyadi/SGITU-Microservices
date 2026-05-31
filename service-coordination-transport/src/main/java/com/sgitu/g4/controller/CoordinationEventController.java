package com.sgitu.g4.controller;

import com.sgitu.g4.dto.CancelMissionEventRequest;
import com.sgitu.g4.dto.CoordinationEventRequest;
import com.sgitu.g4.dto.CoordinationEventResponse;
import com.sgitu.g4.dto.DetectBreakdownRequest;
import com.sgitu.g4.dto.DetectDelayRequest;
import com.sgitu.g4.dto.DetectDeviationRequest;
import com.sgitu.g4.entity.CoordinationEventStatus;
import com.sgitu.g4.entity.CoordinationEventType;
import com.sgitu.g4.service.CoordinationEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "G4 — Événements coordination")
@RestController
@RequestMapping("/api/g4/events")
@RequiredArgsConstructor
public class CoordinationEventController {

	private final CoordinationEventService coordinationEventService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Créer un événement de coordination")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Événement créé (mission inchangée)"),
			@ApiResponse(responseCode = "404", description = "Mission introuvable")
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = """
			{
			  "type": "RETARD",
			  "status": "SIGNALE",
			  "missionId": 1,
			  "vehiculeId": "VH-001",
			  "description": "Retard signalé manuellement",
			  "occurredAt": "2026-05-20T10:15:00Z"
			}
			""")))
	public CoordinationEventResponse create(@Valid @RequestBody CoordinationEventRequest request) {
		return coordinationEventService.create(request);
	}

	@GetMapping
	public List<CoordinationEventResponse> list() {
		return coordinationEventService.findAll();
	}

	@GetMapping("/{eventId}")
	public CoordinationEventResponse get(@PathVariable Long eventId) {
		return coordinationEventService.findById(eventId);
	}

	@GetMapping("/type/{eventType}")
	public List<CoordinationEventResponse> byType(@PathVariable CoordinationEventType eventType) {
		return coordinationEventService.byType(eventType);
	}

	@GetMapping("/status/{status}")
	public List<CoordinationEventResponse> byStatus(@PathVariable CoordinationEventStatus status) {
		return coordinationEventService.byStatus(status);
	}

	@PostMapping("/detect-delay")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Détecter un retard (n'interrompt pas la mission)")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Événement RETARD créé"),
			@ApiResponse(responseCode = "404", description = "Mission introuvable")
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = """
			{"missionId": 1, "retardMinutes": 12, "cause": "Trafic dense zone centre"}
			""")))
	public CoordinationEventResponse detectDelay(@Valid @RequestBody DetectDelayRequest request) {
		return coordinationEventService.detectDelay(request);
	}

	@PostMapping("/detect-deviation")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Détecter une déviation (n'interrompt pas la mission)")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Événement DEVIATION créé"),
			@ApiResponse(responseCode = "404", description = "Mission introuvable")
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = """
			{"missionId": 1, "ecartMetres": 450, "details": "Écart par rapport au tracé prévu"}
			""")))
	public CoordinationEventResponse detectDeviation(@Valid @RequestBody DetectDeviationRequest request) {
		return coordinationEventService.detectDeviation(request);
	}

	@PostMapping("/detect-breakdown")
	@ResponseStatus(HttpStatus.CREATED)
	public CoordinationEventResponse detectBreakdown(@Valid @RequestBody DetectBreakdownRequest request) {
		return coordinationEventService.detectBreakdown(request);
	}

	@PostMapping("/cancel-mission")
	@ResponseStatus(HttpStatus.CREATED)
	public CoordinationEventResponse cancelMission(@Valid @RequestBody CancelMissionEventRequest request) {
		return coordinationEventService.cancelMissionEvent(request);
	}
}
