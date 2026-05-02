package ma.sgitu.payment.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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