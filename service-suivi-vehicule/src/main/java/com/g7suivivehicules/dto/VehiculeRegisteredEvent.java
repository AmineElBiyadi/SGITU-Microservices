package com.g7suivivehicules.dto;

import com.g7suivivehicules.entity.Vehicule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Événement Kafka publié sur le topic "vehicle.registered"
 * à chaque création d'un nouveau véhicule dans la flotte.
 *
 * Consommateurs potentiels :
 * - G4 (Coordination) : affecter le véhicule à une ligne
 * - G8 (Analytique)   : initialiser les statistiques du véhicule
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiculeRegisteredEvent {

    /** UUID unique du véhicule nouvellement créé */
    private UUID vehiculeId;

    /** Immatriculation du véhicule (ex: "AA-123-BB") */
    private String immatriculation;

    /** Type de véhicule : BUS, TRAM, TAXI, METRO, TRAIN */
    private Vehicule.TypeVehicule type;

    /** Identifiant de la ligne affectée (null si non assigné) */
    private String ligne;

    /** Statut initial — toujours DISPONIBLE à la création */
    private Vehicule.StatutVehicule statut;

    /** UUID du conducteur associé (null si non assigné) */
    private UUID conducteurId;

    /** Horodatage de la création */
    private LocalDateTime createdAt;
}
