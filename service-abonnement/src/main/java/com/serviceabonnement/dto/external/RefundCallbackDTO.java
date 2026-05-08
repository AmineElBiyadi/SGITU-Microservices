package com.serviceabonnement.dto.external;

import lombok.Data;

@Data
public class RefundCallbackDTO {
    private String transactionId;
    private String statut; // EN_COURS, REMBOURSE, ECHEC_REMBOURSEMENT
    private Double montantRembourse;
    private String motif;
}
