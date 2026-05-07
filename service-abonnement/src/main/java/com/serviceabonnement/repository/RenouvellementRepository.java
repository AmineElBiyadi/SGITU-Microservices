package com.serviceabonnement.repository;

import com.serviceabonnement.entity.Renouvellement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RenouvellementRepository extends JpaRepository<Renouvellement, Long> {
}
