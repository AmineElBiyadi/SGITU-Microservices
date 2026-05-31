package com.g7suivivehicules.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité de configuration des seuils d'anomalies.
 *
 * Avantages par rapport à application.properties :
 *  - Modifiable à chaud via API REST, sans redémarrage
 *  - Historisable (on sait qui a changé quoi et quand)
 *  - Peut être différent par ligne ou type de véhicule (évolution future)
 *
 * Les valeurs sont chargées au démarrage depuis application.properties
 * si la table est vide (initialisation automatique).
 */
@Entity
@Table(name = "seuils_config", indexes = {
        @Index(name = "idx_seuil_cle", columnList = "cle", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeuilConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Clé unique du seuil — correspond aux constantes de SeuilConfig.Cles.
     * Exemples : "vitesse.max", "temperature.critique", "carburant.critique"
     */
    @Column(nullable = false, unique = true, length = 100)
    private String cle;

    /** Valeur numérique du seuil */
    @Column(nullable = false)
    private Double valeur;

    /** Description lisible pour l'interface d'administration */
    @Column(length = 255)
    private String description;

    /** Unité (km/h, °C, %, m/s², min, m) */
    @Column(length = 20)
    private String unite;

    /** Qui a modifié ce seuil en dernier */
    private String modifiePar;

    /** Quand ce seuil a été modifié en dernier */
    private LocalDateTime modifieLe;

    // ========== CONSTANTES DE CLÉS ==========

    public static final class Cles {
        public static final String VITESSE_MAX              = "vitesse.max";
        public static final String TEMPERATURE_CRITIQUE     = "temperature.critique";
        public static final String CARBURANT_CRITIQUE       = "carburant.critique";
        public static final String IMMOBILISATION_MINUTES   = "immobilisation.minutes";
        public static final String FREINAGE_DECELERATION    = "freinage.deceleration";
        public static final String IMMOBILISATION_RAYON_M   = "immobilisation.rayon.metres";
        public static final String IMMOBILISATION_NB_POS    = "immobilisation.nb.positions";

        private Cles() {}
    }
}
