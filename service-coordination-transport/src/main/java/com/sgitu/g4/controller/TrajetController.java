package com.sgitu.g4.controller;

import com.sgitu.g4.dto.ArretResponse;
import com.sgitu.g4.dto.TrajetRequest;
import com.sgitu.g4.dto.TrajetResponse;
import com.sgitu.g4.service.TrajetService;
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

@Tag(name = "G4 — Trajets")
@RestController
@RequestMapping("/api/g4/trajets")
@RequiredArgsConstructor
public class TrajetController {

	private final TrajetService trajetService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TrajetResponse create(@Valid @RequestBody TrajetRequest request) {
		return trajetService.create(request);
	}

	@GetMapping
	public List<TrajetResponse> list() {
		return trajetService.findAll();
	}

	@GetMapping("/{trajetId}")
	public TrajetResponse get(@PathVariable Long trajetId) {
		return trajetService.findById(trajetId);
	}

	@GetMapping("/{trajetId}/arrets")
	public List<ArretResponse> arrets(@PathVariable Long trajetId) {
		return trajetService.arretsOrdered(trajetId);
	}

	@PutMapping("/{trajetId}")
	public TrajetResponse update(@PathVariable Long trajetId, @Valid @RequestBody TrajetRequest request) {
		return trajetService.update(trajetId, request);
	}

	@DeleteMapping("/{trajetId}")
	public ResponseEntity<Void> delete(@PathVariable Long trajetId) {
		trajetService.delete(trajetId);
		return ResponseEntity.noContent().build();
	}
}
