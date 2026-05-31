package com.sgitu.g4.repository;

import com.sgitu.g4.entity.TrajetArret;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrajetArretRepository extends JpaRepository<TrajetArret, Long> {

	List<TrajetArret> findByTrajetIdOrderBySequenceOrderAsc(Long trajetId);
}
