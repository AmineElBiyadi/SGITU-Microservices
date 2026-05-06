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
    Optional<Alert> findByVehiculeIdAndTypeAlertAndStatut(UUID vehiculeId, Alert.TypeAlert typeAlert, Alert.StatutAlert statut);

    default Optional<Alert> findActiveByVehiculeIdAndTypeAlert(UUID vehiculeId, Alert.TypeAlert typeAlert) {
        return findByVehiculeIdAndTypeAlertAndStatut(vehiculeId, typeAlert, Alert.StatutAlert.OUVERTE);
    }

    // ========== ALERTES PAR VÉHICULE ==========
    List<Alert> findByVehiculeIdOrderByTimestampDebutDesc(UUID vehiculeId);

    List<Alert> findByVehiculeIdAndStatutOrderByTimestampDebutDesc(UUID vehiculeId, Alert.StatutAlert statut);

    default List<Alert> findActiveByVehiculeId(UUID vehiculeId) {
        return findByVehiculeIdAndStatutOrderByTimestampDebutDesc(vehiculeId, Alert.StatutAlert.OUVERTE);
    }

    // ========== TOUTES LES ALERTES ACTIVES (= OUVERTE uniquement) ==========
    List<Alert> findByStatutOrderByTimestampDebutDesc(Alert.StatutAlert statut);

    default List<Alert> findAllActive() {
        return findByStatutOrderByTimestampDebutDesc(Alert.StatutAlert.OUVERTE);
    }

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
