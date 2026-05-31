package com.g7suivivehicules.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "arrets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Arret {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID arretId; // référence G4

    @Column(nullable = false)
    private UUID vehiculeId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Boolean present;

    // Coordonnées GPS de l'arrêt (nécessaires pour le calcul de proximité)
    private Double latitude;

    private Double longitude;

    // Nom lisible de l'arrêt (ex: "Arrêt Liberté", "Terminus Nord")
    private String nom;
}
