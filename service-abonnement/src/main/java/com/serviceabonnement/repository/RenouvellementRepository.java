package com.serviceabonnement.repository;

import com.serviceabonnement.entity.Renouvellement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RenouvellementRepository extends JpaRepository<Renouvellement, Long> {
    List<Renouvellement> findByAbonnementId(Long abonnementId);
    List<Renouvellement> findByStatut(com.serviceabonnement.enums.StatutRenouvellement statut);
    java.util.Optional<Renouvellement> findByPaiementId(String paiementId);
    java.util.Optional<Renouvellement> findFirstByAbonnementIdAndStatutOrderByDateRenouvellementDesc(Long abonnementId, com.serviceabonnement.enums.StatutRenouvellement statut);
}
