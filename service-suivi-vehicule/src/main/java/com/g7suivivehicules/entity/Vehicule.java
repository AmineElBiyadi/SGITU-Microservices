package com.g7suivivehicules.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "vehicules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String immatriculation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeVehicule type; // BUS ou TRAM

    private String ligne;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutVehicule statut; 

    private UUID conducteurId;

    public enum TypeVehicule {
        BUS, TRAM, TAXI, METRO , TRAIN
    }

    public enum StatutVehicule {
        DISPONIBLE,       // Nouveau véhicule prêt à être affecté
        EN_SERVICE,       // circulation normale
        EN_PAUSE,         // arrêt court planifié
        ARRET_PROLONGE,   // vitesse nulle > 5min → alerte G9
        INCIDENT,         // choc/accident détecté IoT → alerte G9 + G5
        EN_PANNE,         // défaillance mécanique/électrique
        HORS_SERVICE      // retiré manuellement
    }
}

