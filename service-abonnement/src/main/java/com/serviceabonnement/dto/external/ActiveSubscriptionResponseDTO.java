package com.serviceabonnement.dto.external;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActiveSubscriptionResponseDTO {
    private boolean aUnAbonnementActif;
    private String typePlan;
    private String dateExpiration;
}
