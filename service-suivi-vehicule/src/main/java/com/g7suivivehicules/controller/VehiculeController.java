package com.g7suivivehicules.controller;

import com.g7suivivehicules.dto.VehiculeRequest;
import com.g7suivivehicules.dto.VehiculeResponse;
import com.g7suivivehicules.entity.Vehicule;
import com.g7suivivehicules.service.VehiculeService;
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
@RequestMapping("/api/suivi-vehicules/vehicules")
@RequiredArgsConstructor
@Tag(name = "Gestion des Vehicules", description = "Endpoints pour la gestion de la flotte de vehicules")
public class VehiculeController {

    private final VehiculeService vehiculeService;

    @PostMapping
    @Operation(summary = "Ajouter un nouveau vehicule")
    public ResponseEntity<VehiculeResponse> ajouterVehicule(@Valid @RequestBody VehiculeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehiculeService.createVehicule(request));
    }

    @GetMapping
    @Operation(summary = "Lister tous les vehicules")
    public ResponseEntity<List<VehiculeResponse>> listerTousLesVehicules() {
        return ResponseEntity.ok(vehiculeService.getAllVehicules());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir les details d un vehicule par ID")
    public ResponseEntity<VehiculeResponse> obtenirVehicule(@PathVariable UUID id) {
        return ResponseEntity.ok(vehiculeService.getVehiculeById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier les informations d un vehicule")
    public ResponseEntity<VehiculeResponse> modifierVehicule(@PathVariable UUID id, @Valid @RequestBody VehiculeRequest request) {
        return ResponseEntity.ok(vehiculeService.updateVehicule(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactiver un vehicule (Hors Service)")
    public ResponseEntity<Void> desactiverVehicule(@PathVariable UUID id) {
        vehiculeService.deleteVehicule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/actifs")
    @Operation(summary = "Lister les vehicules actuellement en service")
    public ResponseEntity<List<VehiculeResponse>> listerVehiculesActifs() {
        return ResponseEntity.ok(vehiculeService.getVehiculesActifs());
    }

    @GetMapping("/statut/{statut}")
    @Operation(summary = "Filtrer les vehicules par statut")
    public ResponseEntity<List<VehiculeResponse>> listerParStatut(@PathVariable Vehicule.StatutVehicule statut) {
        return ResponseEntity.ok(vehiculeService.getVehiculesByStatut(statut));
    }

    @PutMapping("/{id}/statut")
    @Operation(summary = "Changer le statut d un vehicule")
    public ResponseEntity<VehiculeResponse> changerStatut(@PathVariable UUID id, @RequestParam Vehicule.StatutVehicule statut) {
        return ResponseEntity.ok(vehiculeService.updateStatut(id, statut));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Filtrer les vehicules par type (BUS, TRAM, etc.)")
    public ResponseEntity<List<VehiculeResponse>> listerParType(@PathVariable Vehicule.TypeVehicule type) {
        return ResponseEntity.ok(vehiculeService.getVehiculesByType(type));
    }
}

