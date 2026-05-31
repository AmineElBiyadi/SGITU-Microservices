package com.ensate.billetterie.ticket.dto.response;

import lombok.Data;

@Data
public class PaymentResponse {
    private String paymentId;
    private String transactionToken;
    private String paymentStatus;
    private String message;
    private String invoiceId;
    private String invoiceNumber;
    private String failureReason;
}
