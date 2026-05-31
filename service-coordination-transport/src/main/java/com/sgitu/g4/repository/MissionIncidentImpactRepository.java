package com.sgitu.g4.repository;

import com.sgitu.g4.entity.MissionIncidentImpact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MissionIncidentImpactRepository extends JpaRepository<MissionIncidentImpact, Long> {

	List<MissionIncidentImpact> findAllByOrderByOccurredAtDesc();

	List<MissionIncidentImpact> findByMissionIdOrderByOccurredAtDesc(Long missionId);

	Optional<MissionIncidentImpact> findByG9ReferenceIncident(String g9ReferenceIncident);
}
