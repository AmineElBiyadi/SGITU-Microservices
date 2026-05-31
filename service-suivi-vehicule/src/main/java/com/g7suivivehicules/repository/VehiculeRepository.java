package com.g7suivivehicules.repository;

import com.g7suivivehicules.entity.Vehicule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehiculeRepository extends JpaRepository<Vehicule, UUID> {
    List<Vehicule> findByStatut(Vehicule.StatutVehicule statut);
    List<Vehicule> findByType(Vehicule.TypeVehicule type);
    List<Vehicule> findByStatutIn(List<Vehicule.StatutVehicule> statuts);
}

