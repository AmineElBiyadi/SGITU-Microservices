package com.sgitu.g4.controller;

import com.sgitu.g4.dto.LigneRequest;
import com.sgitu.g4.dto.LigneResponse;
import com.sgitu.g4.dto.TrajetResponse;
import com.sgitu.g4.service.LigneService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "G4 — Lignes")
@RestController
@RequestMapping("/api/g4/lignes")
@RequiredArgsConstructor
public class LigneController {

	private final LigneService ligneService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Créer une ligne")
	public LigneResponse create(@Valid @RequestBody LigneRequest request) {
		return ligneService.create(request);
	}

	@GetMapping
	public List<LigneResponse> findAll() {
		return ligneService.findAll();
	}

	@GetMapping("/actives")
	public List<LigneResponse> actives() {
		return ligneService.findActives();
	}

	@GetMapping("/{ligneId}")
	public LigneResponse get(@PathVariable Long ligneId) {
		return ligneService.findById(ligneId);
	}

	@PutMapping("/{ligneId}")
	public LigneResponse update(@PathVariable Long ligneId, @Valid @RequestBody LigneRequest request) {
		return ligneService.update(ligneId, request);
	}

	@DeleteMapping("/{ligneId}")
	public ResponseEntity<Void> delete(@PathVariable Long ligneId) {
		ligneService.delete(ligneId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{ligneId}/trajets")
	public List<TrajetResponse> trajets(@PathVariable Long ligneId) {
		return ligneService.trajetsForLigne(ligneId);
	}
}
