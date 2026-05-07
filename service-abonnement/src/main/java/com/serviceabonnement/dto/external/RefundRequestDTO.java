package com.serviceabonnement.dto.external;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefundRequestDTO {
    private String transactionId;
    private Double montantRemboursement;
    private String motif;
}
