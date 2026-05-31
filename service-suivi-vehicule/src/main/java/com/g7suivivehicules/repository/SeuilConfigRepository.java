package com.g7suivivehicules.repository;

import com.g7suivivehicules.entity.SeuilConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeuilConfigRepository extends JpaRepository<SeuilConfig, UUID> {

    Optional<SeuilConfig> findByCle(String cle);

    boolean existsByCle(String cle);
}
