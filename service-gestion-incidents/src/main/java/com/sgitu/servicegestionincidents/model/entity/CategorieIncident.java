package com.sgitu.servicegestionincidents.model.entity;

import com.sgitu.servicegestionincidents.model.enums.NiveauGravite;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories_incident")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorieIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    private String description;

    @Enumerated(EnumType.STRING)
    private NiveauGravite niveauGraviteDefaut;

    private Integer delaiTraitementStandard; // en heures

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;

    public Integer getDelaiTraitement() {
        return delaiTraitementStandard;
    }
}
