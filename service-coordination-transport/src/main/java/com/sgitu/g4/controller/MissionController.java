package com.sgitu.g4.controller;

import com.sgitu.g4.dto.MissionRequest;
import com.sgitu.g4.dto.MissionResponse;
import com.sgitu.g4.dto.MissionStatusResponse;
import com.sgitu.g4.service.MissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
	@Operation(summary = "Créer une mission")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Mission créée"),
			@ApiResponse(responseCode = "400", description = "Données invalides"),
			@ApiResponse(responseCode = "401", description = "Non authentifié"),
			@ApiResponse(responseCode = "403", description = "Rôle insuffisant (DISPATCHER ou G4_ADMIN requis)"),
			@ApiResponse(responseCode = "404", description = "Ligne, trajet ou affectation introuvable"),
			@ApiResponse(responseCode = "409", description = "Véhicule déjà sur une mission EN_COURS",
					content = @Content(schema = @Schema(example = """
							{"status":409,"error":"CONFLICT","message":"Le véhicule VH-001 est déjà affecté à une mission EN_COURS"}
							""")))
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = """
			{
			  "vehiculeId": "VH-001",
			  "chauffeurId": "42",
			  "ligneId": 1,
			  "trajetId": 1,
			  "affectationId": 1,
			  "statut": "EN_COURS",
			  "plannedStart": "2026-05-20T08:00:00Z",
			  "notes": "Mission régulière matin"
			}
			""")))
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
