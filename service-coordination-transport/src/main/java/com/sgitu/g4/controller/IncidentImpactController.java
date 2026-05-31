package com.sgitu.g4.controller;

import com.sgitu.g4.dto.DetectIncidentRequest;
import com.sgitu.g4.dto.IncidentImpactResponse;
import com.sgitu.g4.service.IncidentImpactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@Tag(name = "G4 — Impacts incident G9", description = """
		Lien entre une mission G4 et un incident métier du microservice G9 (référence externe).
		Distinct des événements de coordination (retard, déviation, panne).
		""")
@RestController
@RequestMapping("/api/g4/incident-impacts")
@RequiredArgsConstructor
public class IncidentImpactController {

	private final IncidentImpactService incidentImpactService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Enregistrer l'impact d'un incident G9 sur une mission")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Impact créé"),
			@ApiResponse(responseCode = "404", description = "Mission introuvable si missionId fourni")
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = """
			{
			  "incidentReference": "INC-2026-001",
			  "missionId": 1,
			  "vehiculeId": "VH-001",
			  "resume": "Incident G9 corrélé à la mission en cours"
			}
			""")))
	public IncidentImpactResponse create(@Valid @RequestBody DetectIncidentRequest request) {
		return incidentImpactService.recordFromRequest(request);
	}

	@GetMapping
	public List<IncidentImpactResponse> list() {
		return incidentImpactService.findAll();
	}

	@GetMapping("/{impactId}")
	public IncidentImpactResponse get(@PathVariable Long impactId) {
		return incidentImpactService.findById(impactId);
	}

	@GetMapping("/mission/{missionId}")
	public List<IncidentImpactResponse> byMission(@PathVariable Long missionId) {
		return incidentImpactService.findByMissionId(missionId);
	}
}
