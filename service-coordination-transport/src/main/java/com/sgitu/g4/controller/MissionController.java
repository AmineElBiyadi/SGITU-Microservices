package com.sgitu.g4.controller;

import com.sgitu.g4.dto.MissionRequest;
import com.sgitu.g4.dto.MissionResponse;
import com.sgitu.g4.dto.MissionStatusResponse;
import com.sgitu.g4.service.MissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "G4 — Missions")
@RestController
@RequestMapping("/api/g4/missions")
@RequiredArgsConstructor
public class MissionController {

	private final MissionService missionService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MissionResponse create(@Valid @RequestBody MissionRequest request) {
		return missionService.create(request);
	}

	@GetMapping
	public List<MissionResponse> list() {
		return missionService.findAll();
	}

	@GetMapping("/actives")
	public List<MissionResponse> actives() {
		return missionService.findActives();
	}

	@GetMapping("/{missionId}")
	public MissionResponse get(@PathVariable Long missionId) {
		return missionService.findById(missionId);
	}

	@GetMapping("/{missionId}/status")
	public MissionStatusResponse status(@PathVariable Long missionId) {
		return missionService.status(missionId);
	}

	@PutMapping("/{missionId}")
	public MissionResponse update(@PathVariable Long missionId, @Valid @RequestBody MissionRequest request) {
		return missionService.update(missionId, request);
	}

	@PostMapping("/{missionId}/cloturer")
	public MissionResponse cloturer(@PathVariable Long missionId) {
		return missionService.cloturer(missionId);
	}

	@PostMapping("/{missionId}/annuler")
	public MissionResponse annuler(@PathVariable Long missionId) {
		return missionService.annuler(missionId);
	}
}
