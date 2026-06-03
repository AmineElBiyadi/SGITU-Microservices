package com.sgitu.g4.repository;

import com.sgitu.g4.entity.AffectationVehiculeLigne;
import com.sgitu.g4.entity.StatutAffectation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AffectationRepository extends JpaRepository<AffectationVehiculeLigne, Long> {

	List<AffectationVehiculeLigne> findByVehiculeIdOrderByDateDebutDesc(String vehiculeId);

	boolean existsByVehiculeIdAndLigne_IdAndStatut(String vehiculeId, Long ligneId, StatutAffectation statut);

	boolean existsByVehiculeIdAndStatut(String vehiculeId, StatutAffectation statut);
}
