package com.sgitu.g4.repository;

import com.sgitu.g4.entity.StatutVehiculeG7;
import com.sgitu.g4.entity.VehiculeReferentiel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehiculeReferentielRepository extends JpaRepository<VehiculeReferentiel, String> {

	List<VehiculeReferentiel> findByDisponiblePourAffectationTrueOrderByRegisteredAtDesc();

	List<VehiculeReferentiel> findByStatutG7OrderByRegisteredAtDesc(StatutVehiculeG7 statutG7);
}
