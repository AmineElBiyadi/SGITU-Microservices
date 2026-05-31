package com.g7suivivehicules.service;

import com.g7suivivehicules.dto.AlertResponseDTO;
import com.g7suivivehicules.dto.VehicleSnapshotDTO;
import com.g7suivivehicules.entity.PositionGPS;
import com.g7suivivehicules.entity.Vehicule;
import com.g7suivivehicules.repository.AlertRepository;
import com.g7suivivehicules.repository.PositionGPSRepository;
import com.g7suivivehicules.repository.VehiculeRepository;
import com.g7suivivehicules.exception.VehiculeNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotService {

    private final VehiculeRepository vehiculeRepository;
    private final PositionGPSRepository positionGPSRepository;
    private final AlertRepository alertRepository;

    @Transactional(readOnly = true)
    public VehicleSnapshotDTO getVehicleSnapshot(UUID vehiculeId) {
        Vehicule vehicule = vehiculeRepository.findById(vehiculeId)
                .orElseThrow(() -> new VehiculeNotFoundException(vehiculeId));

        Optional<PositionGPS> lastPositionOpt = positionGPSRepository.findTopByVehiculeIdOrderByTimestampDesc(vehiculeId);
        List<AlertResponseDTO> activeAlerts = alertRepository.findActiveByVehiculeId(vehiculeId)
                .stream()
                .map(AlertResponseDTO::fromEntity)
                .collect(Collectors.toList());

        VehicleSnapshotDTO.VehicleSnapshotDTOBuilder builder = VehicleSnapshotDTO.builder()
                .id(vehicule.getId())
                .immatriculation(vehicule.getImmatriculation())
                .type(vehicule.getType())
                .ligne(vehicule.getLigne())
                .statut(vehicule.getStatut())
                .conducteurId(vehicule.getConducteurId())
                .alertesActives(activeAlerts);

        if (lastPositionOpt.isPresent()) {
            PositionGPS lastPosition = lastPositionOpt.get();
            builder.latitude(lastPosition.getLatitude())
                   .longitude(lastPosition.getLongitude())
                   .vitesse(lastPosition.getVitesse())
                   .cap(lastPosition.getCap())
                   .timestamp(lastPosition.getTimestamp());
        }

        return builder.build();
    }

    @Transactional(readOnly = true)
    public List<VehicleSnapshotDTO> getFleetSnapshot() {
        List<Vehicule> vehicules = vehiculeRepository.findAll();
        return vehicules.stream()
                .map(v -> {
                    Optional<PositionGPS> lastPositionOpt = positionGPSRepository.findTopByVehiculeIdOrderByTimestampDesc(v.getId());
                    List<AlertResponseDTO> activeAlerts = alertRepository.findActiveByVehiculeId(v.getId())
                            .stream()
                            .map(AlertResponseDTO::fromEntity)
                            .collect(Collectors.toList());

                    VehicleSnapshotDTO.VehicleSnapshotDTOBuilder builder = VehicleSnapshotDTO.builder()
                            .id(v.getId())
                            .immatriculation(v.getImmatriculation())
                            .type(v.getType())
                            .ligne(v.getLigne())
                            .statut(v.getStatut())
                            .conducteurId(v.getConducteurId())
                            .alertesActives(activeAlerts);

                    if (lastPositionOpt.isPresent()) {
                        PositionGPS lastPosition = lastPositionOpt.get();
                        builder.latitude(lastPosition.getLatitude())
                               .longitude(lastPosition.getLongitude())
                               .vitesse(lastPosition.getVitesse())
                               .cap(lastPosition.getCap())
                               .timestamp(lastPosition.getTimestamp());
                    }

                    return builder.build();
                })
                .collect(Collectors.toList());
    }
}
