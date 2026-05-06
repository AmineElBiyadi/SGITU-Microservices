package com.g7suivivehicules.repository;

import com.g7suivivehicules.entity.PositionGPS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PositionGPSRepository extends JpaRepository<PositionGPS, UUID> {

    // Derniere position d'un vehicule
    Optional<PositionGPS> findTopByVehiculeIdOrderByTimestampDesc(UUID vehiculeId);

    // Historique complet d'un vehicule
    List<PositionGPS> findByVehiculeIdOrderByTimestampDesc(UUID vehiculeId);

    // Toutes les positions
    List<PositionGPS> findAllByOrderByTimestampDesc();
}