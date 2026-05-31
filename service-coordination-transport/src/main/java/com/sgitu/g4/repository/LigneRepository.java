package com.sgitu.g4.repository;

import com.sgitu.g4.entity.Ligne;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LigneRepository extends JpaRepository<Ligne, Long> {

	Optional<Ligne> findByCodeIgnoreCase(String code);

	List<Ligne> findByActiveTrue();

	boolean existsByCodeIgnoreCase(String code);
}
