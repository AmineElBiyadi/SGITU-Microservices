package com.sgitu.g4.repository;

import com.sgitu.g4.entity.Mission;
import com.sgitu.g4.entity.StatutMission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {

	List<Mission> findByStatut(StatutMission statut);

	List<Mission> findByVehiculeIdOrderByCreatedAtDesc(String vehiculeId);
}
