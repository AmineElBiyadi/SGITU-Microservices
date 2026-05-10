package ma.sgitu.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.payment.client.SubscriptionClient;
import ma.sgitu.payment.entity.Payment;
import ma.sgitu.payment.entity.Refund;
import ma.sgitu.payment.enums.SourceType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionCallbackService {

    private final SubscriptionClient subscriptionClient;

    public void sendPaymentConfirmation(Payment payment) {
        if (payment.getSourceType() != SourceType.SUBSCRIPTION) {
            log.debug("Paiement non-SUBSCRIPTION, pas de callback G2");
            return;
        }

        log.info("Envoi confirmation paiement vers G2 pour subscription ID: {}", payment.getSourceId());

        Map<String, Object> data = new HashMap<>();
        data.put("transactionToken", payment.getTransactionToken());
        data.put("status", payment.getStatus().name());
        data.put("message", payment.getStatus().name().equals("SUCCESS") ? "Paiement validé" : "Paiement refusé");

        if (payment.getFailureReason() != null) {
            data.put("failureReason", payment.getFailureReason().name());
        }

        try {
            subscriptionClient.confirmPayment(data);
            log.info("Confirmation paiement envoyée à G2 avec succès");
        } catch (Exception e) {
            log.error("Échec envoi confirmation paiement vers G2: {}", e.getMessage());
        }
    }

    public void sendRefundConfirmation(Payment payment, Refund refund) {
        if (payment.getSourceType() != SourceType.SUBSCRIPTION) {
            log.debug("Paiement non-SUBSCRIPTION, pas de callback refund G2");
            return;
        }

        log.info("Envoi confirmation remboursement vers G2 pour subscription ID: {}", payment.getSourceId());

        Map<String, Object> data = new HashMap<>();
        data.put("transactionToken", payment.getTransactionToken());
        data.put("refundToken", refund.getRefundToken());
        data.put("status", refund.getStatus().name());
        data.put("amountRefunded", refund.getAmount());
        data.put("message", refund.getStatus().name().equals("REFUNDED") ? "Remboursement effectué avec succès" : "Remboursement échoué");

        if (refund.getFailureReason() != null) {
            data.put("failureReason", refund.getFailureReason());
        }

        try {
            subscriptionClient.confirmRefund(data);
            log.info("Confirmation remboursement envoyée à G2 avec succès");
        } catch (Exception e) {
            log.error("Échec envoi confirmation remboursement vers G2: {}", e.getMessage());
        }
    }

    public Map<String, Object> checkActiveSubscription(Long userId) {
        log.info("Vérification abonnement actif pour userId: {}", userId);

        try {
            return subscriptionClient.checkActiveSubscription(userId).getBody();
        } catch (Exception e) {
            log.error("Échec vérification abonnement actif: {}", e.getMessage());
            return null;
        }
    }
}