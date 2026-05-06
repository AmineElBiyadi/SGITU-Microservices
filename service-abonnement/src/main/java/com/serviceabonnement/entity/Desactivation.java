package com.serviceabonnement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "desactivations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Desactivation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "abonnement_id", nullable = false)
    private Abonnement abonnement;

    @Column(name = "date_debut_desactivation", nullable = false)
    private LocalDateTime dateDebutDesactivation;

    @Column(name = "date_fin_desactivation")
    private LocalDateTime dateFinDesactivation;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
