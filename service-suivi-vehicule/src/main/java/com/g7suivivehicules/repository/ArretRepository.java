package com.g7suivivehicules.repository;

import com.g7suivivehicules.entity.Arret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArretRepository extends JpaRepository<Arret, UUID> {

    // ========== PROXIMITÉ GPS (formule Haversine, rayon en mètres) ==========
    // Trouve tous les arrêts dans un rayon donné autour d'une position
    // Utilisé pour distinguer immobilisation normale vs anomalie
    @Query(value = """
        SELECT * FROM arrets a
        WHERE a.latitude IS NOT NULL
          AND a.longitude IS NOT NULL
          AND (6371000 * acos(
              LEAST(1.0,
                cos(radians(:latitude)) * cos(radians(a.latitude)) *
                cos(radians(a.longitude) - radians(:longitude)) +
                sin(radians(:latitude)) * sin(radians(a.latitude))
              )
          )) <= :rayonMetres
        ORDER BY (6371000 * acos(
              LEAST(1.0,
                cos(radians(:latitude)) * cos(radians(a.latitude)) *
                cos(radians(a.longitude) - radians(:longitude)) +
                sin(radians(:latitude)) * sin(radians(a.latitude))
              )
          )) ASC
        """, nativeQuery = true)
    List<Arret> findArretsDansRayon(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("rayonMetres") Double rayonMetres);

    // ========== PRÉSENCE VEHICULE À UN ARRÊT ==========
    // Vérifie si un véhicule est marqué comme présent dans un arrêt actuellement
    Optional<Arret> findFirstByVehiculeIdAndPresentTrue(UUID vehiculeId);

    List<Arret> findByVehiculeIdAndPresentTrue(UUID vehiculeId);

    // Arrêts par arretId de référence G4
    List<Arret> findByArretId(UUID arretId);
}
