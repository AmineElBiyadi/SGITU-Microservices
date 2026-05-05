package com.g7suivivehicules.service;

import com.g7suivivehicules.entity.Alert.Severite;
import com.g7suivivehicules.entity.Alert.TypeAlert;
import com.g7suivivehicules.entity.Arret;
import com.g7suivivehicules.entity.PositionGPS;
import com.g7suivivehicules.entity.Telemetrie;
import com.g7suivivehicules.repository.ArretRepository;
import com.g7suivivehicules.repository.PositionGPSRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Service de détection d'anomalies — cœur de la partie G7 suivi véhicule.
 *
 * Appelé par Personne 1 (service GPS/Télémétrie) après chaque enregistrement
 * de position GPS, avec la télémétrie associée.
 *
 * Les seuils sont lus via SeuilConfigService (DB + cache) — modifiables à chaud.
 *
 * Ordre de vérification :
 *   1. Vitesse excessive
 *   2. Température critique
 *   3. Carburant critique
 *   4. Freinage brusque
 *   5. Immobilisation anormale (avec vérification arrêt prévu)
 *   6. Si aucune anomalie → résolution automatique des alertes ouvertes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private final AlertService alertService;
    private final SeuilConfigService seuilConfigService;
    private final ArretRepository arretRepository;
    private final PositionGPSRepository positionGPSRepository;

    // ========== POINT D'ENTRÉE PRINCIPAL ==========

    /**
     * Lance toutes les vérifications d'anomalies pour un véhicule donné.
     *
     * @param position   la dernière position GPS enregistrée
     * @param telemetrie la dernière télémétrie associée (peut être null si indisponible)
     */
    public void detecterAnomalies(PositionGPS position, Telemetrie telemetrie) {
        UUID vehiculeId = position.getVehiculeId();
        Double latitude  = position.getLatitude();
        Double longitude = position.getLongitude();

        log.debug("[AnomalyDetection] Analyse vehiculeId={} lat={} lon={}", vehiculeId, latitude, longitude);

        boolean anomalieDetectee = false;

        // ── 1. Vitesse excessive ──────────────────────────────────────────────
        Double seuilVitesse = seuilConfigService.getVitesseMax();
        Double vitesse = position.getVitesse();
        if (vitesse != null && vitesse > seuilVitesse) {
            anomalieDetectee = true;
            alertService.creerOuMettreAJour(
                    vehiculeId,
                    TypeAlert.VITESSE_EXCESSIVE,
                    latitude, longitude,
                    vitesse, seuilVitesse,
                    vitesse >= seuilVitesse * 1.5 ? Severite.CRITIQUE : Severite.HAUTE,
                    String.format("Vitesse excessive : %.1f km/h (seuil %.1f km/h)", vitesse, seuilVitesse)
            );
        } else {
            alertService.resoudreAutomatiquement(vehiculeId, TypeAlert.VITESSE_EXCESSIVE);
        }

        if (telemetrie != null) {

            // ── 2. Température critique ───────────────────────────────────────
            Double seuilTemp = seuilConfigService.getTemperatureCritique();
            Double temperature = telemetrie.getTemperature();
            if (temperature != null && temperature > seuilTemp) {
                anomalieDetectee = true;
                alertService.creerOuMettreAJour(
                        vehiculeId,
                        TypeAlert.TEMPERATURE_CRITIQUE,
                        latitude, longitude,
                        temperature, seuilTemp,
                        temperature >= seuilTemp * 1.2 ? Severite.CRITIQUE : Severite.HAUTE,
                        String.format("Température critique : %.1f°C (seuil %.1f°C)", temperature, seuilTemp)
                );
            } else {
                alertService.resoudreAutomatiquement(vehiculeId, TypeAlert.TEMPERATURE_CRITIQUE);
            }

            // ── 3. Carburant critique ─────────────────────────────────────────
            Double seuilCarburant = seuilConfigService.getCarburantCritique();
            Double carburant = telemetrie.getCarburant();
            if (carburant != null && carburant < seuilCarburant) {
                anomalieDetectee = true;
                alertService.creerOuMettreAJour(
                        vehiculeId,
                        TypeAlert.CARBURANT_CRITIQUE,
                        latitude, longitude,
                        carburant, seuilCarburant,
                        carburant <= seuilCarburant / 2 ? Severite.CRITIQUE : Severite.HAUTE,
                        String.format("Carburant critique : %.1f%% (seuil %.1f%%)", carburant, seuilCarburant)
                );
            } else {
                alertService.resoudreAutomatiquement(vehiculeId, TypeAlert.CARBURANT_CRITIQUE);
            }
        }

        // ── 4. Freinage brusque ───────────────────────────────────────────────
        Double seuilFreinage = seuilConfigService.getFreinageDeceleration();
        double deceleration = calculerDeceleration(vehiculeId, vitesse);
        if (deceleration < seuilFreinage) {
            anomalieDetectee = true;
            alertService.creerOuMettreAJour(
                    vehiculeId,
                    TypeAlert.FREINAGE_BRUSQUE,
                    latitude, longitude,
                    deceleration, seuilFreinage,
                    deceleration <= seuilFreinage * 2 ? Severite.CRITIQUE : Severite.HAUTE,
                    String.format("Freinage brusque détecté : %.2f m/s² (seuil %.2f m/s²)",
                            deceleration, seuilFreinage)
            );
        } else {
            alertService.resoudreAutomatiquement(vehiculeId, TypeAlert.FREINAGE_BRUSQUE);
        }

        // ── 5. Immobilisation anormale ────────────────────────────────────────
        boolean immobilisationAnormale = detecterImmobilisation(vehiculeId, latitude, longitude);
        if (immobilisationAnormale) {
            anomalieDetectee = true;
            int seuilMin = seuilConfigService.getImmobilisationMin();
            alertService.creerOuMettreAJour(
                    vehiculeId,
                    TypeAlert.IMMOBILISATION,
                    latitude, longitude,
                    (double) seuilMin, (double) seuilMin,
                    Severite.MOYENNE,
                    String.format("Véhicule immobilisé depuis plus de %d minutes hors arrêt prévu", seuilMin)
            );
        } else {
            alertService.resoudreAutomatiquement(vehiculeId, TypeAlert.IMMOBILISATION);
        }

        if (!anomalieDetectee) {
            log.debug("[AnomalyDetection] Aucune anomalie pour vehiculeId={}", vehiculeId);
        }
    }

    // ========== MÉTHODES PRIVÉES D'ANALYSE ==========

    /**
     * Calcule la décélération en m/s² à partir des 2 dernières positions.
     * Retourne 0.0 si pas assez de données.
     */
    private double calculerDeceleration(UUID vehiculeId, Double vitesseCourante) {
        if (vitesseCourante == null) return 0.0;

        List<PositionGPS> dernieres = positionGPSRepository.findTopNByVehiculeId(
                vehiculeId, PageRequest.of(0, 2));

        if (dernieres.size() < 2) return 0.0;

        PositionGPS actuelle   = dernieres.get(0);
        PositionGPS precedente = dernieres.get(1);

        if (precedente.getVitesse() == null || actuelle.getVitesse() == null) return 0.0;

        // Conversion km/h → m/s
        double vActuelle   = actuelle.getVitesse() / 3.6;
        double vPrecedente = precedente.getVitesse() / 3.6;

        long secondes = Duration.between(precedente.getTimestamp(), actuelle.getTimestamp()).getSeconds();
        if (secondes <= 0) return 0.0;

        return (vActuelle - vPrecedente) / secondes; // négatif = décélération
    }

    /**
     * Détecte une immobilisation anormale :
     *  1. Récupère les N dernières positions
     *  2. Vérifie que vitesse ≈ 0 sur toutes les positions (≥ X minutes)
     *  3. Vérifie si la position est proche d'un arrêt prévu (Haversine SQL)
     *     → Proche d'un arrêt : immobilisation NORMALE, pas d'alerte
     *     → Hors arrêt : immobilisation ANORMALE → alerte
     *
     * @return true si une alerte doit être déclenchée
     */
    private boolean detecterImmobilisation(UUID vehiculeId, Double latitude, Double longitude) {
        int nbPositions = seuilConfigService.getNbPositions();
        int seuilMin    = seuilConfigService.getImmobilisationMin();

        List<PositionGPS> positions = positionGPSRepository.findTopNByVehiculeId(
                vehiculeId, PageRequest.of(0, nbPositions));

        if (positions.size() < nbPositions) {
            // Pas encore assez de données pour conclure
            return false;
        }

        // Vérifie que toutes les positions ont une vitesse ≈ 0
        boolean toutesImmobiles = positions.stream()
                .allMatch(p -> p.getVitesse() == null || p.getVitesse() < 1.0);

        if (!toutesImmobiles) return false;

        // Vérifie la durée : différence entre la plus récente et la plus ancienne
        PositionGPS laPlus_recente  = positions.get(0);
        PositionGPS laPlus_ancienne = positions.get(positions.size() - 1);
        long minutesImmobile = Duration.between(
                laPlus_ancienne.getTimestamp(),
                laPlus_recente.getTimestamp()
        ).toMinutes();

        if (minutesImmobile < seuilMin) return false;

        // ── Vérification arrêt prévu ──────────────────────────────────────────
        Double rayon = seuilConfigService.getRayonMetres();
        List<Arret> arretsProches = arretRepository.findArretsDansRayon(latitude, longitude, rayon);

        if (!arretsProches.isEmpty()) {
            log.debug("[AnomalyDetection] Immobilisation normale — vehiculeId={} à l'arrêt '{}'",
                    vehiculeId, arretsProches.get(0).getNom());
            return false;
        }

        // Immobilisation hors arrêt prévu → anomalie
        log.warn("[AnomalyDetection] Immobilisation anormale — vehiculeId={} depuis {}min hors arrêt",
                vehiculeId, minutesImmobile);
        return true;
    }
}
