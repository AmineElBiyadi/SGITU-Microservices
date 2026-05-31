package com.serviceabonnement.repository;

import com.serviceabonnement.entity.SuspensionAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuspensionAdminRepository extends JpaRepository<SuspensionAdmin, Long> {
    List<SuspensionAdmin> findByAbonnementId(Long abonnementId);
}
