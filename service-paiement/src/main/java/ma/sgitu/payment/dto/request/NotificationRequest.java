package ma.sgitu.payment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class NotificationRequest {
    private Long userId;
    private String channel; // "EMAIL"
    private String recipient; // l'email du client
    private String type; // "PAYMENT_METHOD_OTP"
    private String subject;
    private String message;
    private String sourceService; // "PAYMENT"
    private String sourceType; // "PAYMENT_ACCOUNT"
    private Long sourceId;
}