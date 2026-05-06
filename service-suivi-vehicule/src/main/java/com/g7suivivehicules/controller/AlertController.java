package com.g7suivivehicules.controller;

import com.g7suivivehicules.dto.AlertResponseDTO;
import com.g7suivivehicules.dto.AlertStatsDTO;
import com.g7suivivehicules.entity.Alert;
import com.g7suivivehicules.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/suivi-vehicules/alerts")
@RequiredArgsConstructor
@Tag(name = "Alertes", description = "Gestion du cycle de vie des alertes de suivi de véhicule")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "Lister les alertes avec filtres optionnels")
    public ResponseEntity<List<AlertResponseDTO>> listerAlertes(
            @RequestParam(required = false) UUID vehiculeId,
            @RequestParam(required = false) Alert.StatutAlert statut,
            @RequestParam(required = false) Alert.TypeAlert typeAlert) {
        
        List<AlertResponseDTO> responses = alertService.listerAvecFiltres(vehiculeId, statut, typeAlert)
                .stream()
                .map(AlertResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une alerte spécifique")
    public ResponseEntity<AlertResponseDTO> obtenirAlerte(@PathVariable UUID id) {
        return ResponseEntity.ok(AlertResponseDTO.fromEntity(alertService.trouverParId(id)));
    }

    @GetMapping("/active")
    @Operation(summary = "Lister toutes les alertes actuellement OUVERTES")
    public ResponseEntity<List<AlertResponseDTO>> listerAlertesActives() {
        List<AlertResponseDTO> responses = alertService.listerActives()
                .stream()
                .map(AlertResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/vehicule/{vehiculeId}")
    @Operation(summary = "Historique des alertes pour un véhicule donné")
    public ResponseEntity<List<AlertResponseDTO>> listerParVehicule(@PathVariable UUID vehiculeId) {
        List<AlertResponseDTO> responses = alertService.listerParVehicule(vehiculeId)
                .stream()
                .map(AlertResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/vehicule/{vehiculeId}/active")
    @Operation(summary = "Alertes OUVERTES pour un véhicule donné")
    public ResponseEntity<List<AlertResponseDTO>> listerActivesParVehicule(@PathVariable UUID vehiculeId) {
        List<AlertResponseDTO> responses = alertService.listerActivesParVehicule(vehiculeId)
                .stream()
                .map(AlertResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Annuler une alerte (action manuelle opérateur - fausse alerte)")
    public ResponseEntity<AlertResponseDTO> annulerAlerte(@PathVariable UUID id) {
        return ResponseEntity.ok(AlertResponseDTO.fromEntity(alertService.annuler(id)));
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques des alertes pour le Groupe 8")
    public ResponseEntity<AlertStatsDTO> obtenirStats() {
        Map<String, Long> parType = alertService.statsParType().stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]
                ));

        Map<String, Long> parStatut = alertService.statsParStatut().stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]
                ));

        long total = parStatut.values().stream().mapToLong(Long::longValue).sum();

        AlertStatsDTO stats = AlertStatsDTO.builder()
                .parType(parType)
                .parStatut(parStatut)
                .totalAlertes(total)
                .build();

        return ResponseEntity.ok(stats);
    }
}
