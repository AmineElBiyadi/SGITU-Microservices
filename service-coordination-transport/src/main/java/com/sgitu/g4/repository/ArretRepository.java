package com.sgitu.g4.repository;

import com.sgitu.g4.entity.Arret;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArretRepository extends JpaRepository<Arret, Long> {

	List<Arret> findByLigneIdOrderByNomAsc(Long ligneId);
}
