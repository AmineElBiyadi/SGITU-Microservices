package com.sgitu.g4.controller;

import com.sgitu.g4.dto.CancelMissionEventRequest;
import com.sgitu.g4.dto.CoordinationEventRequest;
import com.sgitu.g4.dto.CoordinationEventResponse;
import com.sgitu.g4.dto.DetectBreakdownRequest;
import com.sgitu.g4.dto.DetectDelayRequest;
import com.sgitu.g4.dto.DetectDeviationRequest;
import com.sgitu.g4.dto.DetectIncidentRequest;
import com.sgitu.g4.entity.CoordinationEventStatus;
import com.sgitu.g4.entity.CoordinationEventType;
import com.sgitu.g4.service.CoordinationEventService;
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
	public CoordinationEventResponse detectDelay(@Valid @RequestBody DetectDelayRequest request) {
		return coordinationEventService.detectDelay(request);
	}

	@PostMapping("/detect-deviation")
	@ResponseStatus(HttpStatus.CREATED)
	public CoordinationEventResponse detectDeviation(@Valid @RequestBody DetectDeviationRequest request) {
		return coordinationEventService.detectDeviation(request);
	}

	@PostMapping("/detect-breakdown")
	@ResponseStatus(HttpStatus.CREATED)
	public CoordinationEventResponse detectBreakdown(@Valid @RequestBody DetectBreakdownRequest request) {
		return coordinationEventService.detectBreakdown(request);
	}

	@PostMapping("/detect-incident")
	@ResponseStatus(HttpStatus.CREATED)
	public CoordinationEventResponse detectIncident(@Valid @RequestBody DetectIncidentRequest request) {
		return coordinationEventService.detectIncident(request);
	}

	@PostMapping("/cancel-mission")
	@ResponseStatus(HttpStatus.CREATED)
	public CoordinationEventResponse cancelMission(@Valid @RequestBody CancelMissionEventRequest request) {
		return coordinationEventService.cancelMissionEvent(request);
	}
}
