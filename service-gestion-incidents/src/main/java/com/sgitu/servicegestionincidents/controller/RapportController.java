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
@Tag(name = "Rapports et Statistiques", description = "APIs pour gÃ©nÃ©rer des rapports â€” rÃ©servÃ©es aux Superviseurs et Dispatchers")
public class RapportController {

    private final RapportService rapportService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR', 'ROLE_DISPATCHER')")
    @Operation(summary = "GÃ©nÃ©rer un rapport par pÃ©riode",
               description = "Valeurs acceptÃ©es pour `periode` : jour, semaine, mois, trimestre, annee. " +
                             "Si non prÃ©cisÃ© ou inconnu, retourne les statistiques sur l'ensemble des incidents.")
    public ResponseEntity<RapportDTO> genererRapport(
            @RequestParam(defaultValue = "mois") String periode) {
        RapportDTO rapport = rapportService.genererRapport(periode);
        return ResponseEntity.ok(rapport);
    }

    @GetMapping("/tableau-bord")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR', 'ROLE_DISPATCHER')")
    @Operation(summary = "Consulter le tableau de bord en temps rÃ©el",
               description = "Retourne les compteurs clÃ©s : total, par statut, escaladÃ©s, " +
                             "demandes d'escalade en attente et incidents critiques.")
    public ResponseEntity<Map<String, Object>> consulterTableauBord() {
        Map<String, Object> tableauBord = rapportService.genererTableauBord();
        return ResponseEntity.ok(tableauBord);
    }

    @GetMapping("/par-responsable/{responsableId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR', 'ROLE_DISPATCHER')")
    @Operation(summary = "Voir les stats d'interventions d'un agent terrain spÃ©cifique (combien rÃ©solus, escaladÃ©s, en cours)",
               description = "Utile pour le Superviseur pour Ã©valuer la charge de travail de chaque intervenant.")
    public ResponseEntity<Map<String, Object>> obtenirStatsParResponsable(
            @PathVariable Long responsableId) {
        Map<String, Object> stats = rapportService.obtenirStatsParResponsable(responsableId);
        return ResponseEntity.ok(stats);
    }
}
