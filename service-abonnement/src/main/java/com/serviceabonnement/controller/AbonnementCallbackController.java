package com.serviceabonnement.controller;

import com.serviceabonnement.service.AbonnementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.serviceabonnement.dto.response.ErrorResponseDTO;

@RestController
@RequestMapping("/abonnements")
@RequiredArgsConstructor
@Tag(name = "Callbacks", description = "Endpoints de retour pour les services tiers (Paiement, etc.)")
public class AbonnementCallbackController {

    private final AbonnementService abonnementService;

    @Operation(summary = "Confirmation de paiement (Callback service paiement)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paiement confirmé"),
            @ApiResponse(responseCode = "404", description = "Transaction non trouvée", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/paiement/confirmation")
    public ResponseEntity<Void> confirmerPaiement(@RequestParam("transactionId") String transactionId) {
        abonnementService.confirmerPaiement(transactionId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Confirmation de remboursement (Callback service paiement)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Remboursement confirmé"),
            @ApiResponse(responseCode = "404", description = "Transaction non trouvée", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/remboursement/confirmation")
    public ResponseEntity<Void> confirmerRemboursement(@RequestParam("transactionId") String transactionId) {
        abonnementService.confirmerRemboursement(transactionId);
        return ResponseEntity.ok().build();
    }
}
