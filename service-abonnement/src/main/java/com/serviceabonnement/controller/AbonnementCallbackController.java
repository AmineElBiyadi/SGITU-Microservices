package com.serviceabonnement.controller;

import com.serviceabonnement.service.AbonnementService;
import com.serviceabonnement.dto.external.PaymentCallbackDTO;
import com.serviceabonnement.dto.external.RefundCallbackDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

@RestController
@RequestMapping("/abonnements")
@RequiredArgsConstructor
@Tag(name = "Callbacks", description = "Endpoints de retour pour les services tiers (Paiement, etc.)")
public class AbonnementCallbackController {

    private final AbonnementService abonnementService;

    @Operation(summary = "Confirmation de paiement (Callback G6)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paiement traité avec succès"),
            @ApiResponse(responseCode = "404", description = "Transaction non trouvée")
    })
    @PostMapping("/paiement/confirmation")
    public ResponseEntity<Map<String, String>> confirmerPaiement(@RequestBody PaymentCallbackDTO callback) {
        abonnementService.confirmerPaiement(callback);
        return ResponseEntity.ok(Map.of(
            "statut", "OK",
            "message", "Abonnement mis à jour avec succès"
        ));
    }

    @Operation(summary = "Confirmation de remboursement (Callback G6)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Remboursement traité avec succès"),
            @ApiResponse(responseCode = "404", description = "Transaction non trouvée")
    })
    @PostMapping("/remboursement/confirmation")
    public ResponseEntity<Map<String, String>> confirmerRemboursement(@RequestBody RefundCallbackDTO callback) {
        abonnementService.confirmerRemboursement(callback);
        String message = "REMBOURSE".equals(callback.getStatut()) ? "Abonnement annulé avec succès" : "Échec de remboursement enregistré";
        return ResponseEntity.ok(Map.of(
            "statut", "OK",
            "message", message
        ));
    }
}
