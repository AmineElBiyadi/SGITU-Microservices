package com.g7suivivehicules.service;

import com.g7suivivehicules.entity.SeuilConfig;
import com.g7suivivehicules.entity.SeuilConfig.Cles;
import com.g7suivivehicules.repository.SeuilConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service de gestion des seuils d'anomalies.
 *
 * Stratégie : valeurs stockées en DB (SeuilConfig), avec cache Spring (@Cacheable).
 *
 *  - Au démarrage (@PostConstruct), si la table est vide,
 *    les valeurs par défaut de application.properties sont injectées automatiquement.
 *  - Chaque lecture passe par le cache "seuils" (TTL configurable dans CacheConfig).
 *  - Toute modification via setSeuil() invalide le cache immédiatement.
 *
 * Ainsi : zéro requête DB pendant la détection d'anomalies,
 * et modification à chaud sans redémarrage du microservice.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeuilConfigService {

    private final SeuilConfigRepository seuilConfigRepository;

    // ── Valeurs par défaut depuis application.properties (fallback init) ──
    @Value("${anomalie.seuil.vitesse.max:80.0}")
    private Double defaultVitesseMax;

    @Value("${anomalie.seuil.temperature.critique:100.0}")
    private Double defaultTemperature;

    @Value("${anomalie.seuil.carburant.critique:5.0}")
    private Double defaultCarburant;

    @Value("${anomalie.seuil.immobilisation.minutes:5}")
    private Integer defaultImmobilisationMinutes;

    @Value("${anomalie.seuil.freinage.deceleration:-4.0}")
    private Double defaultFreinage;

    @Value("${anomalie.immobilisation.rayon.metres:50.0}")
    private Double defaultRayonMetres;

    @Value("${anomalie.immobilisation.nb.positions:10}")
    private Integer defaultNbPositions;

    // ========== INITIALISATION ==========

    /**
     * Si la table seuils_config est vide au démarrage,
     * on la remplit avec les valeurs par défaut de application.properties.
     * Permet une initialisation propre sans script SQL manuel.
     */
    @PostConstruct
    @Transactional
    public void initialiserSeuilsParDefaut() {
        if (seuilConfigRepository.count() > 0) {
            log.info("[SeuilConfig] Seuils déjà présents en base, initialisation ignorée.");
            return;
        }
        log.info("[SeuilConfig] Table vide — injection des seuils par défaut depuis application.properties");

        inserer(Cles.VITESSE_MAX,            defaultVitesseMax,               "Vitesse maximale autorisée",          "km/h");
        inserer(Cles.TEMPERATURE_CRITIQUE,   defaultTemperature,              "Température moteur critique",         "°C");
        inserer(Cles.CARBURANT_CRITIQUE,     defaultCarburant,                "Niveau carburant critique",           "%");
        inserer(Cles.IMMOBILISATION_MINUTES, defaultImmobilisationMinutes.doubleValue(), "Durée d'immobilisation avant alerte", "min");
        inserer(Cles.FREINAGE_DECELERATION,  defaultFreinage,                 "Décélération seuil freinage brusque", "m/s²");
        inserer(Cles.IMMOBILISATION_RAYON_M, defaultRayonMetres,              "Rayon de proximité d'un arrêt prévu", "m");
        inserer(Cles.IMMOBILISATION_NB_POS,  defaultNbPositions.doubleValue(), "Nombre de positions analysées pour immobilisation", "positions");
    }

    private void inserer(String cle, Double valeur, String description, String unite) {
        seuilConfigRepository.save(SeuilConfig.builder()
                .cle(cle)
                .valeur(valeur)
                .description(description)
                .unite(unite)
                .modifiePar("SYSTEM")
                .modifieLe(LocalDateTime.now())
                .build());
    }

    // ========== LECTURE (avec cache) ==========

    @Cacheable(value = "seuils", key = "#cle")
    public Double getSeuil(String cle) {
        return seuilConfigRepository.findByCle(cle)
                .map(SeuilConfig::getValeur)
                .orElseThrow(() -> new IllegalArgumentException("Seuil introuvable : " + cle));
    }

    /** Raccourcis typés utilisés par AnomalyDetectionService */
    public Double getVitesseMax()           { return getSeuil(Cles.VITESSE_MAX); }
    public Double getTemperatureCritique()  { return getSeuil(Cles.TEMPERATURE_CRITIQUE); }
    public Double getCarburantCritique()    { return getSeuil(Cles.CARBURANT_CRITIQUE); }
    public Integer getImmobilisationMin()   { return getSeuil(Cles.IMMOBILISATION_MINUTES).intValue(); }
    public Double getFreinageDeceleration() { return getSeuil(Cles.FREINAGE_DECELERATION); }
    public Double getRayonMetres()          { return getSeuil(Cles.IMMOBILISATION_RAYON_M); }
    public Integer getNbPositions()         { return getSeuil(Cles.IMMOBILISATION_NB_POS).intValue(); }

    @Transactional(readOnly = true)
    public List<SeuilConfig> listerTous() {
        return seuilConfigRepository.findAll();
    }

    // ========== MODIFICATION (invalide le cache) ==========

    /**
     * Met à jour un seuil en base et invalide son entrée en cache.
     * La prochaine lecture rechargera la valeur depuis la DB.
     */
    @Transactional
    @CacheEvict(value = "seuils", key = "#cle")
    public SeuilConfig setSeuil(String cle, Double nouvelleValeur, String modifiePar) {
        SeuilConfig seuil = seuilConfigRepository.findByCle(cle)
                .orElseThrow(() -> new IllegalArgumentException("Seuil introuvable : " + cle));

        Double ancienneValeur = seuil.getValeur();
        seuil.setValeur(nouvelleValeur);
        seuil.setModifiePar(modifiePar);
        seuil.setModifieLe(LocalDateTime.now());

        SeuilConfig sauvegarde = seuilConfigRepository.save(seuil);
        log.warn("[SeuilConfig] Seuil '{}' modifié : {} → {} par {}",
                cle, ancienneValeur, nouvelleValeur, modifiePar);
        return sauvegarde;
    }
}
