package com.g7suivivehicules.repository;

import com.g7suivivehicules.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    // ========== REQUête DE DéDUPLICATION (la plus importante) ==========
    // Vérifie si une alerte OUVERTE existe déjà pour ce vehiculeId + typeAlert
    @Query("SELECT a FROM Alert a WHERE a.vehiculeId = :vehiculeId " +
           "AND a.typeAlert = :typeAlert " +
           "AND a.statut = com.g7suivivehicules.entity.Alert.StatutAlert.OUVERTE")
    Optional<Alert> findActiveByVehiculeIdAndTypeAlert(
            @Param("vehiculeId") UUID vehiculeId,
            @Param("typeAlert") Alert.TypeAlert typeAlert);

    // ========== ALERTES PAR VÉHICULE ==========
    List<Alert> findByVehiculeIdOrderByTimestampDebutDesc(UUID vehiculeId);

    @Query("SELECT a FROM Alert a WHERE a.vehiculeId = :vehiculeId " +
           "AND a.statut = com.g7suivivehicules.entity.Alert.StatutAlert.OUVERTE " +
           "ORDER BY a.timestampDebut DESC")
    List<Alert> findActiveByVehiculeId(@Param("vehiculeId") UUID vehiculeId);

    // ========== TOUTES LES ALERTES ACTIVES (= OUVERTE uniquement) ==========
    @Query("SELECT a FROM Alert a " +
           "WHERE a.statut = com.g7suivivehicules.entity.Alert.StatutAlert.OUVERTE " +
           "ORDER BY a.timestampDebut DESC")
    List<Alert> findAllActive();

    // ========== FILTRES OPTIONNELS (pour GET /api/v1/alerts) ==========
    @Query("SELECT a FROM Alert a WHERE " +
           "(:vehiculeId IS NULL OR a.vehiculeId = :vehiculeId) AND " +
           "(:statut IS NULL OR a.statut = :statut) AND " +
           "(:typeAlert IS NULL OR a.typeAlert = :typeAlert) " +
           "ORDER BY a.timestampDebut DESC")
    List<Alert> findWithFilters(
            @Param("vehiculeId") UUID vehiculeId,
            @Param("statut") Alert.StatutAlert statut,
            @Param("typeAlert") Alert.TypeAlert typeAlert);

    // ========== STATISTIQUES POUR G8 ==========
    @Query("SELECT a.typeAlert, COUNT(a) FROM Alert a GROUP BY a.typeAlert")
    List<Object[]> countByTypeAlert();

    @Query("SELECT a.statut, COUNT(a) FROM Alert a GROUP BY a.statut")
    List<Object[]> countByStatut();

    @Query("SELECT a.typeAlert, a.statut, COUNT(a) FROM Alert a GROUP BY a.typeAlert, a.statut")
    List<Object[]> countByTypeAlertAndStatut();
}
