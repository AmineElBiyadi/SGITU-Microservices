package com.serviceabonnement.dto.response;

import com.serviceabonnement.enums.StatutPaiement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementResponseDTO {
    private Long id;
    private LocalDateTime datePaiement;
    private BigDecimal montant;
    private String moyenPaiement;
    private StatutPaiement statut;
    private String transactionId;
}
