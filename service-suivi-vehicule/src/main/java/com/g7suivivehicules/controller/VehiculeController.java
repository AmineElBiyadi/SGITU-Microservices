package com.g7suivivehicules.controller;

import com.g7suivivehicules.entity.Vehicule;
import com.g7suivivehicules.repository.VehiculeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/suivi-vehicules/vehicules")
@RequiredArgsConstructor
@Tag(name = "Véhicules", description = "Gestion de la flotte de véhicules")
public class VehiculeController {

    private final VehiculeRepository vehiculeRepository;

    @PostMapping
    @Operation(summary = "Ajouter un véhicule")
    public ResponseEntity<Vehicule> ajouterVehicule(@RequestBody Vehicule vehicule) {
        return ResponseEntity.ok(vehiculeRepository.save(vehicule));
    }

    @GetMapping
    @Operation(summary = "Lister tous les véhicules")
    public ResponseEntity<List<Vehicule>> listerVehicules() {
        return ResponseEntity.ok(vehiculeRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un véhicule")
    public ResponseEntity<Vehicule> obtenirVehicule(@PathVariable UUID id) {
        return vehiculeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un véhicule")
    public ResponseEntity<Vehicule> modifierVehicule(@PathVariable UUID id, @RequestBody Vehicule vehicule) {
        if (!vehiculeRepository.existsById(id)) return ResponseEntity.notFound().build();
        vehicule.setId(id);
        return ResponseEntity.ok(vehiculeRepository.save(vehicule));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Désactiver un véhicule")
    public ResponseEntity<Void> supprimerVehicule(@PathVariable UUID id) {
        if (!vehiculeRepository.existsById(id)) return ResponseEntity.notFound().build();
        vehiculeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
