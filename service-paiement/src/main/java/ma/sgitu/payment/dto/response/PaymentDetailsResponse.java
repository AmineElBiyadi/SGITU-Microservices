package ma.sgitu.payment.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentDetailsResponse {
    private Long id;
    private String transactionToken;
    private Long userId;
    private String sourceType;
    private String sourceId;
    private BigDecimal amount;
    private String paymentMethod;
    private String savedPaymentToken;
    private String status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
