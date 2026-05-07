package com.serviceabonnement.repository;

import com.serviceabonnement.entity.PlanAbonnement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanAbonnementRepository extends JpaRepository<PlanAbonnement, Long> {
}
