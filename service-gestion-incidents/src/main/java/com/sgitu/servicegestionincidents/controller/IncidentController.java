package com.sgitu.servicegestionincidents.controller;

import com.sgitu.servicegestionincidents.dto.request.*;
import com.sgitu.servicegestionincidents.dto.response.*;
import com.sgitu.servicegestionincidents.model.enums.StatutIncident;
import com.sgitu.servicegestionincidents.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Gestion des Incidents", description = "APIs pour gérer les incidents")
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping("/signaler")
    @PreAuthorize("hasAnyRole('VOYAGEUR', 'CONDUCTEUR', 'SUPERVISEUR_INCIDENTS')")
    @Operation(summary = "Signaler un nouvel incident")
    public ResponseEntity<SignalementResponseDTO> signalerIncident(
            @Valid @RequestBody SignalementRequestDTO request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole) {
        SignalementResponseDTO response = incidentService.signalerIncident(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VOYAGEUR', 'CONDUCTEUR', 'AGENT_INCIDENTS', 'SUPERVISEUR_INCIDENTS')")
    @Operation(summary = "Consulter un incident par ID")
    public ResponseEntity<IncidentResponseDTO> consulterIncident(@PathVariable Long id) {
        IncidentResponseDTO incident = incidentService.consulterIncident(id);
        return ResponseEntity.ok(incident);
    }

    @GetMapping("/{id}/suivi")
    @PreAuthorize("hasAnyRole('AGENT_INCIDENTS', 'SUPERVISEUR_INCIDENTS')")
    @Operation(summary = "Consulter l'historique d'un incident")
    public ResponseEntity<List<ActionDTO>> consulterSuivi(@PathVariable Long id) {
        List<ActionDTO> historique = incidentService.consulterSuivi(id);
        return ResponseEntity.ok(historique);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT_INCIDENTS', 'SUPERVISEUR_INCIDENTS')")
    @Operation(summary = "Filtrer les incidents")
    public ResponseEntity<List<IncidentResponseDTO>> filtrerIncidents(
            @RequestParam(required = false) StatutIncident statut,
            @RequestParam(required = false) Long declarantId) {
        Map<String, Object> criteres = Map.of(
                "statut", statut != null ? statut : "",
                "declarantId", declarantId != null ? declarantId : ""
        );
        List<IncidentResponseDTO> incidents = incidentService.filtrerIncidents(criteres);
        return ResponseEntity.ok(incidents);
    }

    @PutMapping("/{id}/cloturer")
    @PreAuthorize("hasRole('SUPERVISEUR_INCIDENTS')")
    @Operation(summary = "Clôturer un incident")
    public ResponseEntity<Void> cloturerIncident(
            @PathVariable Long id,
            @Valid @RequestBody ClotureRequestDTO request) {
        incidentService.cloturerIncident(id, request.getMotif());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/escalader")
    @PreAuthorize("hasRole('SUPERVISEUR_INCIDENTS')")
    @Operation(summary = "Escalader un incident critique")
    public ResponseEntity<Void> escaladerIncident(
            @PathVariable Long id,
            @Valid @RequestBody EscaladeRequestDTO request) {
        incidentService.escaladerIncident(id, request.getMotif());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/affecter")
    @PreAuthorize("hasRole('SUPERVISEUR_INCIDENTS')")
    @Operation(summary = "Affecter un responsable")
    public ResponseEntity<Void> affecterResponsable(
            @PathVariable Long id,
            @Valid @RequestBody AffectationRequestDTO request) {
        incidentService.affecterResponsable(id, request.getResponsableId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('AGENT_INCIDENTS', 'SUPERVISEUR_INCIDENTS')")
    @Operation(summary = "Mettre à jour le statut")
    public ResponseEntity<Void> mettreAJourStatut(
            @PathVariable Long id,
            @Valid @RequestBody StatutUpdateRequestDTO request) {
        incidentService.mettreAJourStatut(id, request.getStatut());
        return ResponseEntity.noContent().build();
    }
}
