package com.serviceabonnement.repository;

import com.serviceabonnement.entity.Abonnement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbonnementRepository extends JpaRepository<Abonnement, Long> {
    List<Abonnement> findByUserId(Long userId);
    Page<Abonnement> findByUserId(Long userId, Pageable pageable);
    java.util.Optional<Abonnement> findByPaiementId(String paiementId);
    java.util.Optional<Abonnement> findByRemboursementId(String remboursementId);
}
