package com.sgitu.servicegestionincidents.repository;

import com.sgitu.servicegestionincidents.model.entity.Incident;
import com.sgitu.servicegestionincidents.model.enums.NiveauGravite;
import com.sgitu.servicegestionincidents.model.enums.StatutIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    Optional<Incident> findByReference(String reference);
    List<Incident> findByStatut(StatutIncident statut);
    List<Incident> findByGravite(NiveauGravite gravite);
    List<Incident> findByDeclarantId(Long declarantId);
    List<Incident> findByDateSignalementBetween(LocalDateTime debut, LocalDateTime fin);
}
