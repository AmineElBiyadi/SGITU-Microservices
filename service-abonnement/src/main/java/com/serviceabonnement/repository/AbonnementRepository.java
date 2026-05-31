package com.serviceabonnement.repository;

import com.serviceabonnement.entity.Abonnement;
import com.serviceabonnement.enums.StatutAbonnement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AbonnementRepository extends JpaRepository<Abonnement, Long> {
    List<Abonnement> findByUserId(Long userId);

    List<Abonnement> findByStatut(StatutAbonnement statut);

    Page<Abonnement> findByUserId(Long userId, Pageable pageable);

    List<Abonnement> findByUserIdAndStatut(Long userId, StatutAbonnement statut);

    Optional<Abonnement> findByPaiementId(String paiementId);

    java.util.Optional<Abonnement> findByUserIdAndPlanIdPlanAndStatut(Long userId, Long idPlan, StatutAbonnement statut);

    Optional<Abonnement> findByRemboursementId(String remboursementId);
}
