package com.g7suivivehicules.service;

import com.g7suivivehicules.util.GeoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service pour interagir avec l'API REST du Groupe 4 (Coordination).
 */
@Slf4j
@Service
public class G4IntegrationService {

    // private final RestTemplate restTemplate;

    // public G4IntegrationService(RestTemplate restTemplate) {
    //     this.restTemplate = restTemplate;
    // }

    /**
     * Calcule la déviation en mètres par rapport au tracé théorique de la ligne.
     * En production, cette méthode appellera GET /api/v1/lignes/{id}/trajet
     */
    public double verifierDeviationItineraire(UUID ligneId, Double latitudeCourante, Double longitudeCourante) {
        // En production : Récupérer List<PointGPS> trajet = restTemplate.getForEntity(...)
        // Pour l'exemple, on imagine un point théorique (celui de G4)
        double latTheorique = latitudeCourante + 0.002; // Simule un décalage
        double lonTheorique = longitudeCourante + 0.002;

        double distance = GeoUtils.calculerDistanceMetres(latitudeCourante, longitudeCourante, latTheorique, lonTheorique);
        
        log.debug("[G4Integration] Déviation calculée pour ligne {} : {} mètres", ligneId, distance);
        return distance;
    }

    /**
     * Calcule le retard en minutes par rapport à l'horaire prévu à un arrêt.
     * En production, cette méthode appellera GET /api/v1/lignes/{id}/horaires
     */
    public int verifierRetardHoraire(UUID ligneId, UUID arretId, LocalDateTime heurePassageReelle) {
        // En production : Récupérer l'horaire prévu pour cet arrêt via G4
        LocalDateTime heurePrevue = heurePassageReelle.minusMinutes(10); // Simule un retard de 10 min

        long retardMinutes = Duration.between(heurePrevue, heurePassageReelle).toMinutes();
        
        log.debug("[G4Integration] Retard calculé pour ligne {} : {} minutes", ligneId, retardMinutes);
        return (int) retardMinutes;
    }
}
