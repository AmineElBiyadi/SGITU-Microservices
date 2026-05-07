package com.sgitu.servicegestionincidents.repository;

import com.sgitu.servicegestionincidents.model.entity.CategorieIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategorieRepository extends JpaRepository<CategorieIncident, Long> {

    List<CategorieIncident> findByActif(Boolean actif);
}
