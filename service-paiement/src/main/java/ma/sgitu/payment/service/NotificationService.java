package ma.sgitu.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.payment.client.NotificationClient;
import ma.sgitu.payment.dto.request.NotificationRequest;
import ma.sgitu.payment.entity.Invoice;
import ma.sgitu.payment.entity.Payment;
import ma.sgitu.payment.entity.PaymentAccount;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service pour envoyer les notifications à G5
 * Conforme au contrat G5 Notifications v3.1
 *
 * Types de notifications G6 :
 * 1. PAYMENT_METHOD_OTP - OTP pour valider CARD ou MOBILE_MONEY
 * 2. PAYMENT_SUCCESS - Paiement validé
 * 3. PAYMENT_FAILED - Paiement échoué
 * 4. PAYMENT_CANCELLED - Paiement annulé
 * 5. INVOICE_GENERATED - Facture générée
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationClient notificationClient;

    /**
     * Envoie OTP par email pour validation moyen de paiement
     *
     * @param paymentAccount Compte de paiement en attente
     * @param otpCode Code OTP généré
     * @param userEmail Email de l'utilisateur
     */
    public void sendOtpNotification(PaymentAccount paymentAccount, String otpCode, String userEmail) {
        log.info("Envoi notification OTP pour PaymentAccount ID: {}", paymentAccount.getId());

        // Construction metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("paymentAccountId", paymentAccount.getId());
        metadata.put("otpCode", otpCode);
        metadata.put("paymentMethod", paymentAccount.getPaymentMethod().name());
        metadata.put("maskedIdentifier", paymentAccount.getMaskedIdentifier());

        // Construction request
        NotificationRequest request = NotificationRequest.builder()
                .notificationId(UUID.randomUUID().toString())
                .sourceService("PAYMENT")
                .eventType("PAYMENT_METHOD_OTP")
                .channel("EMAIL")
                .priority("HIGH")
                .recipient(NotificationRequest.Recipient.builder()
                        .userId(String.valueOf(paymentAccount.getUserId()))
                        .email(userEmail)
                        .build())
                .metadata(metadata)
                .build();

        // Envoi via OpenFeign
        try {
            notificationClient.sendNotification(request);
            log.info("Notification OTP envoyée avec succès");
        } catch (Exception e) {
            log.warn("G5 indisponible — OTP généré localement uniquement");
        }

    }

    /**
     * Envoie notification de paiement réussi
     *
     * @param payment Paiement SUCCESS
     * @param userEmail Email de l'utilisateur
     */
    public void sendPaymentSuccessNotification(Payment payment, String userEmail) {
        log.info("Envoi notification PAYMENT_SUCCESS pour paiement ID: {}", payment.getId());

        // Construction metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("paymentId", payment.getId());
        metadata.put("amount", payment.getAmount());
        metadata.put("paymentMethod", payment.getPaymentMethod().name());
        metadata.put("sourceType", payment.getSourceType().name());
        metadata.put("sourceId", payment.getSourceId());

        // Construction request
        NotificationRequest request = NotificationRequest.builder()
                .notificationId(UUID.randomUUID().toString())
                .sourceService("PAYMENT")
                .eventType("PAYMENT_SUCCESS")
                .channel("EMAIL")
                .priority("NORMAL")
                .recipient(NotificationRequest.Recipient.builder()
                        .userId(String.valueOf(payment.getUserId()))
                        .email(userEmail)
                        .build())
                .metadata(metadata)
                .build();

        // Envoi via OpenFeign
        try {
            notificationClient.sendNotification(request);
            log.info("Notification PAYMENT_SUCCESS envoyée pour paiement ID: {}", payment.getId());
        } catch (Exception e) {
            log.error("Échec notification PAYMENT_SUCCESS pour paiement ID {}: {}", payment.getId(), e.getMessage());
            // On ne bloque pas le processus si la notification échoue
        }
    }

    /**
     * Envoie notification de paiement échoué
     *
     * @param payment Paiement FAILED
     * @param userEmail Email de l'utilisateur
     */
    public void sendPaymentFailedNotification(Payment payment, String userEmail) {
        log.info("Envoi notification PAYMENT_FAILED pour paiement ID: {}", payment.getId());

        // Construction metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("paymentId", payment.getId());
        metadata.put("amount", payment.getAmount());
        metadata.put("paymentMethod", payment.getPaymentMethod().name());
        metadata.put("failureReason", payment.getFailureReason() != null ? payment.getFailureReason().name() : "UNKNOWN");
        metadata.put("sourceType", payment.getSourceType().name());
        metadata.put("sourceId", payment.getSourceId());

        // Construction request
        NotificationRequest request = NotificationRequest.builder()
                .notificationId(UUID.randomUUID().toString())
                .sourceService("PAYMENT")
                .eventType("PAYMENT_FAILED")
                .channel("EMAIL")
                .priority("HIGH")
                .recipient(NotificationRequest.Recipient.builder()
                        .userId(String.valueOf(payment.getUserId()))
                        .email(userEmail)
                        .build())
                .metadata(metadata)
                .build();

        // Envoi via OpenFeign
        try {
            notificationClient.sendNotification(request);
            log.info("Notification PAYMENT_FAILED envoyée pour paiement ID: {}", payment.getId());
        } catch (Exception e) {
            log.error("Échec notification PAYMENT_FAILED pour paiement ID {}: {}", payment.getId(), e.getMessage());
        }
    }

    /**
     * Envoie notification de paiement annulé
     *
     * @param payment Paiement CANCELLED
     * @param userEmail Email de l'utilisateur
     */
    public void sendPaymentCancelledNotification(Payment payment, String userEmail) {
        log.info("Envoi notification PAYMENT_CANCELLED pour paiement ID: {}", payment.getId());

        // Construction metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("paymentId", payment.getId());
        metadata.put("amount", payment.getAmount());
        metadata.put("sourceType", payment.getSourceType().name());
        metadata.put("sourceId", payment.getSourceId());

        // Construction request
        NotificationRequest request = NotificationRequest.builder()
                .notificationId(UUID.randomUUID().toString())
                .sourceService("PAYMENT")
                .eventType("PAYMENT_CANCELLED")
                .channel("EMAIL")
                .priority("NORMAL")
                .recipient(NotificationRequest.Recipient.builder()
                        .userId(String.valueOf(payment.getUserId()))
                        .email(userEmail)
                        .build())
                .metadata(metadata)
                .build();

        // Envoi via OpenFeign
        try {
            notificationClient.sendNotification(request);
            log.info("Notification PAYMENT_CANCELLED envoyée pour paiement ID: {}", payment.getId());
        } catch (Exception e) {
            log.error("Échec notification PAYMENT_CANCELLED pour paiement ID {}: {}", payment.getId(), e.getMessage());
        }
    }

    /**
     * Envoie notification de facture générée
     *
     * @param invoice Facture générée
     */
    public void sendInvoiceGeneratedNotification(Invoice invoice) {
        log.info("Envoi notification INVOICE_GENERATED pour facture: {}", invoice.getInvoiceNumber());

        // Construction metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceId", invoice.getId());
        metadata.put("invoiceNumber", invoice.getInvoiceNumber());
        metadata.put("paymentId", invoice.getPayment().getId());
        metadata.put("amount", invoice.getTotalAmount());
        metadata.put("paymentMethod", invoice.getPaymentMethod().name());
        metadata.put("sourceType", invoice.getSourceType().name());
        metadata.put("sourceId", invoice.getSourceId());

        // Construction request
        NotificationRequest request = NotificationRequest.builder()
                .notificationId(UUID.randomUUID().toString())
                .sourceService("PAYMENT")
                .eventType("INVOICE_GENERATED")
                .channel("EMAIL")
                .priority("NORMAL")
                .recipient(NotificationRequest.Recipient.builder()
                        .userId(String.valueOf(invoice.getUserId()))
                        .email("user@example.com") // TODO: récupérer email réel depuis G3
                        .build())
                .metadata(metadata)
                .build();

        // Envoi via OpenFeign
        try {
            notificationClient.sendNotification(request);
            log.info("Notification INVOICE_GENERATED envoyée pour facture: {}", invoice.getInvoiceNumber());
        } catch (Exception e) {
            log.error("Échec notification INVOICE_GENERATED pour facture {}: {}", invoice.getInvoiceNumber(), e.getMessage());
        }
    }
}