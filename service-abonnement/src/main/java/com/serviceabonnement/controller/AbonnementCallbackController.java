package com.serviceabonnement.controller;

import com.serviceabonnement.service.AbonnementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/abonnements")
@RequiredArgsConstructor
public class AbonnementCallbackController {

    private final AbonnementService abonnementService;

    @PostMapping("/paiement/confirmation")
    public ResponseEntity<Void> confirmerPaiement(@RequestParam("transactionId") String transactionId) {
        abonnementService.confirmerPaiement(transactionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/remboursement/confirmation")
    public ResponseEntity<Void> confirmerRemboursement(@RequestParam("transactionId") String transactionId) {
        abonnementService.confirmerRemboursement(transactionId);
        return ResponseEntity.ok().build();
    }
}
