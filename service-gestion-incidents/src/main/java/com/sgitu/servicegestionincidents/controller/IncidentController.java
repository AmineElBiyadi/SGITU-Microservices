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
    @PreAuthorize("hasAnyRole('ROLE_PASSENGER', 'ROLE_DRIVER', 'ROLE_DISPATCHER', 'ROLE_SUPERVISOR')")
    @Operation(summary = "Signaler un nouvel incident")
    public ResponseEntity<SignalementResponseDTO> signalerIncident(
            @Valid @RequestBody SignalementRequestDTO request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole) {
        request.setRole(userRole);
        SignalementResponseDTO response = incidentService.signalerIncident(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_PASSENGER', 'ROLE_DRIVER', 'ROLE_TECHNICIAN', 'ROLE_DISPATCHER', 'ROLE_SUPERVISOR', 'ROLE_SECURITY', 'ROLE_MEDIC', 'ROLE_CLEANER')")
    @Operation(summary = "Consulter un incident par ID")
    public ResponseEntity<IncidentResponseDTO> consulterIncident(@PathVariable Long id) {
        IncidentResponseDTO incident = incidentService.consulterIncident(id);
        return ResponseEntity.ok(incident);
    }

    @GetMapping("/{id}/suivi")
    @PreAuthorize("hasAnyRole('ROLE_TECHNICIAN', 'ROLE_DISPATCHER', 'ROLE_SUPERVISOR', 'ROLE_SECURITY', 'ROLE_MEDIC', 'ROLE_CLEANER')")
    @Operation(summary = "Consulter l'historique d'un incident")
    public ResponseEntity<List<ActionDTO>> consulterSuivi(@PathVariable Long id) {
        List<ActionDTO> historique = incidentService.consulterSuivi(id);
        return ResponseEntity.ok(historique);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_TECHNICIAN', 'ROLE_DISPATCHER', 'ROLE_SUPERVISOR', 'ROLE_SECURITY', 'ROLE_MEDIC', 'ROLE_CLEANER')")
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

    @GetMapping("/tous")
    @PreAuthorize("hasRole('ROLE_SUPERVISOR')")
    @Operation(summary = "Récupérer tous les incidents (Superviseur)")
    public ResponseEntity<List<IncidentResponseDTO>> obtenirTousLesIncidents() {
        List<IncidentResponseDTO> incidents = incidentService.obtenirTousLesIncidents();
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/non-escalades")
    @PreAuthorize("hasAnyRole('ROLE_DISPATCHER', 'ROLE_SUPERVISOR')")
    @Operation(summary = "Récupérer les incidents non escaladés")
    public ResponseEntity<List<IncidentResponseDTO>> obtenirIncidentsNonEscalades() {
        List<IncidentResponseDTO> incidents = incidentService.obtenirIncidentsNonEscalades();
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/escalades")
    @PreAuthorize("hasRole('ROLE_SUPERVISOR')")
    @Operation(summary = "Récupérer les incidents escaladés")
    public ResponseEntity<List<IncidentResponseDTO>> obtenirIncidentsEscalades() {
        List<IncidentResponseDTO> incidents = incidentService.obtenirIncidentsEscalades();
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/affectes")
    @PreAuthorize("hasAnyRole('ROLE_TECHNICIAN', 'ROLE_SECURITY', 'ROLE_MEDIC', 'ROLE_CLEANER', 'ROLE_DISPATCHER', 'ROLE_SUPERVISOR')")
    @Operation(summary = "Récupérer les incidents affectés à l'utilisateur actuel (responsable ou renfort)")
    public ResponseEntity<List<IncidentResponseDTO>> obtenirIncidentsAffectes(@RequestHeader("X-User-Id") Long userId) {
        List<IncidentResponseDTO> incidents = incidentService.obtenirIncidentsAffectes(userId);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/demandes-escalades")
    @PreAuthorize("hasAnyRole('ROLE_DISPATCHER', 'ROLE_SUPERVISOR')")
    @Operation(summary = "Récupérer les incidents avec demande d'escalade en attente de validation")
    public ResponseEntity<List<IncidentResponseDTO>> obtenirDemandesEscalades() {
        List<IncidentResponseDTO> incidents = incidentService.obtenirDemandesEscalades();
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/mes-signalements")
    @PreAuthorize("hasAnyRole('ROLE_PASSENGER', 'ROLE_DRIVER', 'ROLE_TECHNICIAN', 'ROLE_DISPATCHER', 'ROLE_SUPERVISOR', 'ROLE_SECURITY', 'ROLE_MEDIC', 'ROLE_CLEANER')")
    @Operation(summary = "Récupérer les incidents signalés par l'utilisateur actuel")
    public ResponseEntity<List<IncidentResponseDTO>> obtenirMesSignalements(@RequestHeader("X-User-Id") Long userId) {
        List<IncidentResponseDTO> incidents = incidentService.obtenirMesSignalements(userId);
        return ResponseEntity.ok(incidents);
    }

    @PutMapping("/{id}/cloturer")
    @PreAuthorize("hasAnyRole('ROLE_DISPATCHER', 'ROLE_SUPERVISOR')")
    @Operation(summary = "Clôturer un incident")
    public ResponseEntity<Void> cloturerIncident(
            @PathVariable Long id,
            @Valid @RequestBody ClotureRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        incidentService.cloturerIncident(id, request.getMotif(), userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/escalader")
    @PreAuthorize("hasAnyRole('ROLE_DISPATCHER', 'ROLE_SUPERVISOR')")
    @Operation(summary = "Escalader un incident critique (ou approuver une demande)")
    public ResponseEntity<Void> escaladerIncident(
            @PathVariable Long id,
            @Valid @RequestBody EscaladeRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        incidentService.escaladerIncident(id, request.getMotif(), userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/demander-escalade")
    @PreAuthorize("hasAnyRole('ROLE_TECHNICIAN', 'ROLE_SECURITY', 'ROLE_MEDIC', 'ROLE_CLEANER')")
    @Operation(summary = "Soumettre une demande d'escalade (Responsable uniquement)")
    public ResponseEntity<Void> demanderEscalade(
            @PathVariable Long id,
            @Valid @RequestBody EscaladeRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        incidentService.demanderEscalade(id, request.getMotif(), userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/refuser-escalade")
    @PreAuthorize("hasAnyRole('ROLE_DISPATCHER', 'ROLE_SUPERVISOR')")
    @Operation(summary = "Refuser une demande d'escalade")
    public ResponseEntity<Void> refuserEscalade(
            @PathVariable Long id,
            @Valid @RequestBody EscaladeRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        incidentService.refuserEscalade(id, request.getMotif(), userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/affecter")
    @PreAuthorize("hasAnyRole('ROLE_DISPATCHER', 'ROLE_SUPERVISOR')")
    @Operation(summary = "Affecter un responsable")
    public ResponseEntity<Void> affecterResponsable(
            @PathVariable Long id,
            @Valid @RequestBody AffectationRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        incidentService.affecterResponsable(id, request.getResponsableId(), userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/renforts/{agentId}")
    @PreAuthorize("hasAnyRole('ROLE_DISPATCHER', 'ROLE_SUPERVISOR')")
    @Operation(summary = "Affecter un renfort (agent supplémentaire) à un incident en cours")
    public ResponseEntity<Void> ajouterRenfort(
            @PathVariable Long id,
            @PathVariable Long agentId,
            @RequestHeader("X-User-Id") Long userId) {
        incidentService.ajouterRenfort(id, agentId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/renforts")
    @PreAuthorize("hasAnyRole('ROLE_TECHNICIAN', 'ROLE_DISPATCHER', 'ROLE_SUPERVISOR', 'ROLE_SECURITY', 'ROLE_MEDIC', 'ROLE_CLEANER')")
    @Operation(summary = "Lister les agents en renfort sur un incident spécifique")
    public ResponseEntity<List<RenfortDTO>> obtenirRenforts(@PathVariable Long id) {
        List<RenfortDTO> renforts = incidentService.obtenirRenforts(id);
        return ResponseEntity.ok(renforts);
    }

    @PutMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ROLE_TECHNICIAN', 'ROLE_DISPATCHER', 'ROLE_SUPERVISOR', 'ROLE_SECURITY', 'ROLE_MEDIC', 'ROLE_CLEANER')")
    @Operation(summary = "Mettre à jour le statut")
    public ResponseEntity<Void> mettreAJourStatut(
            @PathVariable Long id,
            @Valid @RequestBody StatutUpdateRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        incidentService.mettreAJourStatut(id, request.getStatut(), userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('ROLE_DISPATCHER', 'ROLE_SUPERVISOR')")
    @Operation(summary = "Annuler un incident (fausse alerte)")
    public ResponseEntity<Void> annulerIncident(
            @PathVariable Long id,
            @Valid @RequestBody AnnulationRequestDTO request,
            @RequestHeader("X-User-Id") Long userId) {
        incidentService.annulerIncident(id, request.getMotif(), userId);
        return ResponseEntity.noContent().build();
    }
}
