package ma.sgitu.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefundResponse {

    private Long refundId;
    private String refundToken;
    private Long paymentId;
    private Long userId;
    private BigDecimal amountRefunded;
    private String status;
    private String reason;
    private String failureReason;
    private String message;
    private LocalDateTime createdAt;
}