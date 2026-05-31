package com.sgitu.servicegestionincidents.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RenfortDTO {
    private Long id;
    private Long agentId;
    private Long auteurAffectationId;
    private LocalDateTime dateAffectation;
}
