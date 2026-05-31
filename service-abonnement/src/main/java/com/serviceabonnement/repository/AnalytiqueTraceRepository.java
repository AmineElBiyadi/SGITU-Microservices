package com.serviceabonnement.repository;

import com.serviceabonnement.entity.AnalytiqueTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalytiqueTraceRepository extends JpaRepository<AnalytiqueTrace, Long> {
}
