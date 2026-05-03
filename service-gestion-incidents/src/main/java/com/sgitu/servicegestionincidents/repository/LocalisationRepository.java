package com.sgitu.servicegestionincidents.repository;

import com.sgitu.servicegestionincidents.model.entity.Localisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocalisationRepository extends JpaRepository<Localisation, Long> {

    List<Localisation> findByCommune(String commune);
}
