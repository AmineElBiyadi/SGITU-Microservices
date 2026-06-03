package com.sgitu.g4.controller;

import com.sgitu.g4.dto.VehiculeReferentielResponse;
import com.sgitu.g4.service.VehiculeReferentielService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "G4 — Référentiel véhicules G7")
@RestController
@RequestMapping("/api/g4/vehicules")
@RequiredArgsConstructor
public class VehiculeReferentielController {

	private final VehiculeReferentielService vehiculeReferentielService;

	@GetMapping
	@Operation(summary = "Lister les véhicules connus (référentiel G4 alimenté par G7)")
	public List<VehiculeReferentielResponse> list() {
		return vehiculeReferentielService.findAll();
	}

	@GetMapping("/disponibles")
	@Operation(summary = "Véhicules DISPONIBLE prêts pour affectation à une ligne G4")
	public List<VehiculeReferentielResponse> disponibles() {
		return vehiculeReferentielService.listDisponiblesPourAffectation();
	}

	@GetMapping("/{vehiculeId}")
	public VehiculeReferentielResponse get(@PathVariable String vehiculeId) {
		return vehiculeReferentielService.findById(vehiculeId);
	}

	@PostMapping("/sync-from-g7/{vehiculeId}")
	@Operation(summary = "Synchroniser le référentiel G4 depuis G7 (secours si Kafka indisponible)")
	public VehiculeReferentielResponse syncFromG7(@PathVariable String vehiculeId) {
		return vehiculeReferentielService.syncFromG7(vehiculeId);
	}
}
