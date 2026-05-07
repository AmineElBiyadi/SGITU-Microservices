package com.serviceabonnement.controller;

import com.serviceabonnement.service.AbonnementService;
import com.serviceabonnement.dto.response.ErrorResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/abonnements/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Abonnements", description = "Opérations administratives sur les abonnements (ADMIN uniquement)")
public class AdminAbonnementController {

    private final AbonnementService abonnementService;

    @Operation(summary = "Suspendre manuellement un abonnement")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Abonnement suspendu"),
            @ApiResponse(responseCode = "400", description = "Paramètre manquant",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Abonnement non trouvé",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Suspension impossible (statut invalide)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/{id}/suspendre")
    public ResponseEntity<Void> suspendre(
            @PathVariable Long id,
            @Parameter(description = "Motif de la suspension") @RequestParam String motif) {
        abonnementService.suspendre(id, motif);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Forcer l'annulation d'un abonnement")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Annulation effectuée"),
            @ApiResponse(responseCode = "400", description = "Paramètre manquant",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Abonnement non trouvé",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Annulation impossible (statut invalide)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/{id}/forcer-annulation")
    public ResponseEntity<Void> forcerAnnulation(
            @PathVariable Long id,
            @Parameter(description = "Motif de l'annulation") @RequestParam String motif) {
        abonnementService.forcerAnnulation(id, motif);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Forcer le renouvellement d'un abonnement")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Renouvellement forcé initié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Abonnement non trouvé",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "502", description = "Erreur communication service paiement",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/{id}/forcer-renouvellement")
    public ResponseEntity<Void> forcerRenouvellement(@PathVariable Long id) {
        abonnementService.forcerRenouvellement(id);
        return ResponseEntity.ok().build();
    }
}
