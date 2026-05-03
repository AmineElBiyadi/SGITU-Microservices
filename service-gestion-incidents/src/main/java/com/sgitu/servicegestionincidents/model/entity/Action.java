package com.sgitu.servicegestionincidents.model.entity;

import com.sgitu.servicegestionincidents.model.enums.StatutIncident;
import com.sgitu.servicegestionincidents.model.enums.TypeAction;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "actions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAction type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long auteurId;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime dateAction = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private StatutIncident ancienStatut;

    @Enumerated(EnumType.STRING)
    private StatutIncident nouveauStatut;

    public String getDetails() {
        return String.format("[%s] %s par utilisateur %d",
                type, description, auteurId);
    }
}
