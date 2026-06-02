package com.serviceabonnement.controller;

import com.serviceabonnement.entity.Abonnement;
import com.serviceabonnement.entity.Renouvellement;
import com.serviceabonnement.service.AbonnementService;
import com.serviceabonnement.config.JwtUtils;
import com.serviceabonnement.dto.response.ErrorResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.serviceabonnement.dto.external.ActiveSubscriptionResponseDTO;
import com.serviceabonnement.dto.external.PaymentCallbackDTO;
import com.serviceabonnement.dto.external.RefundCallbackDTO;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/abonnements")
@RequiredArgsConstructor
@Tag(name = "Abonnements", description = "Gestion du cycle de vie des abonnements pour les utilisateurs")
public class AbonnementController {

    private final AbonnementService abonnementService;
    private final JwtUtils jwtUtils;

    @Operation(summary = "Souscrire à un nouvel abonnement")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Abonnement créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Paramètre manquant ou invalide", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Plan ou Utilisateur non trouvé", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "502", description = "Erreur communication service paiement", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/souscrire")
    public ResponseEntity<Abonnement> souscrire(
            @Parameter(description = "ID de l'utilisateur") @RequestParam Long userId,
            @Parameter(description = "ID du plan d'abonnement") @RequestParam Long planId,
            HttpServletRequest request) {
        // Extract JWT from Authorization header
        String authHeader = request.getHeader("Authorization");
        String email = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            try {
                email = jwtUtils.extractEmail(jwt);
            } catch (Exception e) {
                email = null;
            }
        }
        
        return new ResponseEntity<>(abonnementService.souscrire(userId, planId, email), HttpStatus.CREATED);
    }

    @Operation(summary = "Récupérer un abonnement par ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Abonnement trouvé"),
            @ApiResponse(responseCode = "404", description = "Abonnement non trouvé", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Abonnement> getAbonnement(@PathVariable Long id) {
        return ResponseEntity.ok(abonnementService.getAbonnementById(id));
    }

    @Operation(summary = "Lister les abonnements d'un utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste retournée"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<Abonnement>> getAbonnementsByUtilisateur(@PathVariable Long userId) {
        return ResponseEntity.ok(abonnementService.getAbonnementsByUtilisateur(userId));
    }

    @Operation(summary = "Historique complet paginé de l'utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page d'historique retournée"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/users/{userId}/complet")
    public ResponseEntity<Page<Abonnement>> getFullHistory(
            @PathVariable Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(abonnementService.getFullHistory(userId, pageable));
    }

    @Operation(summary = "Récupérer l'abonnement actif complet d'un utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Abonnement actif trouvé"),
            @ApiResponse(responseCode = "404", description = "Aucun abonnement actif", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/users/{userId}/active-subscription")
    public ResponseEntity<Abonnement> getActif(@PathVariable Long userId) {
        return ResponseEntity.ok(abonnementService.getActif(userId));
    }

    @Operation(summary = "Consulter l'historique des paiements / renouvellements")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des renouvellements"),
            @ApiResponse(responseCode = "404", description = "Abonnement non trouvé", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}/historique-paiements")
    public ResponseEntity<List<Renouvellement>> getHistoriquePaiements(@PathVariable Long id) {
        return ResponseEntity.ok(abonnementService.getHistoriquePaiements(id));
    }

    @Operation(summary = "Activer / Désactiver le renouvellement automatique")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statut mis à jour"),
            @ApiResponse(responseCode = "404", description = "Abonnement non trouvé", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Transition de statut impossible", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PatchMapping("/{id}/renouvellement-auto")
    public ResponseEntity<Abonnement> toggleAutoRenouvellement(
            @PathVariable Long id,
            @Parameter(description = "true = activer, false = désactiver") @RequestParam boolean enable) {
        return ResponseEntity.ok(abonnementService.toggleAutoRenouvellement(id, enable));
    }

    @Operation(summary = "Renouveler manuellement son abonnement avant expiration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Renouvellement initié"),
            @ApiResponse(responseCode = "404", description = "Abonnement non trouvé", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Renouvellement impossible (statut invalide)", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "502", description = "Erreur communication service paiement", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/{id}/renouveler-manuel")
    public ResponseEntity<Abonnement> renouvelerManuel(@PathVariable Long id) {
        return ResponseEntity.ok(abonnementService.renouvelerManuel(id));
    }

    @Operation(summary = "Demander l'annulation et le remboursement au prorata")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Demande d'annulation prise en compte"),
            @ApiResponse(responseCode = "404", description = "Abonnement non trouvé", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Annulation impossible (statut invalide)", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "502", description = "Erreur communication service paiement", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/{id}/annuler")
    public ResponseEntity<java.util.Map<String, String>> demanderAnnulation(@PathVariable Long id) {
        abonnementService.demanderAnnulation(id);
        return ResponseEntity.ok(java.util.Map.of("message", "Votre abonnement est en cours d'annulation, votre remboursement est en traitement"));
    }

    @Operation(summary = "Désactiver temporairement un abonnement")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Désactivation effectuée"),
            @ApiResponse(responseCode = "400", description = "Paramètre invalide", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Abonnement non trouvé", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Règle métier violée (ex : durée max dépassée)", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/{id}/desactiver")
    public ResponseEntity<Void> desactiver(
            @PathVariable Long id,
            @Parameter(description = "Nombre de jours de désactivation") @RequestParam int jours) {
        abonnementService.desactiver(id, jours);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Vérification d'abonnement actif pour G6")
    @GetMapping("/users/{userId}/actif")
    public ResponseEntity<ActiveSubscriptionResponseDTO> verifierAbonnementG6(@PathVariable Long userId) {
        return ResponseEntity.ok(abonnementService.verifierAbonnementActif(userId));
    }
}
