package ma.sgitu.payment.mapper;

import ma.sgitu.payment.dto.response.InvoiceResponse;
import ma.sgitu.payment.entity.Invoice;
import org.springframework.stereotype.Component;

/**
 * Mapper manuel pour Invoice → InvoiceResponse
 * Conversion Entity ↔ DTO
 */
@Component
public class InvoiceMapper {

    /**
     * Convertit Invoice entity vers InvoiceResponse DTO
     * @param invoice Entity Invoice
     * @return InvoiceResponse DTO
     */
    public InvoiceResponse toResponse(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .paymentId(invoice.getPayment().getId())
                .transactionToken(invoice.getPayment().getTransactionToken())
                .userId(invoice.getUserId())
                .sourceType(invoice.getSourceType().name())
                .sourceId(invoice.getSourceId())
                .totalAmount(invoice.getTotalAmount())
                .paymentMethod(invoice.getPaymentMethod().name())
                .issuedAt(invoice.getIssuedAt())
                .build();
    }
}