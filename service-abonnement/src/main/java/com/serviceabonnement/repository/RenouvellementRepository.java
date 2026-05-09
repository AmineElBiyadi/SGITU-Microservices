package com.serviceabonnement.repository;

import com.serviceabonnement.entity.Renouvellement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RenouvellementRepository extends JpaRepository<Renouvellement, Long> {
    List<Renouvellement> findByAbonnementId(Long abonnementId);
}
