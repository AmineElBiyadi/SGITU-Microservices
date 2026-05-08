package com.serviceabonnement.dto.external;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentRequestDTO {
    private Long userId;
    private String sourceType;
    private Long sourceId;
    private Double amount;
    private String paymentMethod;
    private String email;
    private String description;
}
