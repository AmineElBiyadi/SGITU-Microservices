package com.serviceabonnement.repository;

import com.serviceabonnement.entity.Desactivation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DesactivationRepository extends JpaRepository<Desactivation, Long> {
    java.util.List<Desactivation> findByAbonnementId(Long abonnementId);
}
