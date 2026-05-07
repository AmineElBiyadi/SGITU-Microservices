package com.serviceabonnement.entity;

import com.serviceabonnement.enums.StatutAbonnement;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "abonnements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Abonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanAbonnement plan;

    @Column(name = "date_debut")
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Column(name = "date_demande_annulation")
    private LocalDateTime dateDemandeAnnulation;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @Column(name = "date_derniere_tentative_remb")
    private LocalDateTime dateDerniereTentativeRemb;

    @Column(name = "nb_tentatives_remb")
    private int nbTentativesRemb;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAbonnement statut;

    @Column(name = "remboursement_id")
    private String remboursementId;

    @Column(name = "paiement_id")
    private String paiementId;

    @Column(name = "prix_paye", nullable = false)
    private Double prixPaye; // Snapshot du prix au moment de la souscription

    @Column(name = "renouvellement_auto")
    private boolean renouvellementAuto;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
