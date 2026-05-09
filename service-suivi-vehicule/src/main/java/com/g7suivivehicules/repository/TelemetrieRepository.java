package com.g7suivivehicules.repository;

import com.g7suivivehicules.entity.Telemetrie;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TelemetrieRepository extends JpaRepository<Telemetrie, UUID> {

    // Dernière télémétrie d'un véhicule (température, carburant, vitesse)
    @Query("SELECT t FROM Telemetrie t WHERE t.vehiculeId = :vehiculeId ORDER BY t.timestamp DESC LIMIT 1")
    Optional<Telemetrie> findLatestByVehiculeId(@Param("vehiculeId") UUID vehiculeId);

    // N dernières télémétries — calcul d'accélération (freinage brusque)
    @Query("SELECT t FROM Telemetrie t WHERE t.vehiculeId = :vehiculeId ORDER BY t.timestamp DESC")
    List<Telemetrie> findTopNByVehiculeId(
            @Param("vehiculeId") UUID vehiculeId,
            Pageable pageable);

    List<Telemetrie> findByVehiculeIdOrderByTimestampDesc(UUID vehiculeId);
}
