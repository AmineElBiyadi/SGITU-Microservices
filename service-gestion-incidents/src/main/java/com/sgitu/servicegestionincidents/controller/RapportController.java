package com.sgitu.servicegestionincidents.controller;

import com.sgitu.servicegestionincidents.dto.response.RapportDTO;
import com.sgitu.servicegestionincidents.service.RapportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/rapports")
@RequiredArgsConstructor
@Tag(name = "Rapports et Statistiques", description = "APIs pour générer des rapports — réservées aux Superviseurs et Dispatchers")
public class RapportController {

    private final RapportService rapportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_SUPERVISOR', 'ROLE_DISPATCHER')")
    @Operation(summary = "Générer un rapport par période",
               description = "Valeurs acceptées pour `periode` : jour, semaine, mois, trimestre, annee. " +
                             "Si non précisé ou inconnu, retourne les statistiques sur l'ensemble des incidents.")
    public ResponseEntity<RapportDTO> genererRapport(
            @RequestParam(defaultValue = "mois") String periode) {
        RapportDTO rapport = rapportService.genererRapport(periode);
        return ResponseEntity.ok(rapport);
    }

    @GetMapping("/tableau-bord")
    @PreAuthorize("hasAnyRole('ROLE_SUPERVISOR', 'ROLE_DISPATCHER')")
    @Operation(summary = "Consulter le tableau de bord en temps réel",
               description = "Retourne les compteurs clés : total, par statut, escaladés, " +
                             "demandes d'escalade en attente et incidents critiques.")
    public ResponseEntity<Map<String, Object>> consulterTableauBord() {
        Map<String, Object> tableauBord = rapportService.genererTableauBord();
        return ResponseEntity.ok(tableauBord);
    }

    @GetMapping("/par-responsable/{responsableId}")
    @PreAuthorize("hasAnyRole('ROLE_SUPERVISOR', 'ROLE_DISPATCHER')")
    @Operation(summary = "Voir les stats d'interventions d'un agent terrain spécifique (combien résolus, escaladés, en cours)",
               description = "Utile pour le Superviseur pour évaluer la charge de travail de chaque intervenant.")
    public ResponseEntity<Map<String, Object>> obtenirStatsParResponsable(
            @PathVariable Long responsableId) {
        Map<String, Object> stats = rapportService.obtenirStatsParResponsable(responsableId);
        return ResponseEntity.ok(stats);
    }
}
