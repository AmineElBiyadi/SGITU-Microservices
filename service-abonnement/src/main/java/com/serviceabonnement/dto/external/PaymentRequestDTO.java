package com.serviceabonnement.dto.external;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentRequestDTO {
    private Long abonnementId;
    private Long userId;
    private Double montant;
    private String description;
}
