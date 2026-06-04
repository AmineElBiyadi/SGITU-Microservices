package com.g7suivivehicules.service;

import com.g7suivivehicules.dto.G4ArretResponse;
import com.g7suivivehicules.dto.G4HoraireResponse;
import com.g7suivivehicules.dto.G4TrajetResponse;
import com.g7suivivehicules.util.GeoUtils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Service pour interagir avec l'API REST du Groupe 4 (Coordination).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class G4IntegrationService {

    private final RestTemplate restTemplate;

    @Value("${g4.integration.url:http://api-gateway:8080/api/g4}")
    private String g4Url;

    /**
     * Calcule la déviation en mètres par rapport au tracé théorique de la ligne.
     * Appelle GET /api/v1/lignes/{id}/trajet
     */
    @CircuitBreaker(name = "g4IntegrationService", fallbackMethod = "verifierDeviationItineraireFallback")
    @Retry(name = "g4IntegrationService")
    public double verifierDeviationItineraire(String ligneId, Double latitudeCourante, Double longitudeCourante) {
        log.debug("[G4Integration] Real check for deviation: Ligne={}, Lat={}, Lon={}", ligneId, latitudeCourante, longitudeCourante);
        
        Long id;
        try {
            id = Long.parseLong(ligneId);
        } catch (NumberFormatException e) {
            log.warn("[G4Integration] Invalid non-numeric ligneId: {}. Falling back to simulation.", ligneId);
            return verifierDeviationItineraireFallback(ligneId, latitudeCourante, longitudeCourante, e);
        }

        String url = g4Url + "/v1/lignes/" + id + "/trajet";
        HttpHeaders headers = buildHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<G4TrajetResponse[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                G4TrajetResponse[].class
        );

        G4TrajetResponse[] trajets = response.getBody();
        if (trajets == null || trajets.length == 0) {
            log.warn("[G4Integration] Empty trajet from G4 for ligneId: {}", id);
            return 0.0;
        }

        // Find the minimum distance to any stop in all active trajets of this line
        double minDistance = Double.MAX_VALUE;
        boolean hasStops = false;
        
        for (G4TrajetResponse trajet : trajets) {
            if (trajet.isActif() && trajet.getArrets() != null) {
                for (G4ArretResponse arret : trajet.getArrets()) {
                    if (arret.getLatitude() != null && arret.getLongitude() != null) {
                        hasStops = true;
                        double distance = GeoUtils.calculerDistanceMetres(
                                latitudeCourante,
                                longitudeCourante,
                                arret.getLatitude().doubleValue(),
                                arret.getLongitude().doubleValue()
                        );
                        if (distance < minDistance) {
                            minDistance = distance;
                        }
                    }
                }
            }
        }

        if (!hasStops) {
            log.warn("[G4Integration] No valid stops found in trajets for ligneId: {}", id);
            return 0.0;
        }

        log.debug("[G4Integration] Minimum distance to trajectory stops: {} meters", minDistance);
        return minDistance;
    }

    /**
     * Calcule le retard en minutes par rapport à l'horaire prévu à un arrêt.
     * Appelle GET /api/v1/lignes/{id}/horaires
     */
    @CircuitBreaker(name = "g4IntegrationService", fallbackMethod = "verifierRetardHoraireFallback")
    @Retry(name = "g4IntegrationService")
    public int verifierRetardHoraire(String ligneId, UUID arretId, LocalDateTime heurePassageReelle) {
        log.debug("[G4Integration] Real check for retard: Ligne={}, Arret={}, Time={}", ligneId, arretId, heurePassageReelle);

        Long id;
        try {
            id = Long.parseLong(ligneId);
        } catch (NumberFormatException e) {
            log.warn("[G4Integration] Invalid non-numeric ligneId: {}. Falling back to simulation.", ligneId);
            return verifierRetardHoraireFallback(ligneId, arretId, heurePassageReelle, e);
        }

        String url = g4Url + "/v1/lignes/" + id + "/horaires";
        HttpHeaders headers = buildHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<G4HoraireResponse[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                G4HoraireResponse[].class
        );

        G4HoraireResponse[] horaires = response.getBody();
        if (horaires == null || horaires.length == 0) {
            log.warn("[G4Integration] Empty schedules from G4 for ligneId: {}", id);
            return 0;
        }

        LocalTime passageTime = heurePassageReelle.toLocalTime();
        int dayOfWeek = heurePassageReelle.getDayOfWeek().getValue(); // 1 = Mon, 7 = Sun

        // Find the scheduled horaire closest to passageTime on the same day of week
        G4HoraireResponse closest = null;
        long minDiffMinutes = Long.MAX_VALUE;

        for (G4HoraireResponse horaire : horaires) {
            if (horaire.getJourSemaine() == null || horaire.getJourSemaine() == dayOfWeek) {
                if (horaire.getHeurePassage() != null) {
                    long diff = java.time.Duration.between(horaire.getHeurePassage(), passageTime).toMinutes();
                    long absDiff = Math.abs(diff);
                    if (absDiff < minDiffMinutes) {
                        minDiffMinutes = absDiff;
                        closest = horaire;
                    }
                }
            }
        }

        if (closest == null) {
            return 0;
        }

        // If actual passage time is AFTER scheduled time, we have a delay
        long delay = java.time.Duration.between(closest.getHeurePassage(), passageTime).toMinutes();
        int delayMinutes = delay > 0 ? (int) delay : 0;

        log.debug("[G4Integration] Delay calculated for ligneId {}: {} minutes (Closest schedule: {})", 
                id, delayMinutes, closest.getHeurePassage());
        return delayMinutes;
    }

    /**
     * Fallback pour le calcul de déviation en cas d'erreur ou d'indisponibilité de G4.
     */
    public double verifierDeviationItineraireFallback(String ligneId, Double latitudeCourante, Double longitudeCourante, Throwable t) {
        log.warn("[G4Integration] Fallback activated for deviation check on ligne {} (G4 unavailable or error: {})", ligneId, t.getMessage());
        return 20.0; // Return a small safe value to avoid false alerts when G4 is down
    }

    /**
     * Fallback pour le calcul de retard en cas d'erreur ou d'indisponibilité de G4.
     */
    public int verifierRetardHoraireFallback(String ligneId, UUID arretId, LocalDateTime heurePassageReelle, Throwable t) {
        log.warn("[G4Integration] Fallback activated for retard check on ligne {} (G4 unavailable or error: {})", ligneId, t.getMessage());
        return 0; // Return 0 delay to avoid false alerts when G4 is down
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            if (request != null) {
                String jwt = request.getHeader("Authorization");
                if (jwt != null) {
                    headers.set("Authorization", jwt);
                }
                String email = request.getHeader("X-User-Email");
                if (email != null) {
                    headers.set("X-User-Email", email);
                }
                String userId = request.getHeader("X-User-Id");
                if (userId != null) {
                    headers.set("X-User-Id", userId);
                }
                String roles = request.getHeader("X-Roles");
                if (roles != null) {
                    headers.set("X-Roles", roles);
                }
            }
        }
        
        // In case no active request (e.g. background/test context), set a default auth header if none is present
        if (!headers.containsKey("Authorization")) {
            headers.set("Authorization", "Bearer mock-system-g7-token");
        }
        
        return headers;
    }
}
