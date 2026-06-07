package com.sgitu.servicegestionincidents.model.entity;

import com.sgitu.servicegestionincidents.model.enums.TypePreuve;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "preuves")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Preuve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypePreuve type;

    private String fichier;
    private String stockageKey;
    private String urlStockage;
    private String description;
    private Long tailleFichier;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime dateAjout = LocalDateTime.now();

    public String getUrl() {
        return urlStockage;
    }

    public Long getTaille() {
        return this.tailleFichier != null ? this.tailleFichier : 0L;
    }
}
