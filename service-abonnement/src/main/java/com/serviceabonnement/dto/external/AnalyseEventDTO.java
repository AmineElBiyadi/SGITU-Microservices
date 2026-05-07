package com.serviceabonnement.dto.external;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalyseEventDTO {
    private String timestamp;
    private String userId;
    private String action;
    private String planType;
}
