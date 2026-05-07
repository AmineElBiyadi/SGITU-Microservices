package com.serviceabonnement.entity;

import com.serviceabonnement.enums.StatutRenouvellement;
import com.serviceabonnement.enums.TypeRenouvellement;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "renouvellements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Renouvellement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "abonnement_id", nullable = false)
    private Abonnement abonnement;

    @Column(name = "paiement_id")
    private String paiementId;

    @Column(name = "date_renouvellement")
    private LocalDateTime dateRenouvellement;

    @Enumerated(EnumType.STRING)
    private StatutRenouvellement statut;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_renouvellement")
    private TypeRenouvellement typeRenouvellement;

    @Column(name = "prix_applique")
    private Double prixApplique;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
