package com.serviceabonnement.mapper;

import com.serviceabonnement.dto.request.SouscriptionRequestDTO;
import com.serviceabonnement.dto.response.AbonnementResponseDTO;
import com.serviceabonnement.entity.Abonnement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PlanAbonnementMapper.class})
public interface AbonnementMapper {
    
    AbonnementResponseDTO toResponseDTO(Abonnement entity);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "plan", ignore = true)
    @Mapping(target = "dateDebut", ignore = true)
    @Mapping(target = "dateFin", ignore = true)
    @Mapping(target = "statut", ignore = true)
    Abonnement toEntity(SouscriptionRequestDTO dto);
}
