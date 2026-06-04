package com.g7suivivehicules.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class G4TrajetResponse {
    private Long id;
    private Long ligneId;
    private String code;
    private String nom;
    private boolean actif;
    private List<G4ArretResponse> arrets;
}
