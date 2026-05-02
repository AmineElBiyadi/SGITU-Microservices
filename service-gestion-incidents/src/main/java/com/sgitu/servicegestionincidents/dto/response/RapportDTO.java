package com.sgitu.servicegestionincidents.dto.response;

import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapportDTO {

    private String periode;
    private Integer nbIncidents;
    private Map<String, Object> statistiques;
}
