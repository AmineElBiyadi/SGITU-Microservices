package com.g7suivivehicules.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_incidents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private java.util.UUID id;

    private String vehiculeId;
    private String typeIncident;
    private String gravite;
    private String description;
    private Double latitude;
    private Double longitude;
    private String dateDetection;

    private LocalDateTime createdAt;
    private LocalDateTime lastAttemptAt;

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    private int tentatives;
}
