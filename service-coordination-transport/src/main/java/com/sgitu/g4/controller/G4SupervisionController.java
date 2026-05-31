package com.sgitu.g4.controller;

import com.sgitu.g4.dto.G4HealthResponse;
import com.sgitu.g4.dto.SupervisionLogEntryResponse;
import com.sgitu.g4.service.SupervisionAggregateService;
import com.sgitu.g4.service.SupervisionLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "G4 — Supervision")
@RestController
@RequestMapping("/api/g4")
@RequiredArgsConstructor
public class G4SupervisionController {

	private final SupervisionAggregateService supervisionAggregateService;
	private final SupervisionLogService supervisionLogService;

	@GetMapping("/health")
	public G4HealthResponse health() {
		return supervisionAggregateService.health();
	}

	@GetMapping("/logs")
	public List<SupervisionLogEntryResponse> logs() {
		return supervisionLogService.recent();
	}
}
