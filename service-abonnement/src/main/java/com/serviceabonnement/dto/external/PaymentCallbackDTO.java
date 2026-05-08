package com.serviceabonnement.dto.external;

import lombok.Data;

@Data
public class PaymentCallbackDTO {
    private String transactionToken;
    private String status; // SUCCESS, FAILED, etc.
    private String message;
}
