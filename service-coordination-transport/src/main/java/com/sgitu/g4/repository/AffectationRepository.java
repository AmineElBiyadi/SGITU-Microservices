package com.sgitu.g4.repository;

import com.sgitu.g4.entity.AffectationVehiculeLigne;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AffectationRepository extends JpaRepository<AffectationVehiculeLigne, Long> {

	List<AffectationVehiculeLigne> findByVehiculeIdOrderByDateDebutDesc(String vehiculeId);
}
