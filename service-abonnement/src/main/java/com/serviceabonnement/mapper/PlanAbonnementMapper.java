package com.serviceabonnement.mapper;

import com.serviceabonnement.dto.response.PlanAbonnementResponseDTO;
import com.serviceabonnement.entity.PlanAbonnement;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlanAbonnementMapper {
    PlanAbonnementResponseDTO toResponseDTO(PlanAbonnement entity);
    PlanAbonnement toEntity(PlanAbonnementResponseDTO dto);
}
