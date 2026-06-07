package com.g7suivivehicules.repository;

import com.g7suivivehicules.entity.AlertStatus;
import com.g7suivivehicules.entity.PendingAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PendingAlertRepository extends JpaRepository<PendingAlert, UUID> {
    List<PendingAlert> findByStatus(AlertStatus status);
}
