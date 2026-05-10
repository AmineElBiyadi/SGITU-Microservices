package com.sgitu.g4.controller;

import com.sgitu.g4.dto.HoraireRequest;
import com.sgitu.g4.dto.HoraireResponse;
import com.sgitu.g4.service.HoraireService;
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

@Tag(name = "G4 — Horaires")
@RestController
@RequestMapping("/api/g4/horaires")
@RequiredArgsConstructor
public class HoraireController {

	private final HoraireService horaireService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public HoraireResponse create(@Valid @RequestBody HoraireRequest request) {
		return horaireService.create(request);
	}

	@GetMapping
	public List<HoraireResponse> list() {
		return horaireService.findAll();
	}

	@GetMapping("/{horaireId}")
	public HoraireResponse get(@PathVariable Long horaireId) {
		return horaireService.findById(horaireId);
	}

	@PutMapping("/{horaireId}")
	public HoraireResponse update(@PathVariable Long horaireId, @Valid @RequestBody HoraireRequest request) {
		return horaireService.update(horaireId, request);
	}

	@DeleteMapping("/{horaireId}")
	public ResponseEntity<Void> delete(@PathVariable Long horaireId) {
		horaireService.delete(horaireId);
		return ResponseEntity.noContent().build();
	}
}
