package com.ensate.billetterie.ticket.client;

import com.ensate.billetterie.ticket.dto.request.PaymentRequest;
import com.ensate.billetterie.ticket.dto.response.PaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PaymentServiceClient {
    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    public PaymentServiceClient(
            RestTemplate restTemplate,
            @Value("${payment.service.url}") String paymentServiceUrl) {
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
        log.info("Connecting to Payment Service REST API at {}", paymentServiceUrl);
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "payFallback")
    @Retry(name = "paymentService")
    @RateLimiter(name = "paymentService", fallbackMethod = "payRateLimitFallback")
    public PaymentResponse pay(PaymentRequest paymentRequest) {
        // Appel sécurisé au service de paiement (URL corrigée au pluriel "/payments")
        PaymentResponse response = restTemplate.postForObject(
                paymentServiceUrl + "/payments",
                paymentRequest,
                PaymentResponse.class
        );
        log.info("Received response from billing service via REST: {}", response);
        return response;
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "refundFallback")
    @Retry(name = "paymentService")
    @RateLimiter(name = "paymentService", fallbackMethod = "refundRateLimitFallback")
    public PaymentResponse refund(Long paymentId, java.math.BigDecimal amount, String reason) {
        // Appel réel de remboursement via l'endpoint correct de G6
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("amount", amount);
        requestBody.put("reason", reason);

        PaymentResponse response = restTemplate.postForObject(
                paymentServiceUrl + "/payments/" + paymentId + "/refund",
                requestBody,
                PaymentResponse.class
        );
        log.info("Received refund response from payment service via REST: {}", response);
        return response;
    }

    // ==========================================
    //            METHODES DE FALLBACK
    // ==========================================

    // Fallbacks pour Pay (Paiement)
    public PaymentResponse payFallback(PaymentRequest paymentRequest, Throwable t) {
        log.error("Payment fallback triggered due to error: {}", t.getMessage());
        PaymentResponse fallback = new PaymentResponse();
        fallback.setPaymentStatus("FAILED");
        fallback.setFailureReason("SERVICE_UNAVAILABLE");
        fallback.setMessage("Le service de paiement est indisponible pour le moment (Circuit Breaker ouvert / Timeout).");
        return fallback;
    }

    public PaymentResponse payRateLimitFallback(PaymentRequest paymentRequest, Throwable t) {
        log.error("Payment Rate Limit exceeded: {}", t.getMessage());
        PaymentResponse fallback = new PaymentResponse();
        fallback.setPaymentStatus("FAILED");
        fallback.setFailureReason("RATE_LIMITED");
        fallback.setMessage("Trop de tentatives de paiement. Veuillez patienter un instant.");
        return fallback;
    }

    // Fallbacks pour Refund (Remboursement)
    public PaymentResponse refundFallback(Long paymentId, java.math.BigDecimal amount, String reason, Throwable t) {
        log.error("Refund fallback triggered due to error: {}", t.getMessage());
        PaymentResponse fallback = new PaymentResponse();
        fallback.setPaymentStatus("FAILED");
        fallback.setFailureReason("SERVICE_UNAVAILABLE");
        fallback.setMessage("Impossible de traiter le remboursement pour le moment (Service indisponible).");
        return fallback;
    }

    public PaymentResponse refundRateLimitFallback(Long paymentId, java.math.BigDecimal amount, String reason, Throwable t) {
        log.error("Refund Rate Limit exceeded: {}", t.getMessage());
        PaymentResponse fallback = new PaymentResponse();
        fallback.setPaymentStatus("FAILED");
        fallback.setFailureReason("RATE_LIMITED");
        fallback.setMessage("Trop de requêtes de remboursement. Veuillez patienter.");
        return fallback;
    }
}