package com.serviceabonnement.client;

import com.serviceabonnement.dto.external.PaymentRequestDTO;
import com.serviceabonnement.dto.external.PaymentResponseDTO;
import com.serviceabonnement.dto.external.RefundRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "service-paiement", url = "${application.config.paiement-service-url}")
public interface PaiementClient {

    @PostMapping("/api/v1/payments/process")
    PaymentResponseDTO initierPaiement(@RequestBody PaymentRequestDTO request);

    @GetMapping("/api/v1/payments/{transactionId}")
    PaymentResponseDTO verifierPaiement(@PathVariable("transactionId") String transactionId);

    @PostMapping("/api/v1/payments/refund")
    PaymentResponseDTO rembourser(@RequestBody RefundRequestDTO request);
}
