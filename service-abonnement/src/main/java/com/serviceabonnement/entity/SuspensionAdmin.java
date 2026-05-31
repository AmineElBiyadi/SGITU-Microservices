package com.serviceabonnement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "suspension_admin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspensionAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "abonnement_id", nullable = false)
    private Abonnement abonnement;

    @Column(name = "admin_id", nullable = false)
    private String adminId;

    @Column(name = "date_debut_suspension", nullable = false)
    private LocalDateTime dateDebutSuspension;

    @Column(name = "date_fin_suspension")
    private LocalDateTime dateFinSuspension;

    @Column(name = "motif", nullable = false)
    private String motif;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
