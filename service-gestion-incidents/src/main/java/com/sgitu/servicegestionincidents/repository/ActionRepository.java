package com.sgitu.servicegestionincidents.repository;

import com.sgitu.servicegestionincidents.model.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActionRepository extends JpaRepository<Action, Long> {

    List<Action> findByIncidentIdOrderByDateActionDesc(Long incidentId);
    List<Action> findByAuteurId(Long auteurId);
    List<Action> findByDateActionBetween(LocalDateTime debut, LocalDateTime fin);
}
