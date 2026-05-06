package com.sgitu.servicegestionincidents.model.entity;

import com.sgitu.servicegestionincidents.model.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeIncident type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime dateSignalement;

    @Column(nullable = false)
    private LocalDateTime dateIncident;

    private LocalDateTime dateLimiteResolution;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutIncident statut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NiveauGravite gravite;

    @Column(nullable = false)
    private Long declarantId;

    private Long responsableId;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "localisation_id", nullable = false)
    private Localisation localisation;

    @ManyToOne
    @JoinColumn(name = "categorie_id")
    private CategorieIncident categorie;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Preuve> preuves = new ArrayList<>();

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Action> actions = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public boolean isCloturable() {
        return this.statut == StatutIncident.RESOLU;
    }

    public boolean isEscaladable() {
        return this.gravite == NiveauGravite.CRITIQUE &&
                this.statut != StatutIncident.ESCALADE;
    }

    public void addPreuve(Preuve preuve) {
        preuves.add(preuve);
        preuve.setIncident(this);
    }

    public void addAction(Action action) {
        actions.add(action);
        action.setIncident(this);
    }
}
