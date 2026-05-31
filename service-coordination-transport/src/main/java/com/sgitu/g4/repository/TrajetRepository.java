package com.sgitu.g4.repository;

import com.sgitu.g4.entity.Trajet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrajetRepository extends JpaRepository<Trajet, Long> {

	List<Trajet> findByLigneIdOrderByCodeAsc(Long ligneId);

	@Query("""
			SELECT DISTINCT t FROM Trajet t
			LEFT JOIN FETCH t.arretsSequence ta
			LEFT JOIN FETCH ta.arret
			WHERE t.id = :id
			""")
	Optional<Trajet> findFetchedById(@Param("id") Long id);
}
