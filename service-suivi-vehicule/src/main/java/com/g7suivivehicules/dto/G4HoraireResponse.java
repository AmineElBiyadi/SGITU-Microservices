package com.g7suivivehicules.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class G4HoraireResponse {
    private Long id;
    private Long trajetId;
    private Long arretId;
    private LocalTime heurePassage;
    private Integer jourSemaine;
}
