package com.g7suivivehicules.service;

import com.g7suivivehicules.dto.PositionGPSRequest;
import com.g7suivivehicules.dto.PositionGPSResponse;
import com.g7suivivehicules.entity.PositionGPS;
import jakarta.persistence.EntityNotFoundException;
import com.g7suivivehicules.repository.PositionGPSRepository;
import com.g7suivivehicules.repository.VehiculeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PositionService {

        private final PositionGPSRepository positionRepository;
        private final VehiculeRepository vehiculeRepository;
        private final AnomalyDetectionService anomalyDetectionService;
        private final com.g7suivivehicules.kafka.KafkaProducerService kafkaProducerService;

        // ================================
        // Enregistrer une position GPS
        // ================================
        public PositionGPSResponse enregistrerPosition(PositionGPSRequest request) {

                PositionGPS position = PositionGPS.builder()
                                .vehiculeId(request.getVehiculeId())
                                .latitude(request.getLatitude())
                                .longitude(request.getLongitude())
                                .vitesse(request.getVitesse())
                                .cap(request.getCap())
                                .timestamp(LocalDateTime.now())
                                .build();

                PositionGPS saved = positionRepository.save(position);

                // Récupération de la ligne du véhicule pour G4
                String ligneId = vehiculeRepository.findById(request.getVehiculeId())
                                .map(v -> v.getLigne())
                                .orElse("NON_ASSIGNE");

                // Publier sur Kafka pour G4 (Suivi temps réel)
                kafkaProducerService.envoyerPositionG4(saved, ligneId);

                // Déclenchement de la détection d'anomalies
                anomalyDetectionService.detecterAnomalies(saved, null);

                log.info("Position enregistree et publiee sur Kafka pour vehicule {}", request.getVehiculeId());
                return toResponse(saved);
        }

        // ================================
        // Toutes les positions
        // ================================
        public List<PositionGPSResponse> getToutesLesPositions() {
                return positionRepository.findAllByOrderByTimestampDesc()
                                .stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        // ================================
        // Position actuelle d'un vehicule
        // ================================
        public PositionGPSResponse getPositionActuelle(UUID vehiculeId) {
                PositionGPS position = positionRepository
                                .findTopByVehiculeIdOrderByTimestampDesc(vehiculeId)
                                .orElseThrow(() -> new EntityNotFoundException("Aucune position trouvée pour ce véhicule"));
                return toResponse(position);
        }

        // ================================
        // Historique d'un vehicule
        // ================================
        public List<PositionGPSResponse> getHistorique(UUID vehiculeId) {
                return positionRepository
                                .findByVehiculeIdOrderByTimestampDesc(vehiculeId)
                                .stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        // ================================
        // Vitesse moyenne
        // ================================
        public Double calculerVitesseMoyenne(UUID vehiculeId) {
                List<PositionGPS> positions = positionRepository
                                .findByVehiculeIdOrderByTimestampDesc(vehiculeId);

                if (positions.isEmpty())
                        return 0.0;

                return positions.stream()
                                .filter(p -> p.getVitesse() != null)
                                .mapToDouble(PositionGPS::getVitesse)
                                .average()
                                .orElse(0.0);
        }

        // ================================
        // Calcul retard (comparaison timestamp)
        // ================================
        public Long calculerRetard(UUID vehiculeId) {
                PositionGPS derniere = positionRepository
                                .findTopByVehiculeIdOrderByTimestampDesc(vehiculeId)
                                .orElseThrow(() -> new EntityNotFoundException("Aucune position trouvée pour ce véhicule"));

                // Retard en secondes depuis derniere position
                long retard = java.time.Duration.between(
                                derniere.getTimestamp(),
                                LocalDateTime.now()).getSeconds();

                return retard;
        }

        // ================================
        // Supprimer historique
        // ================================
        public void supprimerHistorique(UUID vehiculeId) {
                List<PositionGPS> positions = positionRepository
                                .findByVehiculeIdOrderByTimestampDesc(vehiculeId);
                positionRepository.deleteAll(positions);
                log.info("Historique supprime pour vehicule {}", vehiculeId);
        }

        // ================================
        // Mapper entite -> DTO
        // ================================
        private PositionGPSResponse toResponse(PositionGPS position) {
                return PositionGPSResponse.builder()
                                .id(position.getId())
                                .vehiculeId(position.getVehiculeId())
                                .latitude(position.getLatitude())
                                .longitude(position.getLongitude())
                                .timestamp(position.getTimestamp())
                                .vitesse(position.getVitesse())
                                .cap(position.getCap())
                                .build();
        }
}