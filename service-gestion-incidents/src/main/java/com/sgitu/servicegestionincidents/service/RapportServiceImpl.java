package com.sgitu.servicegestionincidents.service;

import com.sgitu.servicegestionincidents.dto.response.RapportDTO;
import com.sgitu.servicegestionincidents.model.entity.Incident;
import com.sgitu.servicegestionincidents.model.enums.NiveauGravite;
import com.sgitu.servicegestionincidents.model.enums.StatutIncident;
import com.sgitu.servicegestionincidents.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RapportServiceImpl implements RapportService {

    private final IncidentRepository incidentRepository;

    /**
     * Résout la date de début de la période à partir d'un identifiant textuel.
     * Valeurs acceptées : "jour", "semaine", "mois", "trimestre", "annee".
     * Par défaut (inconnu) : retourne null → tous les incidents.
     */
    private LocalDateTime resoudreDateDebut(String periode) {
        if (periode == null) return null;
        return switch (periode.toLowerCase().trim()) {
            case "jour"      -> LocalDateTime.now().minusDays(1);
            case "semaine"   -> LocalDateTime.now().minusWeeks(1);
            case "mois"      -> LocalDateTime.now().minusMonths(1);
            case "trimestre" -> LocalDateTime.now().minusMonths(3);
            case "annee"     -> LocalDateTime.now().minusYears(1);
            default -> {
                log.warn("Période inconnue '{}' — rapport généré sur l'ensemble des incidents.", periode);
                yield null;
            }
        };
    }

    @Override
    @Transactional(readOnly = true)
    public RapportDTO genererRapport(String periode) {
        log.info("Génération du rapport pour la période : {}", periode);

        // Récupération unique filtrée par période
        LocalDateTime dateDebut = resoudreDateDebut(periode);
        List<Incident> incidents = (dateDebut != null)
                ? incidentRepository.findByDateSignalementAfter(dateDebut)
                : incidentRepository.findAll();

        // Statistiques calculées en un seul passage
        Map<String, Long> parStatut = incidents.stream()
                .collect(Collectors.groupingBy(i -> i.getStatut().name(), Collectors.counting()));

        Map<String, Long> parType = incidents.stream()
                .collect(Collectors.groupingBy(i -> i.getType().name(), Collectors.counting()));

        Map<String, Long> parGravite = incidents.stream()
                .collect(Collectors.groupingBy(i -> i.getGravite().name(), Collectors.counting()));

        Map<String, Object> statistiques = new HashMap<>();
        statistiques.put("parStatut", parStatut);
        statistiques.put("parType", parType);
        statistiques.put("parGravite", parGravite);

        return RapportDTO.builder()
                .periode(periode)
                .nbIncidents(incidents.size())
                .statistiques(statistiques)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> genererTableauBord() {
        log.info("Génération du tableau de bord");

        // Un seul appel en base — tout calculé en mémoire
        List<Incident> tous = incidentRepository.findAll();

        long total              = tous.size();
        long nouveaux           = tous.stream().filter(i -> i.getStatut() == StatutIncident.NOUVEAU).count();
        long enAnalyse          = tous.stream().filter(i -> i.getStatut() == StatutIncident.ANALYSE).count();
        long assignes           = tous.stream().filter(i -> i.getStatut() == StatutIncident.ASSIGNE).count();
        long enCours            = tous.stream().filter(i -> i.getStatut() == StatutIncident.EN_TRAITEMENT).count();
        long resolus            = tous.stream().filter(i -> i.getStatut() == StatutIncident.RESOLU).count();
        long clotures           = tous.stream().filter(i -> i.getStatut() == StatutIncident.CLOTURE).count();
        long escalades          = tous.stream().filter(Incident::isEscalade).count();
        long demandesEnAttente  = tous.stream().filter(Incident::isDemandeEscalade).count();
        long critiques          = tous.stream().filter(i -> i.getGravite() == NiveauGravite.CRITIQUE).count();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalIncidents",         total);
        dashboard.put("nouveauxIncidents",       nouveaux);
        dashboard.put("incidentsEnAnalyse",      enAnalyse);
        dashboard.put("incidentsAssignes",       assignes);
        dashboard.put("incidentsEnCours",        enCours);
        dashboard.put("incidentsResolus",        resolus);
        dashboard.put("incidentsClotures",       clotures);
        dashboard.put("incidentsEscalades",      escalades);
        dashboard.put("demandesEscaladeEnAttente", demandesEnAttente);
        dashboard.put("incidentsCritiques",      critiques);
        dashboard.put("timestamp",               LocalDateTime.now());

        return dashboard;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenirStatsParResponsable(Long responsableId) {
        log.info("Génération des statistiques d'interventions pour l'agent terrain : {}", responsableId);

        List<Incident> incidents = incidentRepository.trouverIncidentsAffectes(responsableId);

        long total = incidents.size();
        long resolus = incidents.stream()
                .filter(i -> i.getStatut() == StatutIncident.RESOLU || i.getStatut() == StatutIncident.CLOTURE)
                .count();
        long escalades = incidents.stream()
                .filter(Incident::isEscalade)
                .count();
        long enCours = incidents.stream()
                .filter(i -> i.getStatut() == StatutIncident.ASSIGNE || i.getStatut() == StatutIncident.EN_TRAITEMENT)
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("responsableId", responsableId);
        stats.put("total", total);
        stats.put("resolus", resolus);
        stats.put("escalades", escalades);
        stats.put("enCours", enCours);

        return stats;
    }
}
