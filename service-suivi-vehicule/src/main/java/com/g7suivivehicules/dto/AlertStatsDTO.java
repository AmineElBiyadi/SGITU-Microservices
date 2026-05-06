package com.g7suivivehicules.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatsDTO {
    private Map<String, Long> parType;
    private Map<String, Long> parStatut;
    private long totalAlertes;
}
