package com.serviceabonnement.entity;

import com.serviceabonnement.enums.CategorieAbonnement;
import com.serviceabonnement.enums.DureeOffre;
import com.serviceabonnement.enums.MoyenTransport;
import com.serviceabonnement.enums.StatutOffre;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "plans_abonnement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanAbonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPlan;

    @Column(nullable = false)
    private String nomPlan;

    private String description;

    @Column(nullable = false)
    private Double prix;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DureeOffre duree;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategorieAbonnement categorie;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", nullable = false)
    private MoyenTransport transportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "est_actif", nullable = false)
    private StatutOffre estActif;

    private int maxDesactivation;
    private int minJoursEntreDesactivation;
    private int maxPeriodeDesactivation;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
