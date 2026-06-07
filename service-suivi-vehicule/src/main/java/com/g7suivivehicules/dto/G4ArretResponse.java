package com.g7suivivehicules.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class G4ArretResponse {
    private Long id;
    private String code;
    private String nom;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Long ligneId;
}
