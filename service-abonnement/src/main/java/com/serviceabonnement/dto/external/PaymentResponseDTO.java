package com.serviceabonnement.dto.external;

import lombok.Data;

@Data
public class PaymentResponseDTO {
    private String transactionId;
    private String statut;
    private String motif; // Optionnel
}
