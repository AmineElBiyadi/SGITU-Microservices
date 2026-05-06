package ma.sgitu.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour moyens de paiement enregistrés
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentAccountResponse {

    private Long id;
    private Long userId;
    private String paymentMethod;
    private String paymentToken;
    private String maskedIdentifier;
    private String provider;
    private BigDecimal balance;
    private String status;
    private Integer expiryMonth;
    private Integer expiryYear;
    private LocalDateTime createdAt;
}