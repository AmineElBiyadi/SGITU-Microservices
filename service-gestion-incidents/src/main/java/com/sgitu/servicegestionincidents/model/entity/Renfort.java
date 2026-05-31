package com.sgitu.servicegestionincidents.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident_renforts")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Renfort {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Incident incident;

    @Column(nullable = false)
    private Long agentId;

    @Column(nullable = false)
    private Long auteurAffectationId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateAffectation;
}
