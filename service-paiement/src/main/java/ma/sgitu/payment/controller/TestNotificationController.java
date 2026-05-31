package ma.sgitu.payment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.payment.dto.request.NotificationRequest;
import ma.sgitu.payment.entity.Payment;
import ma.sgitu.payment.repository.PaymentRepository;
import ma.sgitu.payment.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class TestNotificationController {

    private final NotificationService notificationService;
    private final PaymentRepository paymentRepository;

    @PostMapping("/notification/{paymentId}")
    public ResponseEntity<Map<String, Object>> testNotification(
            @PathVariable Long paymentId,
            @RequestParam String email) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable"));

        notificationService.sendPaymentSuccessNotification(payment, email);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Notification envoyée pour paiement ID " + paymentId);
        response.put("email", email);
        response.put("paymentStatus", payment.getStatus());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/notification-format/{paymentId}")
    public ResponseEntity<NotificationRequest> getNotificationFormat(
            @PathVariable Long paymentId,
            @RequestParam String email) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable"));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("paymentId", payment.getId());
        metadata.put("amount", payment.getAmount());
        metadata.put("paymentMethod", payment.getPaymentMethod().name());
        metadata.put("sourceType", payment.getSourceType().name());
        metadata.put("sourceId", payment.getSourceId());

        NotificationRequest request = NotificationRequest.builder()
                .notificationId(UUID.randomUUID().toString())
                .sourceService("PAYMENT")
                .eventType("PAYMENT_SUCCESS")
                .channel("EMAIL")
                .priority("NORMAL")
                .recipient(NotificationRequest.Recipient.builder()
                        .userId(String.valueOf(payment.getUserId()))
                        .email(email)
                        .build())
                .metadata(metadata)
                .build();

        return ResponseEntity.ok(request);
    }
}