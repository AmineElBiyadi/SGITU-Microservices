package com.sgitu.g4.controller;

import com.sgitu.g4.dto.ArretRequest;
import com.sgitu.g4.dto.ArretResponse;
import com.sgitu.g4.service.ArretService;
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

@Tag(name = "G4 — Arrêts")
@RestController
@RequestMapping("/api/g4/arrets")
@RequiredArgsConstructor
public class ArretController {

	private final ArretService arretService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ArretResponse create(@Valid @RequestBody ArretRequest request) {
		return arretService.create(request);
	}

	@GetMapping
	public List<ArretResponse> list() {
		return arretService.findAll();
	}

	@GetMapping("/{arretId}")
	public ArretResponse get(@PathVariable Long arretId) {
		return arretService.findById(arretId);
	}

	@PutMapping("/{arretId}")
	public ArretResponse update(@PathVariable Long arretId, @Valid @RequestBody ArretRequest request) {
		return arretService.update(arretId, request);
	}

	@DeleteMapping("/{arretId}")
	public ResponseEntity<Void> delete(@PathVariable Long arretId) {
		arretService.delete(arretId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/ligne/{ligneId}")
	public List<ArretResponse> byLigne(@PathVariable Long ligneId) {
		return arretService.findByLigne(ligneId);
	}
}
