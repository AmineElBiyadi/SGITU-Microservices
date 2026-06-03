package com.ensate.billetterie.ticket.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class PaymentResponse {
    private Long paymentId;
    private String transactionToken;

    @JsonAlias("status")
    private String paymentStatus;

    private String message;
    private Long invoiceId;
    private String invoiceNumber;
    private String failureReason;
}
