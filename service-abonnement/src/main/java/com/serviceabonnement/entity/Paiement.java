package com.serviceabonnement.entity;

import com.serviceabonnement.enums.StatutPaiement;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "paiements_snapshot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "abonnement_id", nullable = false)
    private Abonnement abonnement;

    @Column(nullable = false)
    private Double montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPaiement statut;

    @Column(name = "paiement_ref")
    private String paiementRef; // Correspond au transactionId de G6

    @Column(name = "date_transaction")
    private LocalDateTime dateTransaction;

    @Column(name = "date_remboursement")
    private LocalDateTime dateRemboursement;

    @Column(name = "motif_refus")
    private String motifRefus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
