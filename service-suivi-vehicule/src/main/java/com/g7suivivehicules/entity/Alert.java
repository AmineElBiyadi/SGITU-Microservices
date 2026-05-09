package com.g7suivivehicules.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alert_vehicule",
                columnList = "vehiculeId"),
        @Index(name = "idx_alert_statut",
                columnList = "statut"),
        @Index(name = "idx_alert_type",
                columnList = "typeAlert"),
        @Index(name = "idx_alert_vehicule_type_statut",
                columnList = "vehiculeId, typeAlert, statut")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    // ========== IDENTIFIANT ==========
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ========== IDENTIFICATION VEHICULE ==========
    @Column(nullable = false)
    private UUID vehiculeId;

    // ========== TYPE & GRAVITE ==========
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAlert typeAlert;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severite severite;

    // ========== DONNEES TECHNIQUES ==========
    private Double valeur;   // ex: vitesse mesurée = 120.5 km/h
    private Double seuil;    // ex: seuil configuré = 80.0 km/h

    // ========== LOCALISATION ==========
    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    // ========== DESCRIPTION ==========
    @Column(nullable = false)
    private String message;

    // ========== TIMESTAMPS ==========
    @Column(nullable = false)
    private LocalDateTime timestampDebut;

    private LocalDateTime timestampFin;  // null si alerte encore ouverte

    // ========== CYCLE DE VIE ==========
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutAlert statut = StatutAlert.OUVERTE;

    @Builder.Default
    private Integer dureeMinutes = 0;

    // ========== FLAGS INTEGRATION ==========
    @Column(nullable = false)
    @Builder.Default
    private Boolean envoyeeG9 = false;      // signalé à G9 Incidents

    @Column(nullable = false)
    @Builder.Default
    private Boolean envoyeeKafka = false;   // publié sur Kafka

    // ========== ENUMS ==========

    public enum TypeAlert {
        IMMOBILISATION,        // vitesse = 0 trop longtemps hors arrêt
        VITESSE_EXCESSIVE,     // vitesse > seuil configuré
        TEMPERATURE_CRITIQUE,  // température moteur > seuil
        CARBURANT_CRITIQUE,    // carburant < seuil
        FREINAGE_BRUSQUE,      // décélération anormale détectée
        DEVIATION_ITINERAIRE,  // écart trop important par rapport au tracé G4
        RETARD_HORAIRE         // retard significatif par rapport à l'horaire G4
    }

    public enum StatutAlert {
        OUVERTE,    // alerte créée, anomalie toujours présente
        RESOLUE,    // anomalie disparue (résolution automatique)
        ANNULEE     // fausse alerte, annulée manuellement par l'opérateur
    }

    public enum Severite {
        FAIBLE,
        MOYENNE,
        HAUTE,
        CRITIQUE
    }
}