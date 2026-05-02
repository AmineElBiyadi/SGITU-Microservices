package ma.sgitu.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.sgitu.payment.dto.request.AddCardRequest;
import ma.sgitu.payment.dto.request.VerifyOtpRequest;
import ma.sgitu.payment.dto.response.PaymentAccountResponse;
import ma.sgitu.payment.dto.response.TestCardResponse;
import ma.sgitu.payment.service.PaymentAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Moyens de paiement", description = "Gestion des cartes et Mobile Money")
public class PaymentAccountController {

    private final PaymentAccountService paymentAccountService;

    @PostMapping("/payment-accounts/card")
    @Operation(summary = "Ajouter une carte et déclencher OTP email")
    public ResponseEntity<PaymentAccountResponse> addCard(
            @Valid @RequestBody AddCardRequest request) {
        return ResponseEntity.ok(paymentAccountService.addCard(request));
    }

    @PostMapping("/payment-accounts/{paymentAccountId}/verify-otp")
    @Operation(summary = "Vérifier OTP et activer le moyen de paiement")
    public ResponseEntity<PaymentAccountResponse> verifyOtp(
            @PathVariable Long paymentAccountId,
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(paymentAccountService.verifyOtp(paymentAccountId, request));
    }

    @GetMapping("/payment-accounts/user/{userId}")
    @Operation(summary = "Lister les moyens de paiement d'un utilisateur")
    public ResponseEntity<List<PaymentAccountResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentAccountService.getByUserId(userId));
    }

    @GetMapping("/payment-accounts/{paymentAccountId}")
    @Operation(summary = "Consulter un moyen de paiement précis")
    public ResponseEntity<PaymentAccountResponse> getById(@PathVariable Long paymentAccountId) {
        return ResponseEntity.ok(paymentAccountService.getById(paymentAccountId));
    }

    @DeleteMapping("/payment-accounts/{paymentAccountId}")
    @Operation(summary = "Supprimer un moyen de paiement")
    public ResponseEntity<Void> delete(@PathVariable Long paymentAccountId) {
        paymentAccountService.delete(paymentAccountId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/test-cards")
    @Operation(summary = "Lister les cartes fictives (sans données sensibles)")
    public ResponseEntity<List<TestCardResponse>> getTestCards() {
        return ResponseEntity.ok(paymentAccountService.getTestCards());
    }
}