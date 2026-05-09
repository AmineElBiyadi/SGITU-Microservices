package com.sgitu.servicegestionincidents.service;

import com.sgitu.servicegestionincidents.dto.response.RapportDTO;
import com.sgitu.servicegestionincidents.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RapportServiceImpl implements RapportService {

    private final IncidentRepository incidentRepository;

    @Override
    public RapportDTO genererRapport(String periode) {
        log.info("Génération du rapport pour la période: {}", periode);
        
        long count = incidentRepository.count();
        
        // Count by status
        java.util.Map<String, Long> countByStatut = incidentRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(i -> i.getStatut().name(), java.util.stream.Collectors.counting()));
        
        // Count by type
        java.util.Map<String, Long> countByType = incidentRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(i -> i.getType().name(), java.util.stream.Collectors.counting()));

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("parStatut", countByStatut);
        stats.put("parType", countByType);

        return RapportDTO.builder()
                .periode(periode)
                .nbIncidents((int) count)
                .statistiques(stats)
                .build();
    }

    @Override
    public java.util.Map<String, Object> genererTableauBord() {
        log.info("Génération du tableau de bord");
        
        long total = incidentRepository.count();
        long enCours = incidentRepository.findByStatut(com.sgitu.servicegestionincidents.model.enums.StatutIncident.EN_TRAITEMENT).size();
        long nouveaux = incidentRepository.findByStatut(com.sgitu.servicegestionincidents.model.enums.StatutIncident.NOUVEAU).size();
        long resolus = incidentRepository.findByStatut(com.sgitu.servicegestionincidents.model.enums.StatutIncident.RESOLU).size();

        java.util.Map<String, Object> dashboard = new java.util.HashMap<>();
        dashboard.put("totalIncidents", total);
        dashboard.put("incidentsEnCours", enCours);
        dashboard.put("nouveauxIncidents", nouveaux);
        dashboard.put("incidentsResolus", resolus);
        dashboard.put("timestamp", java.time.LocalDateTime.now());

        return dashboard;
    }
}
