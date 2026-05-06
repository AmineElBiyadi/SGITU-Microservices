package ma.sgitu.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour les factures
 * Utilisé par InvoiceController
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoiceResponse {

    private Long id;
    private String invoiceNumber;
    private Long paymentId;
    private String transactionToken;
    private Long userId;
    private String sourceType;        // TICKET ou SUBSCRIPTION
    private Long sourceId;
    private BigDecimal totalAmount;
    private String paymentMethod;     // CARD ou MOBILE_MONEY
    private LocalDateTime issuedAt;
}