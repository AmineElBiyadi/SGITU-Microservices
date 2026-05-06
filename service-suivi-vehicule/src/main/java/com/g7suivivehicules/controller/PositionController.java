package com.g7suivivehicules.controller;

import com.g7suivivehicules.dto.PositionGPSRequest;
import com.g7suivivehicules.dto.PositionGPSResponse;
import com.g7suivivehicules.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/suivi-vehicules/positions")
@RequiredArgsConstructor
@Tag(name = "Positions GPS", description = "Gestion des positions GPS des vehicules")
public class PositionController {

    private final PositionService positionService;

    // ================================
    // POST - Enregistrer une position
    // ================================
    @PostMapping
    @Operation(summary = "Enregistrer une position GPS depuis le capteur IoT")
    public ResponseEntity<PositionGPSResponse> enregistrerPosition(
            @Valid @RequestBody PositionGPSRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(positionService.enregistrerPosition(request));
    }

    // ================================
    // GET - Toutes les positions
    // ================================
    @GetMapping
    @Operation(summary = "Obtenir les positions de tous les vehicules")
    public ResponseEntity<List<PositionGPSResponse>> getToutesLesPositions() {
        return ResponseEntity.ok(positionService.getToutesLesPositions());
    }

    // ================================
    // GET - Position actuelle
    // ================================
    @GetMapping("/{vehiculeId}")
    @Operation(summary = "Obtenir la position actuelle d un vehicule")
    public ResponseEntity<PositionGPSResponse> getPositionActuelle(
            @PathVariable UUID vehiculeId) {
        return ResponseEntity.ok(positionService.getPositionActuelle(vehiculeId));
    }

    // ================================
    // GET - Historique
    // ================================
    @GetMapping("/{vehiculeId}/historique")
    @Operation(summary = "Obtenir l historique des positions d un vehicule")
    public ResponseEntity<List<PositionGPSResponse>> getHistorique(
            @PathVariable UUID vehiculeId) {
        return ResponseEntity.ok(positionService.getHistorique(vehiculeId));
    }

    // ================================
    // GET - Vitesse moyenne
    // ================================
    @GetMapping("/{vehiculeId}/vitesse-moyenne")
    @Operation(summary = "Calculer la vitesse moyenne d un vehicule")
    public ResponseEntity<Double> getVitesseMoyenne(
            @PathVariable UUID vehiculeId) {
        return ResponseEntity.ok(positionService.calculerVitesseMoyenne(vehiculeId));
    }

    // ================================
    // GET - Retard
    // ================================
    @GetMapping("/{vehiculeId}/retard")
    @Operation(summary = "Calculer le retard d un vehicule en secondes")
    public ResponseEntity<Long> getRetard(
            @PathVariable UUID vehiculeId) {
        return ResponseEntity.ok(positionService.calculerRetard(vehiculeId));
    }

    // ================================
    // DELETE - Supprimer historique
    // ================================
    @DeleteMapping("/{vehiculeId}/historique")
    @Operation(summary = "Supprimer l historique des positions d un vehicule")
    public ResponseEntity<Void> supprimerHistorique(
            @PathVariable UUID vehiculeId) {
        positionService.supprimerHistorique(vehiculeId);
        return ResponseEntity.noContent().build();
    }
}