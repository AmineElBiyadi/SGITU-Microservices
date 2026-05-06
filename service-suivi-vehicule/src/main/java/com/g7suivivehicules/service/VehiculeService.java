package com.g7suivivehicules.service;

import com.g7suivivehicules.dto.VehiculeRequest;
import com.g7suivivehicules.dto.VehiculeResponse;
import com.g7suivivehicules.entity.Vehicule;
import com.g7suivivehicules.kafka.KafkaProducer;
import com.g7suivivehicules.repository.VehiculeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehiculeService {

    private final VehiculeRepository vehiculeRepository;
    // private final KafkaProducer kafkaProducer; // Temporairement désactivé pour test sans Kafka

    @Transactional
    public VehiculeResponse createVehicule(VehiculeRequest request) {
        log.info("Creation d'un nouveau vehicule: {}", request.getImmatriculation());
        
        Vehicule vehicule = Vehicule.builder()
                .immatriculation(request.getImmatriculation())
                .type(request.getType())
                .ligne(request.getLigne())
                .statut(Vehicule.StatutVehicule.DISPONIBLE) // Initialement disponible
                .conducteurId(request.getConducteurId())
                .build();

        Vehicule saved = vehiculeRepository.save(vehicule);
        
        // Notification Kafka (Désactivée pour test local)
        // kafkaProducer.envoyerEvenementVehicule("vehicle.registered", saved);

        return mapToResponse(saved);
    }



    public List<VehiculeResponse> getAllVehicules() {
        return vehiculeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public VehiculeResponse getVehiculeById(UUID id) {
        return vehiculeRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Vehicule non trouve"));
    }

    @Transactional
    public VehiculeResponse updateVehicule(UUID id, VehiculeRequest request) {
        Vehicule vehicule = vehiculeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicule non trouve"));

        vehicule.setImmatriculation(request.getImmatriculation());
        vehicule.setType(request.getType());
        vehicule.setLigne(request.getLigne());
        vehicule.setConducteurId(request.getConducteurId());

        return mapToResponse(vehiculeRepository.save(vehicule));
    }

    @Transactional
    public void deleteVehicule(UUID id) {
        Vehicule vehicule = vehiculeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicule non trouve"));
        vehicule.setStatut(Vehicule.StatutVehicule.HORS_SERVICE);
        vehiculeRepository.save(vehicule);
    }

    public List<VehiculeResponse> getVehiculesActifs() {
        return vehiculeRepository.findByStatut(Vehicule.StatutVehicule.EN_SERVICE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<VehiculeResponse> getVehiculesByStatut(Vehicule.StatutVehicule statut) {
        return vehiculeRepository.findByStatut(statut).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<VehiculeResponse> getVehiculesByType(Vehicule.TypeVehicule type) {
        return vehiculeRepository.findByType(type).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public VehiculeResponse updateStatut(UUID id, Vehicule.StatutVehicule statut) {
        Vehicule vehicule = vehiculeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicule non trouve"));
        vehicule.setStatut(statut);
        return mapToResponse(vehiculeRepository.save(vehicule));
    }

    private VehiculeResponse mapToResponse(Vehicule vehicule) {
        return VehiculeResponse.builder()
                .id(vehicule.getId())
                .immatriculation(vehicule.getImmatriculation())
                .type(vehicule.getType())
                .ligne(vehicule.getLigne())
                .statut(vehicule.getStatut())
                .conducteurId(vehicule.getConducteurId())
                .build();
    }
}

