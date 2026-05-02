package ma.sgitu.payment.controller;

import lombok.RequiredArgsConstructor;
import ma.sgitu.payment.dto.request.AddMobileMoneyRequest;
import ma.sgitu.payment.entity.TestMobileMoneyAccount;
import ma.sgitu.payment.repository.TestMobileMoneyAccountRepository;
import ma.sgitu.payment.service.PaymentAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payment-accounts")
@RequiredArgsConstructor
public class PaymentAccountController {

    private final PaymentAccountService paymentAccountService;
    private final TestMobileMoneyAccountRepository testMobileMoneyAccountRepository;

    @PostMapping("/mobile-money")
    public ResponseEntity<String> addMobileMoney(@RequestBody AddMobileMoneyRequest request) {
        String token = paymentAccountService.registerMobileMoney(request);
        return ResponseEntity.ok("Compte enregistré. Token généré : " + token + ". Veuillez vérifier votre email pour l'OTP.");
    }

    @GetMapping("/test-mobile-money-accounts")
    public ResponseEntity<List<TestMobileMoneyAccount>> getTestAccounts() {
        return ResponseEntity.ok(testMobileMoneyAccountRepository.findAll());
    }
}