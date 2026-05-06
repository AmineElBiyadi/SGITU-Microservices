package com.serviceabonnement.dto.response;

import com.serviceabonnement.enums.StatutRenouvellement;
import com.serviceabonnement.enums.TypeRenouvellement;
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
public class RenouvellementResponseDTO {
    private Long id;
    private LocalDateTime dateRenouvellement;
    private BigDecimal montant;
    private StatutRenouvellement statut;
    private TypeRenouvellement type;
}
