package com.serviceabonnement.controller;

import com.serviceabonnement.entity.PlanAbonnement;
import com.serviceabonnement.service.PlanAbonnementService;
import com.serviceabonnement.dto.response.ErrorResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
@Tag(name = "Plans d'Abonnement", description = "Administration et consultation des offres d'abonnement")
public class PlanAbonnementController {

    private final PlanAbonnementService planService;

    @Operation(summary = "Lister tous les plans disponibles (paginé)", description = "Accès public.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des plans retournée"),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<Page<PlanAbonnement>> getAllPlans(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(planService.getAllPlans(pageable));
    }

    @Operation(summary = "Récupérer un plan par son ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plan trouvé"),
            @ApiResponse(responseCode = "404", description = "Plan non trouvé",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<PlanAbonnement> getPlan(@PathVariable Long id) {
        return ResponseEntity.ok(planService.getPlanById(id));
    }

    @Operation(summary = "Créer un nouveau plan d'abonnement", description = "Réservé aux ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plan créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Corps de la requête invalide",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<PlanAbonnement> createPlan(@RequestBody PlanAbonnement plan) {
        return ResponseEntity.ok(planService.createPlan(plan));
    }

    @Operation(summary = "Mettre à jour un plan existant", description = "Réservé aux ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plan mis à jour"),
            @ApiResponse(responseCode = "400", description = "Corps de la requête invalide",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Plan non trouvé",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<PlanAbonnement> updatePlan(@PathVariable Long id, @RequestBody PlanAbonnement plan) {
        return ResponseEntity.ok(planService.updatePlan(id, plan));
    }

    @Operation(summary = "Supprimer un plan", description = "Réservé aux ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Plan supprimé"),
            @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Plan non trouvé",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}
