package com.g7suivivehicules.dto;



import com.g7suivivehicules.entity.Anomalie;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalieResponse {

    private UUID id;
    private UUID vehiculeId;
    private Anomalie.TypeAnomalie type;
    private Double valeur;
    private Double seuil;
    private LocalDateTime timestamp;
    private Boolean traitee;
}