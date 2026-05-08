package com.sgitu.servicegestionincidents.repository;

import com.sgitu.servicegestionincidents.model.entity.Incident;
import com.sgitu.servicegestionincidents.model.enums.NiveauGravite;
import com.sgitu.servicegestionincidents.model.enums.StatutIncident;
import com.sgitu.servicegestionincidents.model.enums.TypeIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

        Optional<Incident> findByReference(String reference);

        List<Incident> findByStatut(StatutIncident statut);

        List<Incident> findByGravite(NiveauGravite gravite);

        List<Incident> findByDeclarantId(Long declarantId);

        List<Incident> findByDateSignalementBetween(LocalDateTime debut, LocalDateTime fin);

        /**
         * Task 1.8 — Détection de doublon par vehiculeId.
         * Recherche un incident non résolu (statut != RESOLU, CLOTURE, ANNULE) pour un
         * véhicule donné.
         */
        @Query("SELECT i FROM Incident i WHERE i.vehiculeId = :vehiculeId " +
                        "AND i.statut NOT IN (com.sgitu.servicegestionincidents.model.enums.StatutIncident.RESOLU, " +
                        "com.sgitu.servicegestionincidents.model.enums.StatutIncident.CLOTURE, " +
                        "com.sgitu.servicegestionincidents.model.enums.StatutIncident.ANNULE) " +
                        "ORDER BY i.dateSignalement DESC " +
                        "LIMIT 1")
        Optional<Incident> trouverIncidentNonResoluParVehicule(@Param("vehiculeId") String vehiculeId);

        /**
         * Détection de doublon par localisation et type (pour incidents sans
         * vehiculeId).
         * Utilise la formule de Haversine pour calculer la distance en mètres.
         */
        @Query("SELECT i FROM Incident i JOIN i.localisation l " +
                        "WHERE i.type = :type " +
                        "AND i.statut NOT IN (com.sgitu.servicegestionincidents.model.enums.StatutIncident.RESOLU, " +
                        "com.sgitu.servicegestionincidents.model.enums.StatutIncident.CLOTURE, " +
                        "com.sgitu.servicegestionincidents.model.enums.StatutIncident.ANNULE) " +
                        "AND (6371000 * acos(cos(radians(:lat)) * cos(radians(l.latitude)) * " +
                        "cos(radians(l.longitude) - radians(:lng)) + " +
                        "sin(radians(:lat)) * sin(radians(l.latitude)))) < :radiusMeters " +
                        "ORDER BY i.dateSignalement DESC " +
                        "LIMIT 1")
        Optional<Incident> trouverIncidentNonResoluParLocalisationEtType(
                        @Param("type") TypeIncident type,
                        @Param("lat") Double latitude,
                        @Param("lng") Double longitude,
                        @Param("radiusMeters") Double radiusMeters);

        /**
         * Filtrage par statut et déclarant combinés.
         */
        List<Incident> findByStatutAndDeclarantId(StatutIncident statut, Long declarantId);
}
