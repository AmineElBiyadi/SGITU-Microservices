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

    private String ligneTransport;

    public String getCoordonnees() {
        return latitude + ", " + longitude;
    }

}
