package ma.sgitu.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.payment.entity.Payment;
import ma.sgitu.payment.entity.Refund;
import ma.sgitu.payment.enums.SourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketCallbackService {

    private final RestTemplate restTemplate;

    @Value("${ticket.service.url:http://localhost:8081}")
    private String ticketServiceUrl;

    public void sendRefundConfirmation(Payment payment, Refund refund) {
        if (payment.getSourceType() != SourceType.TICKET) {
            log.debug("Paiement non-TICKET, pas de callback refund G1");
            return;
        }

        log.info("Envoi confirmation remboursement vers G1 pour ticket ID: {}", payment.getSourceId());

        Map<String, Object> data = new HashMap<>();
        data.put("transactionToken", payment.getTransactionToken());
        data.put("refundToken", refund.getRefundToken());
        data.put("status", refund.getStatus().name());
        data.put("amountRefunded", refund.getAmount());
        data.put("ticketId", payment.getSourceId());
        
        if (refund.getFailureReason() != null) {
            data.put("failureReason", refund.getFailureReason());
        }

        try {
            String url = ticketServiceUrl + "/tickets/remboursement/confirmation"; // Ensure this matches G1's actual endpoint if needed, or adjust.
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(data);
            restTemplate.postForEntity(url, request, Void.class);
            log.info("Confirmation remboursement envoyée à G1 avec succès");
        } catch (Exception e) {
            log.error("Échec envoi confirmation remboursement vers G1: {}", e.getMessage());
        }
    }
}
