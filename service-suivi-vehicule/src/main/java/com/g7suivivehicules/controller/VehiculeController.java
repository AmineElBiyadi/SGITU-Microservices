package com.g7suivivehicules.controller;

import com.g7suivivehicules.dto.VehiculeRequest;
import com.g7suivivehicules.dto.VehiculeResponse;
import com.g7suivivehicules.entity.Vehicule;
import com.g7suivivehicules.service.VehiculeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Gestion des Véhicules", description = "CRUD de la flotte de véhicules (bus, trams, taxis). Gestion des statuts et affectation aux lignes G4.")
public class VehiculeController {

    private final VehiculeService vehiculeService;

    @PostMapping
    @Operation(summary = "Créer un nouveau véhicule", description = "Enregistre un nouveau véhicule dans la flotte. L'immatriculation doit être unique.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Véhicule créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides (champ manquant ou format incorrect)"),
            @ApiResponse(responseCode = "500", description = "Immatriculation déjà existante (contrainte unique)")
    })
    public ResponseEntity<VehiculeResponse> ajouterVehicule(@Valid @RequestBody VehiculeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehiculeService.createVehicule(request));
    }

    @GetMapping
    @Operation(summary = "Lister tous les véhicules", description = "Retourne la liste complète de la flotte, tous statuts confondus.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournée avec succès")
    })
    public ResponseEntity<List<VehiculeResponse>> listerTousLesVehicules() {
        return ResponseEntity.ok(vehiculeService.getAllVehicules());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un véhicule", description = "Retourne les informations complètes d'un véhicule par son UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Véhicule trouvé"),
            @ApiResponse(responseCode = "404", description = "Véhicule introuvable avec cet ID")
    })
    public ResponseEntity<VehiculeResponse> obtenirVehicule(
            @Parameter(description = "UUID du véhicule", example = "53c31262-591a-44d4-8872-51e84611ac5e")
            @PathVariable UUID id) {
        return ResponseEntity.ok(vehiculeService.getVehiculeById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un véhicule", description = "Met à jour les informations d'un véhicule existant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Véhicule mis à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Véhicule introuvable")
    })
    public ResponseEntity<VehiculeResponse> modifierVehicule(
            @Parameter(description = "UUID du véhicule", example = "53c31262-591a-44d4-8872-51e84611ac5e")
            @PathVariable UUID id,
            @Valid @RequestBody VehiculeRequest request) {
        return ResponseEntity.ok(vehiculeService.updateVehicule(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Désactiver un véhicule", description = "Passe le statut du véhicule à HORS_SERVICE. Opération non-destructive (les données sont conservées).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Véhicule désactivé"),
            @ApiResponse(responseCode = "404", description = "Véhicule introuvable")
    })
    public ResponseEntity<Void> desactiverVehicule(
            @Parameter(description = "UUID du véhicule", example = "53c31262-591a-44d4-8872-51e84611ac5e")
            @PathVariable UUID id) {
        vehiculeService.deleteVehicule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/actifs")
    @Operation(summary = "Véhicules EN_SERVICE", description = "Retourne uniquement les véhicules actuellement en circulation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des véhicules actifs")
    })
    public ResponseEntity<List<VehiculeResponse>> listerVehiculesActifs() {
        return ResponseEntity.ok(vehiculeService.getVehiculesActifs());
    }

    @GetMapping("/statut/{statut}")
    @Operation(summary = "Filtrer par statut", description = "Filtre la flotte par statut : DISPONIBLE, EN_SERVICE, EN_PAUSE, ARRET_PROLONGE, INCIDENT, EN_PANNE, HORS_SERVICE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste filtrée"),
            @ApiResponse(responseCode = "400", description = "Statut inconnu")
    })
    public ResponseEntity<List<VehiculeResponse>> listerParStatut(
            @Parameter(description = "Statut du véhicule", example = "EN_SERVICE")
            @PathVariable Vehicule.StatutVehicule statut) {
        return ResponseEntity.ok(vehiculeService.getVehiculesByStatut(statut));
    }

    @PutMapping("/{id}/statut")
    @Operation(summary = "Changer le statut d'un véhicule", description = "Met à jour le statut opérationnel d'un véhicule. Utilisé par le système IoT ou l'opérateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut mis à jour"),
            @ApiResponse(responseCode = "400", description = "Statut invalide"),
            @ApiResponse(responseCode = "404", description = "Véhicule introuvable")
    })
    public ResponseEntity<VehiculeResponse> changerStatut(
            @Parameter(description = "UUID du véhicule", example = "53c31262-591a-44d4-8872-51e84611ac5e")
            @PathVariable UUID id,
            @Parameter(description = "Nouveau statut", example = "EN_SERVICE")
            @RequestParam Vehicule.StatutVehicule statut) {
        return ResponseEntity.ok(vehiculeService.updateStatut(id, statut));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Filtrer par type", description = "Filtre la flotte par type de véhicule : BUS, TRAM, TAXI, METRO, TRAIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste filtrée"),
            @ApiResponse(responseCode = "400", description = "Type inconnu")
    })
    public ResponseEntity<List<VehiculeResponse>> listerParType(
            @Parameter(description = "Type de véhicule", example = "BUS")
            @PathVariable Vehicule.TypeVehicule type) {
        return ResponseEntity.ok(vehiculeService.getVehiculesByType(type));
    }
}
