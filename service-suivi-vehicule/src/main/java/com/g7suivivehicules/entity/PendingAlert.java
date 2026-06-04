package com.g7suivivehicules.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private java.util.UUID id;

    private String vehiculeId;
    private String typeAnomalie;
    private String message;
    private String priority;

    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    private LocalDateTime createdAt;
    private LocalDateTime lastAttemptAt;

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    private int tentatives;
}
