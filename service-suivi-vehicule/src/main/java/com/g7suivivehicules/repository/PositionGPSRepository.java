package com.g7suivivehicules.repository;

import com.g7suivivehicules.entity.PositionGPS;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PositionGPSRepository extends JpaRepository<PositionGPS, UUID> {

    // N dernières positions d'un véhicule — utilisé pour détecter l'immobilisation
    @Query("SELECT p FROM PositionGPS p WHERE p.vehiculeId = :vehiculeId ORDER BY p.timestamp DESC")
    List<PositionGPS> findTopNByVehiculeId(
            @Param("vehiculeId") UUID vehiculeId,
            Pageable pageable);

    // Toutes les positions d'un véhicule, tri chronologique inverse
    List<PositionGPS> findByVehiculeIdOrderByTimestampDesc(UUID vehiculeId);

    // Dernière position connue d'un véhicule
    @Query("SELECT p FROM PositionGPS p WHERE p.vehiculeId = :vehiculeId ORDER BY p.timestamp DESC LIMIT 1")
    java.util.Optional<PositionGPS> findLatestByVehiculeId(@Param("vehiculeId") UUID vehiculeId);
}
