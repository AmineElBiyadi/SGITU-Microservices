package com.sgitu.servicegestionincidents.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "localisations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Localisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private String adresse;
    private String commune;
    private String quartier;
    private String ligneTransport;
    private String arretProche;

    public String getCoordonnees() {
        return latitude + ", " + longitude;
    }

    public String getAdresseComplete() {
        return (adresse != null ? adresse + ", " : "") +
                (quartier != null ? quartier + ", " : "") +
                (commune != null ? commune : "");
    }
}
