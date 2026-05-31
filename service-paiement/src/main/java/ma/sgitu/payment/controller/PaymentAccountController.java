package ma.sgitu.payment.controller;

import lombok.RequiredArgsConstructor;
import ma.sgitu.payment.dto.request.AddCardRequest;
import ma.sgitu.payment.dto.request.AddMobileMoneyRequest;
import ma.sgitu.payment.dto.request.VerifyOtpRequest;
import ma.sgitu.payment.dto.response.PaymentAccountResponse;
import ma.sgitu.payment.service.PaymentAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/payment-accounts")
@RequiredArgsConstructor
public class PaymentAccountController {

    private final PaymentAccountService paymentAccountService;

    @PostMapping("/card")
    public ResponseEntity<PaymentAccountResponse> addCard(
            @Valid @RequestBody AddCardRequest request) {

        PaymentAccountResponse response = paymentAccountService.addCard(request);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/mobile-money")
    public ResponseEntity<PaymentAccountResponse> addMobileMoney(
            @Valid @RequestBody AddMobileMoneyRequest request) {

        PaymentAccountResponse response = paymentAccountService.addMobileMoney(request);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/{paymentAccountId}/verify-otp")
    public ResponseEntity<PaymentAccountResponse> verifyOtp(
            @PathVariable Long paymentAccountId,
            @Valid @RequestBody VerifyOtpRequest request) {

        request.setPaymentAccountId(paymentAccountId);
        PaymentAccountResponse response = paymentAccountService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentAccountResponse>> getByUserId(
            @PathVariable Long userId) {

        return ResponseEntity.ok(paymentAccountService.getByUserId(userId));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<PaymentAccountResponse> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(paymentAccountService.getById(id));
    }

    @DeleteMapping("/id/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        paymentAccountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}