package com.sgitu.servicegestionincidents.repository;

import com.sgitu.servicegestionincidents.model.entity.Preuve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreuveRepository extends JpaRepository<Preuve, Long> {

    List<Preuve> findByIncidentId(Long incidentId);
}
