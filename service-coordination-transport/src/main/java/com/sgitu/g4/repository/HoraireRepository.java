package com.sgitu.g4.repository;

import com.sgitu.g4.entity.Horaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HoraireRepository extends JpaRepository<Horaire, Long> {

	List<Horaire> findByTrajetIdOrderByHeurePassageAsc(Long trajetId);

	List<Horaire> findByTrajetLigneIdOrderByHeurePassageAsc(Long ligneId);
}
