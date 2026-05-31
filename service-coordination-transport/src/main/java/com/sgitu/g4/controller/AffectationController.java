package com.sgitu.g4.controller;

import com.sgitu.g4.dto.AffectationRequest;
import com.sgitu.g4.dto.AffectationResponse;
import com.sgitu.g4.service.AffectationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "G4 — Affectations")
@RestController
@RequestMapping("/api/g4/affectations")
@RequiredArgsConstructor
public class AffectationController {

	private final AffectationService affectationService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AffectationResponse create(@Valid @RequestBody AffectationRequest request) {
		return affectationService.create(request);
	}

	@GetMapping
	public List<AffectationResponse> list() {
		return affectationService.findAll();
	}

	@GetMapping("/{affectationId}")
	public AffectationResponse get(@PathVariable Long affectationId) {
		return affectationService.findById(affectationId);
	}

	@GetMapping("/vehicule/{vehiculeId}")
	public List<AffectationResponse> byVehicule(@PathVariable String vehiculeId) {
		return affectationService.byVehicule(vehiculeId);
	}

	@PutMapping("/{affectationId}")
	public AffectationResponse update(@PathVariable Long affectationId, @Valid @RequestBody AffectationRequest request) {
		return affectationService.update(affectationId, request);
	}

	@DeleteMapping("/{affectationId}")
	public ResponseEntity<Void> delete(@PathVariable Long affectationId) {
		affectationService.delete(affectationId);
		return ResponseEntity.noContent().build();
	}
}
