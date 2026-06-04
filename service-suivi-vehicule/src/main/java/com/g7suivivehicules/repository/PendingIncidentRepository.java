package com.g7suivivehicules.repository;

import com.g7suivivehicules.entity.AlertStatus;
import com.g7suivivehicules.entity.PendingIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PendingIncidentRepository extends JpaRepository<PendingIncident, UUID> {
    List<PendingIncident> findByStatus(AlertStatus status);
}
